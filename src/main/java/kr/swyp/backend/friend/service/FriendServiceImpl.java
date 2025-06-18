package kr.swyp.backend.friend.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import kr.swyp.backend.common.domain.File;
import kr.swyp.backend.common.repository.FileRepository;
import kr.swyp.backend.common.service.S3Service;
import kr.swyp.backend.friend.domain.Friend;
import kr.swyp.backend.friend.domain.FriendAnniversary;
import kr.swyp.backend.friend.domain.FriendCheckingLog;
import kr.swyp.backend.friend.domain.FriendDetail;
import kr.swyp.backend.friend.domain.FriendDetail.FriendDetailBuilder;
import kr.swyp.backend.friend.dto.FriendDto.FriendCheckLogResponse;
import kr.swyp.backend.friend.dto.FriendDto.FriendCreateListRequest;
import kr.swyp.backend.friend.dto.FriendDto.FriendCreateListRequest.FriendRequest.FriendAnniversaryCreateRequest;
import kr.swyp.backend.friend.dto.FriendDto.FriendCreateListResponse;
import kr.swyp.backend.friend.dto.FriendDto.FriendCreateListResponse.FriendResponse;
import kr.swyp.backend.friend.dto.FriendDto.FriendCreateListResponse.FriendResponse.FriendAnniversaryCreateResponse;
import kr.swyp.backend.friend.dto.FriendDto.FriendCreateListResponse.FriendResponse.FriendResponseBuilder;
import kr.swyp.backend.friend.dto.FriendDto.FriendDetailResponse;
import kr.swyp.backend.friend.dto.FriendDto.FriendDetailUpdateRequest;
import kr.swyp.backend.friend.dto.FriendDto.FriendDetailUpdateRequest.FriendAnniversaryDetailUpdateRequest;
import kr.swyp.backend.friend.dto.FriendDto.FriendListResponse;
import kr.swyp.backend.friend.dto.FriendDto.FriendNearResponse;
import kr.swyp.backend.friend.enums.FriendRemind;
import kr.swyp.backend.friend.repository.FriendAnniversaryRepository;
import kr.swyp.backend.friend.repository.FriendCheckingLogRepository;
import kr.swyp.backend.friend.repository.FriendDetailRepository;
import kr.swyp.backend.friend.repository.FriendRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {

    private final S3Service s3Service;
    private final FriendRepository friendRepository;
    private final FriendAnniversaryRepository friendAnniversaryRepository;
    private final FriendCheckingLogRepository friendCheckingLogRepository;
    private final FileRepository fileRepository;
    private final FriendDetailRepository friendDetailRepository;

    @Override
    @Transactional
    public FriendCreateListResponse init(UUID memberId, FriendCreateListRequest request) {
        AtomicInteger position = new AtomicInteger(0); // 시작 값을 0으로 설정

        List<FriendResponse> friendResponseList = request.getFriendList().stream()
                .map(friendRequest -> {
                    Friend friend = Friend.builder()
                            .memberId(memberId)
                            .name(friendRequest.getName())
                            .friendSource(friendRequest.getSource())
                            .contactFrequency(friendRequest.getContactFrequency())
                            .position(position.getAndIncrement())
                            .nextContactAt(
                                    friendRequest.getNextContactAt(
                                            friendRequest.getContactFrequency()))
                            .build();

                    friendRepository.save(friend);

                    FriendDetailBuilder friendDetailBuilder = FriendDetail.builder()
                            .friend(friend)
                            .phone(friendRequest.getPhone())
                            .relation(friendRequest.getRelation())
                            .birthday(friendRequest.getBirthDay())
                            .memo(friendRequest.getMemo());

                    FriendResponseBuilder friendResponseBuilder = FriendResponse.builder()
                            .friendId(friend.getFriendId())
                            .name(friend.getName())
                            .source(friend.getFriendSource())
                            .contactFrequency(friend.getContactFrequency())
                            .nextContactAt(friend.getNextContactAt());

                    if (friendRequest.getImageUploadRequest() != null) {
                        String preSignedUrl = s3Service.generatePreSignedUrlForUpload(memberId,
                                friendRequest.getImageUploadRequest()).getPreSignedUrl();
                        File file = s3Service.createFile(memberId,
                                friendRequest.getImageUploadRequest());
                        friendDetailBuilder.imageFileId(file.getId());
                        friendResponseBuilder.fileName(file.getFileName());
                        friendResponseBuilder.preSignedImageUrl(preSignedUrl);
                    }

                    friend.addFriendDetail(friendDetailBuilder.build());
                    friendRepository.save(friend);

                    if (friendRequest.getAnniversary() != null) {
                        FriendAnniversary friendAnniversary = friendAnniversaryRepository.save(
                                FriendAnniversaryCreateRequest.toEntity(friend.getFriendId(),
                                        friendRequest.getAnniversary()));
                        friendResponseBuilder.anniversary(
                                FriendAnniversaryCreateResponse.fromEntity(friendAnniversary));
                    }

                    if (friendRequest.getPhone() != null) {
                        friendResponseBuilder.phone(friend.getFriendDetail().getPhone());
                    }
                    if (friendRequest.getMemo() != null) {
                        friendResponseBuilder.memo(friend.getFriendDetail().getMemo());
                    }

                    return friendResponseBuilder.build();
                }).toList();

        return FriendCreateListResponse.builder()
                .friendList(friendResponseList)
                .build();
    }

    @Override
    @Transactional
    public void recordCheckAndUpdateRate(UUID memberId, UUID friendId) {
        // Friend 엔티티 조회
        Friend friend = friendRepository.findByFriendIdAndMemberId(friendId, memberId)
                .orElseThrow(() -> new NoSuchElementException("해당 친구를 찾을 수 없습니다."));

        // 체크 로그 생성 및 저장
        FriendCheckingLog log = FriendCheckingLog.of(friend, true);
        friendCheckingLogRepository.save(log);

        // 체크율 계산
        Integer checkCount = friendCheckingLogRepository.countCheckedLogsByFriendId(friendId);
        int alarmTriggerCount = friend.getAlarmTriggerCount();
        int checkRate = 0;

        if (alarmTriggerCount > 0) {
            checkRate = (int) Math.round(((double) checkCount / alarmTriggerCount) * 100);
        }

        // 체크율 업데이트
        friend.updateCheckRate(checkRate);
        friend.updateNextContactAt();
        friendRepository.save(friend);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FriendCheckLogResponse> getCheckLogs(UUID memberId, UUID friendId) {

        // Friend 엔티티 조회
        Friend friend = friendRepository.findByFriendIdAndMemberId(friendId, memberId)
                .orElseThrow(() -> new NoSuchElementException("해당 친구를 찾을 수 없습니다."));

        List<FriendCheckingLog> logs = friendCheckingLogRepository.findByFriend_FriendId(
                friend.getFriendId());

        return logs.stream()
                .map(FriendCheckLogResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public void updateAlarmCheck(UUID memberId, UUID friendId) {
        Friend friend = friendRepository.findByFriendIdAndMemberId(friendId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("해당 친구를 찾을 수 없습니다."));
        friend.updateAlarmTriggerCount();
        friendRepository.save(friend);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FriendListResponse> getFriendList(UUID memberId) {
        List<Friend> friends = friendRepository.findAllByMemberIdWithDetail(memberId);

        return friends.stream()
                .map(friend -> {
                    File imageFile = Optional.ofNullable(friend.getFriendDetail())
                            .filter(detail -> detail.getImageFileId() != null)
                            .flatMap(detail -> fileRepository.findById(detail.getImageFileId()))
                            .orElse(null);

                    String fileName = imageFile != null ? imageFile.getFileName() : null;
                    String imageUrl = imageFile != null ? s3Service.generatePreSignedUrlForDownload(
                            memberId,
                            imageFile.getCategory(),
                            imageFile.getFileName()
                    ).getPreSignedUrl() : null;
                    FriendCheckingLog friendCheckingLog =
                            friendCheckingLogRepository
                                    .findFirstByFriend_FriendIdAndIsCheckedTrueOrderByCreatedAtDesc(
                                            friend.getFriendId())
                                    .orElse(null);
                    return FriendListResponse.fromEntity(friend, imageUrl, fileName,
                            friendCheckingLog);
                })
                .toList();
    }

    @Override
    @Transactional
    public void updateFriendPosition(UUID memberId, UUID friendId, int newPosition) {
        Friend friend = friendRepository.findByFriendIdAndMemberId(friendId, memberId)
                .orElseThrow(() -> new NoSuchElementException("해당 친구를 찾을 수 없습니다."));
        friend.updatePosition(newPosition);
    }

    @Override
    @Transactional(readOnly = true)
    public FriendDetailResponse getFriendDetail(UUID memberId, UUID friendId) {
        Friend friend = friendRepository.findByFriendIdAndMemberId(friendId, memberId)
                .orElseThrow(() -> new NoSuchElementException("해당 친구를 찾을 수 없습니다."));

        File imageFile = Optional.ofNullable(friend.getFriendDetail())
                .filter(detail -> detail.getImageFileId() != null)
                .flatMap(detail -> fileRepository.findById(detail.getImageFileId()))
                .orElse(null);

        String imageUrl = imageFile != null ? s3Service.generatePreSignedUrlForDownload(
                memberId,
                imageFile.getCategory(),
                imageFile.getFileName()
        ).getPreSignedUrl() : null;

        List<FriendAnniversary> friendAnniversaryList = friendAnniversaryRepository
                .findByFriendId(friend.getFriendId());

        FriendCheckingLog friendCheckingLog =
                friendCheckingLogRepository
                        .findFirstByFriend_FriendIdAndIsCheckedTrueOrderByCreatedAtDesc(friendId)
                        .orElse(null);

        return FriendDetailResponse.fromEntity(friend, imageUrl, friend.getFriendDetail(),
                friendAnniversaryList, friendCheckingLog);
    }

    @Override
    @Transactional
    public FriendDetailResponse updateFriend(UUID memberId, UUID friendId,
            FriendDetailUpdateRequest request) {
        Friend friend = friendRepository.findByFriendIdAndMemberId(friendId, memberId)
                .orElseThrow(() -> new NoSuchElementException("해당 친구를 찾을 수 없습니다."));

        // Friend 업데이트
        friend.updateName(request.getName());
        friend.updateFriendContactFrequency(request.getContactFrequency());
        friend.updateNextContactAt();

        // FriendDetail 업데이트
        FriendDetail friendDetail = friend.getFriendDetail();
        friendDetail.updateRelation(request.getRelation());
        friendDetail.updateBirthday(request.getBirthday());
        friendDetail.updateMemo(request.getMemo());
        friendDetail.updatePhone(request.getPhone());

        // AnniversaryList 처리
        List<FriendAnniversary> friendAnniversaryList = processAnniversaries(friendId,
                request.getAnniversaryList());

        return FriendDetailResponse.fromEntity(friend, null, friendDetail, friendAnniversaryList);
    }

    private List<FriendAnniversary> processAnniversaries(UUID friendId,
            List<FriendAnniversaryDetailUpdateRequest> anniversaryRequests) {
        if (anniversaryRequests == null || anniversaryRequests.isEmpty()) {
            return new ArrayList<>();
        }

        // 새로운 기념일과 기존 기념일 분리
        List<FriendAnniversaryDetailUpdateRequest> existingAnniversaries =
                anniversaryRequests.stream()
                        .filter(req -> req.getId() != null)
                        .toList();

        List<FriendAnniversaryDetailUpdateRequest> newAnniversaries = anniversaryRequests.stream()
                .filter(req -> req.getId() == null)
                .toList();

        List<FriendAnniversary> result = new ArrayList<>();

        // 기존 기념일 업데이트
        if (!existingAnniversaries.isEmpty()) {
            List<FriendAnniversary> existingList = friendAnniversaryRepository
                    .findByIdIsIn(existingAnniversaries.stream()
                            .map(FriendAnniversaryDetailUpdateRequest::getId)
                            .toList());

            existingAnniversaries.forEach(request -> {
                FriendAnniversary anniversary = existingList.stream()
                        .filter(a -> a.getId().equals(request.getId()))
                        .findFirst()
                        .orElseThrow(() -> new NoSuchElementException("해당 기념일을 찾을 수 없습니다."));

                anniversary.updateDate(request.getDate());
                anniversary.updateTitle(request.getTitle());
            });

            result.addAll(existingList);
        }

        // 새 기념일 생성
        if (!newAnniversaries.isEmpty()) {
            List<FriendAnniversary> newList = newAnniversaries.stream()
                    .map(request -> FriendAnniversary.builder()
                            .friendId(friendId)
                            .title(request.getTitle())
                            .date(request.getDate())
                            .build())
                    .toList();

            result.addAll(friendAnniversaryRepository.saveAll(newList));
        }

        return result;
    }

    @Override
    @Transactional
    public void deleteFriend(UUID memberId, UUID friendId) {
        friendAnniversaryRepository.deleteByFriendId(friendId);
        friendRepository.deleteById(friendId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FriendNearResponse> getMonthlyFriendNearList(UUID memberId) {
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();

        // 친구 디테일 조회
        List<FriendDetail> friendDetailList = friendDetailRepository
                .findAllByFriend_MemberIdAndBirthdayBetween(
                        memberId,
                        now.withDayOfMonth(1),
                        now.withDayOfMonth(now.lengthOfMonth()));

        // 친구 ID 목록 추출
        List<UUID> friendIdList = friendRepository.findAllByMemberId(memberId)
                .stream()
                .map(Friend::getFriendId)
                .toList();

        // 기념일 목록 조회
        List<FriendAnniversary> friendAnniversaryList = friendAnniversaryRepository
                .findAllByFriendIdIsInAndDateBetween(friendIdList,
                        now.withDayOfMonth(1),
                        now.withDayOfMonth(now.lengthOfMonth()));

        // 친구 조회
        List<Friend> friendList = friendRepository.findAllByMemberIdAndNextContactAtBetween(
                memberId,
                now.withDayOfMonth(1),
                now.withDayOfMonth(now.lengthOfMonth()));

        // 친구 상세 정보로부터 응답 객체 생성
        List<FriendNearResponse> birthdayResponses = friendDetailList
                .stream()
                .filter(detail -> detail.getBirthday() != null)
                .map(detail -> {
                    Friend friend = detail.getFriend();
                    LocalDate birthday = detail.getBirthday();
                    // 생일을 올해 날짜로 변환
                    LocalDate thisYearBirthday = birthday.withYear(currentYear);
                    // 이미 지난 경우, 내년 생일로 설정
                    LocalDate nextBirthday = thisYearBirthday.isBefore(now)
                            ? thisYearBirthday.plusYears(1) : thisYearBirthday;

                    return FriendNearResponse.builder()
                            .friendId(friend.getFriendId())
                            .name(friend.getName())
                            .type(FriendRemind.BIRTHDAY) // 생일 타입 설정
                            .nextContactAt(nextBirthday)
                            .build();
                })
                .toList();

        // 기념일 정보로부터 응답 객체 생성
        List<FriendNearResponse> anniversaryResponses = friendAnniversaryList
                .stream()
                .map(anniversary -> {
                    // 해당 기념일의 친구 찾기
                    Friend friend = friendRepository.findById(anniversary.getFriendId())
                            .orElseThrow(() -> new NoSuchElementException(
                                    "친구를 찾을 수 없습니다: " + anniversary.getFriendId()));

                    LocalDate anniversaryDate = anniversary.getDate();
                    // 기념일을 올해 날짜로 변환
                    LocalDate thisYearAnniversary = anniversaryDate.withYear(currentYear);
                    // 이미 지난 경우, 내년 기념일로 설정
                    LocalDate nextAnniversary = thisYearAnniversary.isBefore(now)
                            ? thisYearAnniversary.plusYears(1) : thisYearAnniversary;

                    return FriendNearResponse.builder()
                            .friendId(anniversary.getFriendId())
                            .name(friend.getName())
                            .type(FriendRemind.ANNIVERSARY) // 기념일 제목을 타입으로 설정
                            .nextContactAt(nextAnniversary)
                            .build();
                })
                .toList();

        // 메시지 응답 객체 생성
        List<FriendNearResponse> messageResponse = friendList
                .stream()
                .map(friend -> FriendNearResponse.builder()
                        .friendId(friend.getFriendId())
                        .name(friend.getName())
                        .type(FriendRemind.MESSAGE)
                        .nextContactAt(friend.getNextContactAt())
                        .build())
                .toList();

        // 결과 합치기
        List<FriendNearResponse> result = new ArrayList<>();
        result.addAll(birthdayResponses);
        result.addAll(anniversaryResponses);
        result.addAll(messageResponse);

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("checkstyle:LineLength")
    public List<FriendNearResponse> getMonthlyCompleteFriendNearList(UUID memberId) {
        LocalDate now = LocalDate.now();

        List<FriendDetail> friendDetailList = friendDetailRepository
                .findBirthdaysWithCheckedLogs(
                        memberId,
                        now.withDayOfMonth(1),
                        now.withDayOfMonth(now.lengthOfMonth()));

        List<UUID> friendIdList = friendRepository.findAllByMemberId(memberId)
                .stream()
                .map(Friend::getFriendId)
                .toList();

        List<Friend> friendList = friendRepository.findAllByMemberIdWithCheckedLogsInPeriod(
                memberId,
                now.withDayOfMonth(1).atStartOfDay(),
                now.withDayOfMonth(now.lengthOfMonth()).atTime(LocalTime.of(23, 59, 59))
        );

        List<Long> friendCheckingLogIdList = friendCheckingLogRepository
                .findAllByFriend_FriendIdIsInAndIsCheckedTrueAndCreatedAtBetweenOrderByCreatedAtDesc(
                        friendIdList,
                        now.withDayOfMonth(1).atStartOfDay(),
                        now.withDayOfMonth(now.lengthOfMonth())
                                .atTime(LocalTime.of(23, 59, 59)))
                .stream()
                .map(FriendCheckingLog::getId)
                .toList();

        List<FriendAnniversary> friendAnniversaryList =
                friendAnniversaryRepository.findAllAnniversaryByCheckingLogIdList(
                        friendCheckingLogIdList);

        // 친구 상세 정보로부터 응답 객체 생성
        List<FriendNearResponse> birthdayResponses = friendDetailList
                .stream()
                .filter(detail -> detail.getBirthday() != null)
                .map(detail -> {
                    Friend friend = detail.getFriend();
                    return FriendNearResponse.builder()
                            .friendId(friend.getFriendId())
                            .name(friend.getName())
                            .type(FriendRemind.BIRTHDAY) // 생일 타입 설정
                            .nextContactAt(detail.getBirthday().plusYears(1))
                            .build();
                })
                .toList();

        // 기념일 정보로부터 응답 객체 생성
        List<FriendNearResponse> anniversaryResponses = friendAnniversaryList
                .stream()
                .map(anniversary -> {
                    // 해당 기념일의 친구 찾기
                    Friend friend = friendRepository.findById(anniversary.getFriendId())
                            .orElseThrow(() -> new NoSuchElementException(
                                    "친구를 찾을 수 없습니다: " + anniversary.getFriendId()));

                    return FriendNearResponse.builder()
                            .friendId(anniversary.getFriendId())
                            .name(friend.getName())
                            .type(FriendRemind.ANNIVERSARY) // 기념일 제목을 타입으로 설정
                            .nextContactAt(anniversary.getDate().plusYears(1))
                            .build();
                })
                .toList();

        List<FriendNearResponse> messageResponse = friendList
                .stream()
                .map(friend -> FriendNearResponse.builder()
                        .friendId(friend.getFriendId())
                        .name(friend.getName())
                        .type(FriendRemind.MESSAGE)
                        .nextContactAt(friend.getNextContactAt())
                        .build()).toList();

        // 두 리스트 합치기
        List<FriendNearResponse> result = new ArrayList<>();
        result.addAll(birthdayResponses);
        result.addAll(anniversaryResponses);
        result.addAll(messageResponse);

        return result;
    }
}

