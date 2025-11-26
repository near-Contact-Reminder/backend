package kr.swyp.backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.LocalDateTime;
import java.util.UUID;
import kr.swyp.backend.member.domain.Member;
import kr.swyp.backend.member.enums.RoleType;
import kr.swyp.backend.member.repository.MemberRepository;
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
