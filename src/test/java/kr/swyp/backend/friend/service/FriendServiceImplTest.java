package kr.swyp.backend.friend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import kr.swyp.backend.common.dto.FileDto.FileUploadRequest;
import kr.swyp.backend.friend.domain.FriendContactFrequency;
import kr.swyp.backend.friend.dto.FriendDto.FriendCreateListRequest;
import kr.swyp.backend.friend.dto.FriendDto.FriendCreateListRequest.FriendRequest;
import kr.swyp.backend.friend.dto.FriendDto.FriendCreateListRequest.FriendRequest.FriendAnniversaryCreateRequest;
import kr.swyp.backend.friend.dto.FriendDto.FriendCreateListResponse;
import kr.swyp.backend.friend.enums.FriendContactWeek;
import kr.swyp.backend.friend.enums.FriendRelation;
import kr.swyp.backend.friend.enums.FriendSource;
import kr.swyp.backend.friend.domain.ContactReminder;
import kr.swyp.backend.friend.domain.Friend;
import kr.swyp.backend.friend.enums.ReminderType;
import kr.swyp.backend.friend.repository.ContactReminderRepository;
import kr.swyp.backend.friend.repository.FriendAnniversaryRepository;
import kr.swyp.backend.friend.repository.FriendRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class FriendServiceImplTest {

    @Autowired
    private FriendServiceImpl friendService;

    @Autowired
    private FriendRepository friendRepository;

    @Autowired
    private ContactReminderRepository contactReminderRepository;

    @Autowired
    private FriendAnniversaryRepository friendAnniversaryRepository;

    @Test
    @DisplayName("친구를 추가할 수 있어야 한다.")
    void 친구를_추가할_수_있어야_한다() {
        // given
        UUID memberId = UUID.randomUUID();

        FriendCreateListRequest friendCreateListRequest =
                FriendCreateListRequest.builder()
                        .friendList(List.of(FriendRequest.builder()
                                        .name("test")
                                        .source(FriendSource.KAKAO)
                                        .relation(FriendRelation.FRIEND)
                                        .birthDay(LocalDate.now())
                                        .contactFrequency(FriendContactFrequency.builder()
                                                .contactWeek(FriendContactWeek.EVERY_WEEK)
                                                .dayOfWeek(DayOfWeek.MONDAY)
                                                .build())
                                        .imageUploadRequest(FileUploadRequest.builder()
                                                .fileName("test.jpg")
                                                .contentType("image/jpeg")
                                                .fileSize(1024L)
                                                .category("test")
                                                .build())
                                        .anniversary(FriendAnniversaryCreateRequest.builder()
                                                .title("test")
                                                .date(LocalDate.now())
                                                .build())
                                        .phone("01000000000")
                                        .memo("test")
                                        .build(),
                                FriendRequest.builder()
                                        .name("test")
                                        .relation(FriendRelation.FRIEND)
                                        .birthDay(LocalDate.now())
                                        .source(FriendSource.APPLE)
                                        .contactFrequency(FriendContactFrequency.builder()
                                                .contactWeek(FriendContactWeek.EVERY_WEEK)
                                                .dayOfWeek(DayOfWeek.MONDAY)
                                                .build())
                                        .imageUploadRequest(FileUploadRequest.builder()
                                                .fileName("test.jpg")
                                                .contentType("image/jpeg")
                                                .fileSize(1024L)
                                                .category("test")
                                                .build())
                                        .anniversary(FriendAnniversaryCreateRequest.builder()
                                                .title("test")
                                                .date(LocalDate.now())
                                                .build())
                                        .phone("01000000000")
                                        .memo("test")
                                        .build()
                        )).build();

        // when
        FriendCreateListResponse response = friendService.init(memberId, friendCreateListRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getFriendList().get(0).getPreSignedImageUrl()).isNotNull();
    }

    @Test
    @DisplayName("친구 초기화 시 연락 주기에 따른 ContactReminder가 생성되어야 한다")
    void 친구_초기화_시_연락_주기에_따른_ContactReminder가_생성되어야_한다() {
        // given
        UUID memberId = UUID.randomUUID();
        
        FriendCreateListRequest request = FriendCreateListRequest.builder()
                .friendList(List.of(
                        FriendRequest.builder()
                                .name("매주 연락 친구")
                                .source(FriendSource.KAKAO)
                                .relation(FriendRelation.FRIEND)
                                .contactFrequency(FriendContactFrequency.builder()
                                        .contactWeek(FriendContactWeek.EVERY_WEEK)
                                        .dayOfWeek(DayOfWeek.MONDAY)
                                        .build())
                                .build()
                ))
                .build();

        // when
        FriendCreateListResponse response = friendService.init(memberId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getFriendList()).hasSize(1);
        
        // ContactReminder가 생성되었는지 확인
        List<ContactReminder> reminders = contactReminderRepository.findByMemberIdAndIsActiveTrue(memberId);
        assertThat(reminders).hasSize(1);
        
        ContactReminder reminder = reminders.get(0);
        assertThat(reminder.getReminderType()).isEqualTo(ReminderType.CONTACT);
        assertThat(reminder.getFrequencyDays()).isEqualTo(7); // 매주는 7일
        assertThat(reminder.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("친구 초기화 시 다양한 연락 주기로 ContactReminder가 생성되어야 한다")
    void 친구_초기화_시_다양한_연락_주기로_ContactReminder가_생성되어야_한다() {
        // given
        UUID memberId = UUID.randomUUID();
        
        FriendCreateListRequest request = FriendCreateListRequest.builder()
                .friendList(List.of(
                        FriendRequest.builder()
                                .name("매일 연락 친구")
                                .source(FriendSource.KAKAO)
                                .relation(FriendRelation.FRIEND)
                                .contactFrequency(FriendContactFrequency.builder()
                                        .contactWeek(FriendContactWeek.EVERY_DAY)
                                        .build())
                                .build(),
                        FriendRequest.builder()
                                .name("2주마다 연락 친구")
                                .source(FriendSource.KAKAO)
                                .relation(FriendRelation.FRIEND)
                                .contactFrequency(FriendContactFrequency.builder()
                                        .contactWeek(FriendContactWeek.EVERY_TWO_WEEK)
                                        .dayOfWeek(DayOfWeek.FRIDAY)
                                        .build())
                                .build(),
                        FriendRequest.builder()
                                .name("매월 연락 친구")
                                .source(FriendSource.APPLE)
                                .relation(FriendRelation.FRIEND)
                                .contactFrequency(FriendContactFrequency.builder()
                                        .contactWeek(FriendContactWeek.EVERY_MONTH)
                                        .build())
                                .build(),
                        FriendRequest.builder()
                                .name("6개월마다 연락 친구")
                                .source(FriendSource.KAKAO)
                                .relation(FriendRelation.FRIEND)
                                .contactFrequency(FriendContactFrequency.builder()
                                        .contactWeek(FriendContactWeek.EVERY_SIX_MONTH)
                                        .build())
                                .build()
                ))
                .build();

        // when
        FriendCreateListResponse response = friendService.init(memberId, request);

        // then
        assertThat(response.getFriendList()).hasSize(4);
        
        List<ContactReminder> reminders = contactReminderRepository.findByMemberIdAndIsActiveTrue(memberId);
        assertThat(reminders).hasSize(4);
        
        // 주기별로 확인
        assertThat(reminders).anyMatch(r -> r.getFrequencyDays() == 1);   // 매일
        assertThat(reminders).anyMatch(r -> r.getFrequencyDays() == 14);  // 2주
        assertThat(reminders).anyMatch(r -> r.getFrequencyDays() == 30);  // 매월
        assertThat(reminders).anyMatch(r -> r.getFrequencyDays() == 180); // 6개월
    }

    @Test
    @DisplayName("친구 초기화 시 기념일이 있으면 연간 알림이 생성되어야 한다")
    void 친구_초기화_시_기념일이_있으면_연간_알림이_생성되어야_한다() {
        // given
        UUID memberId = UUID.randomUUID();
        LocalDate anniversaryDate = LocalDate.of(2024, 12, 25);
        
        FriendCreateListRequest request = FriendCreateListRequest.builder()
                .friendList(List.of(
                        FriendRequest.builder()
                                .name("기념일 있는 친구")
                                .source(FriendSource.KAKAO)
                                .relation(FriendRelation.FRIEND)
                                .contactFrequency(FriendContactFrequency.builder()
                                        .contactWeek(FriendContactWeek.EVERY_WEEK)
                                        .build())
                                .anniversary(FriendAnniversaryCreateRequest.builder()
                                        .title("결혼기념일")
                                        .date(anniversaryDate)
                                        .build())
                                .build()
                ))
                .build();

        // when
        FriendCreateListResponse response = friendService.init(memberId, request);

        // then
        assertThat(response.getFriendList()).hasSize(1);
        
        List<ContactReminder> reminders = contactReminderRepository.findByMemberIdAndIsActiveTrue(memberId);
        assertThat(reminders).hasSize(2); // 연락 주기 알림 + 기념일 알림
        
        // 기념일 알림 확인
        ContactReminder anniversaryReminder = reminders.stream()
                .filter(r -> r.getReminderType() == ReminderType.ANNIVERSARY)
                .findFirst()
                .orElse(null);
                
        assertThat(anniversaryReminder).isNotNull();
        assertThat(anniversaryReminder.getFrequencyDays()).isEqualTo(365); // 1년 주기
        assertThat(anniversaryReminder.getAnniversaryId()).isNotNull();
    }

    @Test
    @DisplayName("친구 초기화 시 생일이 있으면 연간 알림이 생성되어야 한다")
    void 친구_초기화_시_생일이_있으면_연간_알림이_생성되어야_한다() {
        // given
        UUID memberId = UUID.randomUUID();
        LocalDate birthday = LocalDate.of(1990, 5, 15);
        
        FriendCreateListRequest request = FriendCreateListRequest.builder()
                .friendList(List.of(
                        FriendRequest.builder()
                                .name("생일 있는 친구")
                                .source(FriendSource.KAKAO)
                                .relation(FriendRelation.FRIEND)
                                .contactFrequency(FriendContactFrequency.builder()
                                        .contactWeek(FriendContactWeek.EVERY_MONTH)
                                        .build())
                                .birthDay(birthday)
                                .build()
                ))
                .build();

        // when
        FriendCreateListResponse response = friendService.init(memberId, request);

        // then
        assertThat(response.getFriendList()).hasSize(1);
        
        List<ContactReminder> reminders = contactReminderRepository.findByMemberIdAndIsActiveTrue(memberId);
        assertThat(reminders).hasSize(2); // 연락 주기 알림 + 생일 알림
        
        // 생일 알림 확인
        ContactReminder birthdayReminder = reminders.stream()
                .filter(r -> r.getReminderType() == ReminderType.BIRTHDAY)
                .findFirst()
                .orElse(null);
                
        assertThat(birthdayReminder).isNotNull();
        assertThat(birthdayReminder.getFrequencyDays()).isEqualTo(365); // 1년 주기
    }

    @Test
    @DisplayName("친구 초기화 시 모든 정보가 있으면 3개의 알림이 생성되어야 한다")
    void 친구_초기화_시_모든_정보가_있으면_3개의_알림이_생성되어야_한다() {
        // given
        UUID memberId = UUID.randomUUID();
        
        FriendCreateListRequest request = FriendCreateListRequest.builder()
                .friendList(List.of(
                        FriendRequest.builder()
                                .name("모든 정보 있는 친구")
                                .source(FriendSource.KAKAO)
                                .relation(FriendRelation.FRIEND)
                                .contactFrequency(FriendContactFrequency.builder()
                                        .contactWeek(FriendContactWeek.EVERY_WEEK)
                                        .dayOfWeek(DayOfWeek.SUNDAY)
                                        .build())
                                .birthDay(LocalDate.of(1990, 3, 15))
                                .anniversary(FriendAnniversaryCreateRequest.builder()
                                        .title("첫만남 기념일")
                                        .date(LocalDate.of(2020, 6, 1))
                                        .build())
                                .phone("01012345678")
                                .memo("중요한 친구")
                                .build()
                ))
                .build();

        // when
        FriendCreateListResponse response = friendService.init(memberId, request);

        // then
        assertThat(response.getFriendList()).hasSize(1);
        
        List<ContactReminder> reminders = contactReminderRepository.findByMemberIdAndIsActiveTrue(memberId);
        assertThat(reminders).hasSize(3); // 연락 주기 + 생일 + 기념일
        
        // 각 타입별로 하나씩 있는지 확인
        assertThat(reminders).anyMatch(r -> r.getReminderType() == ReminderType.CONTACT);
        assertThat(reminders).anyMatch(r -> r.getReminderType() == ReminderType.BIRTHDAY);
        assertThat(reminders).anyMatch(r -> r.getReminderType() == ReminderType.ANNIVERSARY);
        
        // 모든 알림이 활성화 상태인지 확인
        assertThat(reminders).allMatch(ContactReminder::getIsActive);
    }
}