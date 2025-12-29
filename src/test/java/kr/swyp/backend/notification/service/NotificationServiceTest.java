package kr.swyp.backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import kr.swyp.backend.member.domain.Member;
import kr.swyp.backend.member.enums.RoleType;
import kr.swyp.backend.member.repository.MemberRepository;
import kr.swyp.backend.notification.domain.Notification;
import kr.swyp.backend.notification.dto.NotificationResponseDto;
import kr.swyp.backend.notification.enums.NotificationType;
import kr.swyp.backend.notification.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("특정 회원에게 강제 알림을 전송할 수 있어야 한다.")
    void 특정_회원에게_강제_알림을_전송할_수_있어야_한다() {
        // given
        Member member = createMemberWithFcmToken("test@test.com", "테스트유저", "test-fcm-token");
        String title = "테스트 제목";
        String body = "테스트 내용";
        UUID friendId = UUID.randomUUID();

        // when
        Throwable throwable = catchThrowable(
                () -> notificationService.forceSendNotification(
                        member.getMemberId(), title, body, friendId));

        // then
        // FCM이 비활성화된 테스트 환경에서는 예외 없이 정상 처리되어야 함
        assertThat(throwable).isNull();
    }

    @Test
    @DisplayName("friendId 없이도 강제 알림을 전송할 수 있어야 한다.")
    void friendId_없이도_강제_알림을_전송할_수_있어야_한다() {
        // given
        Member member = createMemberWithFcmToken("test@test.com", "테스트유저", "test-fcm-token");
        String title = "테스트 제목";
        String body = "테스트 내용";

        // when
        Throwable throwable = catchThrowable(
                () -> notificationService.forceSendNotification(
                        member.getMemberId(), title, body, null));

        // then
        assertThat(throwable).isNull();
    }

    @Test
    @DisplayName("FCM 토큰이 없는 회원에게는 강제 알림 전송이 실패해야 한다.")
    void FCM_토큰이_없는_회원에게는_강제_알림_전송이_실패해야_한다() {
        // given
        Member member = createMemberWithFcmToken("test@test.com", "테스트유저", null);
        String title = "테스트 제목";
        String body = "테스트 내용";

        // when
        Throwable throwable = catchThrowable(
                () -> notificationService.forceSendNotification(
                        member.getMemberId(), title, body, null));

        // then
        assertThat(throwable).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FCM 토큰이 등록되지 않았습니다.");
    }

    @Test
    @DisplayName("빈 FCM 토큰을 가진 회원에게는 강제 알림 전송이 실패해야 한다.")
    void 빈_FCM_토큰을_가진_회원에게는_강제_알림_전송이_실패해야_한다() {
        // given
        Member member = createMemberWithFcmToken("test@test.com", "테스트유저", "");
        String title = "테스트 제목";
        String body = "테스트 내용";

        // when
        Throwable throwable = catchThrowable(
                () -> notificationService.forceSendNotification(
                        member.getMemberId(), title, body, null));

        // then
        assertThat(throwable).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FCM 토큰이 등록되지 않았습니다.");
    }

    @Test
    @DisplayName("존재하지 않는 회원에게는 강제 알림 전송이 실패해야 한다.")
    void 존재하지_않는_회원에게는_강제_알림_전송이_실패해야_한다() {
        // given
        UUID nonExistentMemberId = UUID.randomUUID();
        String title = "테스트 제목";
        String body = "테스트 내용";

        // when
        Throwable throwable = catchThrowable(
                () -> notificationService.forceSendNotification(
                        nonExistentMemberId, title, body, null));

        // then
        assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("회원을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("알림을 저장할 수 있어야 한다.")
    void 알림을_저장할_수_있어야_한다() {
        // given
        Member member = createMemberWithFcmToken("test@test.com", "테스트유저", "test-fcm-token");
        UUID friendId = UUID.randomUUID();
        String title = "테스트 제목";
        String body = "테스트 내용";

        // when
        Notification saved = notificationService.saveNotification(
                member.getMemberId(), friendId, NotificationType.FRIEND_REMINDER, title, body);

        // then
        assertThat(saved.getNotificationId()).isNotNull();
        assertThat(saved.getMemberId()).isEqualTo(member.getMemberId());
        assertThat(saved.getFriendId()).isEqualTo(friendId);
        assertThat(saved.getType()).isEqualTo(NotificationType.FRIEND_REMINDER);
        assertThat(saved.getTitle()).isEqualTo(title);
        assertThat(saved.getBody()).isEqualTo(body);
        assertThat(saved.getIsRead()).isFalse();
    }

    @Test
    @DisplayName("friendId 없이도 알림을 저장할 수 있어야 한다.")
    void friendId_없이도_알림을_저장할_수_있어야_한다() {
        // given
        Member member = createMemberWithFcmToken("test@test.com", "테스트유저", "test-fcm-token");
        String title = "테스트 제목";
        String body = "테스트 내용";

        // when
        Notification saved = notificationService.saveNotification(
                member.getMemberId(), null, NotificationType.TEST_NOTIFICATION, title, body);

        // then
        assertThat(saved.getNotificationId()).isNotNull();
        assertThat(saved.getFriendId()).isNull();
    }

    @Test
    @DisplayName("회원의 알림 목록을 조회할 수 있어야 한다.")
    void 회원의_알림_목록을_조회할_수_있어야_한다() {
        // given
        Member member = createMemberWithFcmToken("test@test.com", "테스트유저", "test-fcm-token");
        notificationService.saveNotification(
                member.getMemberId(), null, NotificationType.FRIEND_REMINDER, "제목1", "내용1");
        notificationService.saveNotification(
                member.getMemberId(), null, NotificationType.ANNIVERSARY, "제목2", "내용2");

        // when
        List<NotificationResponseDto> notifications =
                notificationService.getNotifications(member.getMemberId());

        // then
        assertThat(notifications).hasSize(2);
    }

    @Test
    @DisplayName("다른 회원의 알림은 조회되지 않아야 한다.")
    void 다른_회원의_알림은_조회되지_않아야_한다() {
        // given
        Member member1 = createMemberWithFcmToken("test1@test.com", "테스트유저1", "test-fcm-token1");
        Member member2 = createMemberWithFcmToken("test2@test.com", "테스트유저2", "test-fcm-token2");

        notificationService.saveNotification(
                member1.getMemberId(), null, NotificationType.FRIEND_REMINDER, "제목1", "내용1");
        notificationService.saveNotification(
                member2.getMemberId(), null, NotificationType.FRIEND_REMINDER, "제목2", "내용2");

        // when
        List<NotificationResponseDto> notifications =
                notificationService.getNotifications(member1.getMemberId());

        // then
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getTitle()).isEqualTo("제목1");
    }

    @Test
    @DisplayName("읽지 않은 알림만 조회할 수 있어야 한다.")
    void 읽지_않은_알림만_조회할_수_있어야_한다() {
        // given
        Member member = createMemberWithFcmToken("test@test.com", "테스트유저", "test-fcm-token");
        Notification notification1 = notificationService.saveNotification(
                member.getMemberId(), null, NotificationType.FRIEND_REMINDER, "제목1", "내용1");
        notificationService.saveNotification(
                member.getMemberId(), null, NotificationType.ANNIVERSARY, "제목2", "내용2");

        // 첫 번째 알림 읽음 처리
        notificationService.markAsRead(member.getMemberId(), notification1.getNotificationId());

        // when
        List<NotificationResponseDto> unreadNotifications =
                notificationService.getUnreadNotifications(member.getMemberId());

        // then
        assertThat(unreadNotifications).hasSize(1);
        assertThat(unreadNotifications.get(0).getTitle()).isEqualTo("제목2");
    }

    @Test
    @DisplayName("알림을 읽음 처리할 수 있어야 한다.")
    void 알림을_읽음_처리할_수_있어야_한다() {
        // given
        Member member = createMemberWithFcmToken("test@test.com", "테스트유저", "test-fcm-token");
        Notification notification = notificationService.saveNotification(
                member.getMemberId(), null, NotificationType.FRIEND_REMINDER, "제목", "내용");

        assertThat(notification.getIsRead()).isFalse();

        // when
        notificationService.markAsRead(member.getMemberId(), notification.getNotificationId());

        // then
        Notification updated = notificationRepository.findById(notification.getNotificationId())
                .orElseThrow();
        assertThat(updated.getIsRead()).isTrue();
    }

    @Test
    @DisplayName("다른 회원의 알림은 읽음 처리할 수 없어야 한다.")
    void 다른_회원의_알림은_읽음_처리할_수_없어야_한다() {
        // given
        Member member1 = createMemberWithFcmToken("test1@test.com", "테스트유저1", "test-fcm-token1");
        Member member2 = createMemberWithFcmToken("test2@test.com", "테스트유저2", "test-fcm-token2");

        Notification notification = notificationService.saveNotification(
                member1.getMemberId(), null, NotificationType.FRIEND_REMINDER, "제목", "내용");

        // when
        Throwable throwable = catchThrowable(
                () -> notificationService.markAsRead(
                        member2.getMemberId(), notification.getNotificationId()));

        // then
        assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 알림에 접근할 권한이 없습니다.");
    }

    @Test
    @DisplayName("존재하지 않는 알림은 읽음 처리할 수 없어야 한다.")
    void 존재하지_않는_알림은_읽음_처리할_수_없어야_한다() {
        // given
        Member member = createMemberWithFcmToken("test@test.com", "테스트유저", "test-fcm-token");
        Long nonExistentNotificationId = 999999L;

        // when
        Throwable throwable = catchThrowable(
                () -> notificationService.markAsRead(member.getMemberId(), nonExistentNotificationId));

        // then
        assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("알림을 찾을 수 없습니다.");
    }

    private Member createMemberWithFcmToken(String username, String nickname, String fcmToken) {
        Member member = Member.builder()
                .username(username)
                .nickname(nickname)
                .password(" ")
                .isActive(true)
                .notificationAgreedAt(LocalDateTime.now())
                .fcmToken(fcmToken)
                .build();

        member.addRole(RoleType.USER);

        return memberRepository.save(member);
    }
}
