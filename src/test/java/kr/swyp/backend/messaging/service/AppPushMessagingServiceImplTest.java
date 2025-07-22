package kr.swyp.backend.messaging.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;
import kr.swyp.backend.member.domain.Member;
import kr.swyp.backend.member.enums.RoleType;
import kr.swyp.backend.member.repository.MemberRepository;
import kr.swyp.backend.messaging.dto.TokenDto.RegisterAppPushTokenRequest;
import kr.swyp.backend.messaging.dto.TokenDto.UnregisterAppPushTokenRequest;
import kr.swyp.backend.messaging.enums.AppTokenOsType;
import kr.swyp.backend.messaging.repository.AppPushTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;


@SpringBootTest
@Transactional
@ActiveProfiles("test")
class AppPushMessagingServiceImplTest {

    @Autowired
    private AppPushMessagingServiceImpl appPushMessagingService;

    @Autowired
    private AppPushTokenRepository appPushTokenRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("앱 푸시 토큰 등록을 할 수 있어야 한다.")
    void 앱_푸시_토큰_등록을_할_수_있어야_한다() {
        // given
        Member member = createMember("test", "test");
        String token = "test-token";
        AppTokenOsType osType = AppTokenOsType.IOS;

        // when
        appPushMessagingService.registerDevice(member.getMemberId(),
                RegisterAppPushTokenRequest.builder()
                        .token(token)
                        .osType(osType)
                        .build());

        // then
        assertThat(appPushTokenRepository.findByMemberId(member.getMemberId()).get()
                .getToken()).isEqualTo(token);

    }

    @Test
    @DisplayName("앱 푸시 토큰 등록 시 회원의 알림 동의 시간을 업데이트해야 한다.")
    void 앱_푸시_토큰_등록_시_회원의_알림_동의_시간을_업데이트해야_한다() {
        // given
        Member member = createMember("test", "test");
        String token = "test-token";
        AppTokenOsType osType = AppTokenOsType.IOS;

        // when
        appPushMessagingService.registerDevice(member.getMemberId(),
                RegisterAppPushTokenRequest.builder()
                        .token(token)
                        .osType(osType)
                        .build());

        // then
        assertThat(memberRepository.findById(member.getMemberId()).get()
                .getNotificationAgreedAt()).isNotNull();
    }

    @Test
    @DisplayName("앱 푸시 토큰 등록 시 중복된 토큰이 있으면 기존 토큰을 업데이트해야 한다.")
    void 앱_푸시_토큰_등록_시_중복된_토큰이_있으면_기존_토큰을_업데이트해야_한다() {
        // given
        Member member = createMember("test", "test");
        String originalToken = "original-test-token";
        AppTokenOsType osType = AppTokenOsType.IOS;

        appPushMessagingService.registerDevice(member.getMemberId(),
                RegisterAppPushTokenRequest.builder()
                        .token(originalToken)
                        .osType(osType)
                        .build());

        String newToken = "new-test-token";
        AppTokenOsType newOsType = AppTokenOsType.ANDROID;

        // when
        appPushMessagingService.registerDevice(member.getMemberId(),
                RegisterAppPushTokenRequest.builder()
                        .token(newToken)
                        .osType(newOsType)
                        .build());

        // then
        assertThat(appPushTokenRepository.findByMemberId(member.getMemberId()).get()
                .getToken()).isEqualTo(newToken);
        assertThat(appPushTokenRepository.findByMemberId(member.getMemberId()).get()
                .getOsType()).isEqualTo(newOsType);
    }

    @Test
    @DisplayName("앱 푸시 토큰 등록 시 회원이 존재하지 않으면 예외가 발생해야 한다.")
    void 앱_푸시_토큰_등록_시_회원이_존재하지_않으면_예외가_발생해야_한다() {
        // given
        UUID nonExistentMemberId = UUID.randomUUID();
        String token = "test-token";
        AppTokenOsType osType = AppTokenOsType.IOS;

        // when
        Throwable throwable = catchThrowable(
                () -> appPushMessagingService.registerDevice(nonExistentMemberId,
                        RegisterAppPushTokenRequest.builder()
                                .token(token)
                                .osType(osType)
                                .build()));

        // then
        assertThat(throwable).isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("해당 회원을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("토큰 등록 취소를 할 수 있어야 한다.")
    void 토큰_등록_취소를_할_수_있어야_한다() {
        // given
        Member member = createMember("test", "test");
        String token = "test-token";
        AppTokenOsType osType = AppTokenOsType.IOS;

        appPushMessagingService.registerDevice(member.getMemberId(),
                RegisterAppPushTokenRequest.builder()
                        .token(token)
                        .osType(osType)
                        .build());

        // when
        appPushMessagingService.unregisterDevice(member.getMemberId(),
                UnregisterAppPushTokenRequest.builder()
                        .token(token)
                        .build());

        // then
        assertThat(appPushTokenRepository.findByMemberId(member.getMemberId())).isEmpty();
    }

    @Test
    @DisplayName("토큰 등록 취소 시 회원의 알림 동의 시간을 null로 업데이트해야 한다.")
    void 토큰_등록_취소_시_회원의_알림_동의_시간을_null로_업데이트해야_한다() {
        // given
        Member member = createMember("test", "test");
        String token = "test-token";
        AppTokenOsType osType = AppTokenOsType.IOS;
        appPushMessagingService.registerDevice(member.getMemberId(),
                RegisterAppPushTokenRequest.builder()
                        .token(token)
                        .osType(osType)
                        .build());

        // when
        appPushMessagingService.unregisterDevice(member.getMemberId(),
                UnregisterAppPushTokenRequest.builder()
                        .token(token)
                        .build());

        // then
        assertThat(memberRepository.findById(member.getMemberId()).get()
                .getNotificationAgreedAt()).isNull();
    }


    private Member createMember(String username, String nickname) {
        Member member = Member.builder()
                .username(username)
                .nickname(nickname)
                .password(" ")
                .isActive(true)
                .notificationAgreedAt(LocalDateTime.now())
                .build();

        member.addRole(RoleType.USER);

        return memberRepository.save(member);
    }
}