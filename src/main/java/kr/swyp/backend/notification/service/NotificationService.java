package kr.swyp.backend.notification.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import kr.swyp.backend.friend.domain.Friend;
import kr.swyp.backend.friend.repository.FriendRepository;
import kr.swyp.backend.member.domain.Member;
import kr.swyp.backend.member.repository.MemberRepository;
import kr.swyp.backend.notification.domain.Notification;
import kr.swyp.backend.notification.dto.FcmNotificationRequest;
import kr.swyp.backend.notification.dto.NotificationResponseDto;
import kr.swyp.backend.notification.enums.NotificationType;
import kr.swyp.backend.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final MemberRepository memberRepository;
    private final FriendRepository friendRepository;
    private final FcmService fcmService;
    private final NotificationRepository notificationRepository;

    /**
     * 특정 회원에게 테스트 FCM 알림을 전송합니다.
     *
     * @param memberId 회원 ID
     * @param title    알림 제목
     * @param body     알림 내용
     */
    @Transactional(readOnly = true)
    public void sendTestNotificationToMember(UUID memberId, String title, String body) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        if (member.getFcmToken() == null || member.getFcmToken().isEmpty()) {
            log.warn("Member {} does not have FCM token", memberId);
            throw new IllegalStateException("FCM 토큰이 등록되지 않았습니다.");
        }

        Map<String, String> data = new HashMap<>();
        data.put("type", "TEST_NOTIFICATION");

        FcmNotificationRequest request = FcmNotificationRequest.builder()
                .fcmToken(member.getFcmToken())
                .title(title)
                .body(body)
                .data(data)
                .build();

        fcmService.sendNotification(request);
        log.info("Test notification sent to member: {}", memberId);
    }

    /**
     * 특정 회원에게 강제로 FCM 알림을 전송합니다 (관리자용).
     *
     * @param memberId 회원 ID
     * @param title    알림 제목
     * @param body     알림 내용
     * @param friendId 친구 ID (선택사항, 알림 클릭 시 친구 상세 페이지로 이동)
     */
    @Transactional(readOnly = true)
    public void forceSendNotification(UUID memberId, String title, String body, UUID friendId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        if (member.getFcmToken() == null || member.getFcmToken().isEmpty()) {
            log.warn("Member {} does not have FCM token", memberId);
            throw new IllegalStateException("FCM 토큰이 등록되지 않았습니다.");
        }

        Map<String, String> data = new HashMap<>();
        data.put("type", "FORCE_NOTIFICATION");

        FcmNotificationRequest request = FcmNotificationRequest.builder()
                .fcmToken(member.getFcmToken())
                .title(title)
                .body(body)
                .friendId(friendId)
                .data(data)
                .build();

        fcmService.sendNotification(request);
        log.info("Force notification sent to member: {}, friendId: {}", memberId, friendId);
    }

    /**
     * 알림을 DB에 저장합니다.
     *
     * @param memberId 회원 ID
     * @param friendId 친구 ID (선택)
     * @param type     알림 타입
     * @param title    알림 제목
     * @param body     알림 내용
     * @return 저장된 알림
     */
    @Transactional
    public Notification saveNotification(UUID memberId, UUID friendId, NotificationType type,
            String title, String body) {
        Notification notification = Notification.builder()
                .memberId(memberId)
                .friendId(friendId)
                .type(type)
                .title(title)
                .body(body)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Notification saved: id={}, memberId={}, type={}", saved.getNotificationId(),
                memberId, type);
        return saved;
    }

    /**
     * 회원의 알림 목록을 조회합니다.
     *
     * @param memberId 회원 ID
     * @return 알림 목록
     */
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getNotifications(UUID memberId) {
        List<Notification> notifications =
                notificationRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
        Map<UUID, String> friendNameMap = getFriendNameMap(memberId);

        return notifications.stream()
                .map(notification -> NotificationResponseDto.from(
                        notification,
                        notification.getFriendId() != null
                                ? friendNameMap.get(notification.getFriendId())
                                : null))
                .toList();
    }

    /**
     * 회원의 읽지 않은 알림 목록을 조회합니다.
     *
     * @param memberId 회원 ID
     * @return 읽지 않은 알림 목록
     */
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getUnreadNotifications(UUID memberId) {
        List<Notification> notifications =
                notificationRepository.findByMemberIdAndIsReadFalseOrderByCreatedAtDesc(memberId);
        Map<UUID, String> friendNameMap = getFriendNameMap(memberId);

        return notifications.stream()
                .map(notification -> NotificationResponseDto.from(
                        notification,
                        notification.getFriendId() != null
                                ? friendNameMap.get(notification.getFriendId())
                                : null))
                .toList();
    }

    /**
     * 회원의 친구 ID와 이름 매핑을 조회합니다.
     *
     * @param memberId 회원 ID
     * @return 친구 ID와 이름 매핑
     */
    private Map<UUID, String> getFriendNameMap(UUID memberId) {
        return friendRepository.findAllByMemberId(memberId).stream()
                .collect(Collectors.toMap(Friend::getFriendId, Friend::getName));
    }

    /**
     * 알림을 읽음 처리합니다.
     *
     * @param memberId       회원 ID
     * @param notificationId 알림 ID
     */
    @Transactional
    public void markAsRead(UUID memberId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));

        if (!notification.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("해당 알림에 접근할 권한이 없습니다.");
        }

        notification.markAsRead();
        log.info("Notification marked as read: id={}, memberId={}", notificationId, memberId);
    }
}
