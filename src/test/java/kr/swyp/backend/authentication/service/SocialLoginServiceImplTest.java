package kr.swyp.backend.authentication.service;

import static kr.swyp.backend.authentication.dto.SocialLoginDto.Request;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.swyp.backend.authentication.dto.MemberInfo;
import kr.swyp.backend.authentication.dto.SocialLoginDto;
import kr.swyp.backend.member.domain.Member;
import kr.swyp.backend.member.domain.MemberSocialLoginInfo;
import kr.swyp.backend.member.enums.RoleType;
import kr.swyp.backend.member.enums.SocialLoginProviderType;
import kr.swyp.backend.member.repository.MemberRepository;
import kr.swyp.backend.member.repository.MemberSocialLoginInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SocialLoginServiceImplTest {

    private SocialLoginServiceImpl socialLoginService;
    private MemberRepository memberRepository;
    private MemberSocialLoginInfoRepository memberSocialLoginInfoRepository;
    private KakaoLoginStrategy kakaoLoginStrategy;
    private AppleLoginStrategy appleLoginStrategy;

    @BeforeEach
    void setUp() {
        memberRepository = Mockito.mock(MemberRepository.class);
        memberSocialLoginInfoRepository = Mockito.mock(MemberSocialLoginInfoRepository.class);
        kakaoLoginStrategy = Mockito.mock(KakaoLoginStrategy.class);
        appleLoginStrategy = Mockito.mock(AppleLoginStrategy.class);

        when(kakaoLoginStrategy.getProviderType()).thenReturn(SocialLoginProviderType.KAKAO);
        when(appleLoginStrategy.getProviderType()).thenReturn(SocialLoginProviderType.APPLE);

        socialLoginService = new SocialLoginServiceImpl(
                List.of(kakaoLoginStrategy, appleLoginStrategy),
                memberSocialLoginInfoRepository,
                memberRepository
        );
    }

    @Test
    @DisplayName("카카오 신규 유저는 로그인이 성공해야 한다")
    void 카카오_신규_유저는_로그인이_성공해야_한다() {
        // given
        SocialLoginProviderType providerType = SocialLoginProviderType.KAKAO;
        SocialLoginDto.Request request = SocialLoginDto.Request.builder().accessToken("test_token")
                .build();
        SocialLoginDto.UserInfo newUserInfo = SocialLoginDto.UserInfo.builder()
                .providerId("kakao_12345")
                .email("new.user@kakao.com")
                .nickname("신규유저")
                .build();

        when(kakaoLoginStrategy.getUserInfo(request)).thenReturn(newUserInfo);
        when(memberSocialLoginInfoRepository.findByProviderIdAndProviderType(
                newUserInfo.getProviderId(), providerType))
                .thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class))).thenAnswer(
                invocation -> invocation.getArgument(0));

        // when
        MemberInfo memberInfo = socialLoginService.login(providerType, request);

        // then
        assertNotNull(memberInfo);
        assertEquals(newUserInfo.getEmail(), memberInfo.getUsername());
        assertEquals(RoleType.USER, memberInfo.getRoleType());

        verify(kakaoLoginStrategy, times(1)).getUserInfo(request);
        verify(memberRepository, times(1)).save(any(Member.class));
        verify(memberSocialLoginInfoRepository, times(1)).save(any(MemberSocialLoginInfo.class));
    }

    @Test
    @DisplayName("애플 기존 유저는 로그인이 성공해야 한다")
    void 애플_기존_유저는_로그인이_성공해야_한다() {
        // given
        SocialLoginProviderType providerType = SocialLoginProviderType.APPLE;
        SocialLoginDto.Request request = SocialLoginDto.Request.builder().identityToken("id_token")
                .build();
        SocialLoginDto.UserInfo existingUserInfo = SocialLoginDto.UserInfo.builder()
                .providerId("apple_67890")
                .email("existing.user@apple.com")
                .nickname("기존유저")
                .build();

        Member existingMember = Member.builder().memberId(UUID.randomUUID())
                .username(existingUserInfo.getEmail()).build();
        MemberSocialLoginInfo existingSocialInfo = MemberSocialLoginInfo.builder()
                .member(existingMember)
                .providerId(existingUserInfo.getProviderId())
                .providerType(providerType)
                .build();

        when(appleLoginStrategy.getUserInfo(request)).thenReturn(existingUserInfo);
        when(memberSocialLoginInfoRepository.findByProviderIdAndProviderType(
                existingUserInfo.getProviderId(), providerType))
                .thenReturn(Optional.of(existingSocialInfo));

        // when
        MemberInfo memberInfo = socialLoginService.login(providerType, request);

        // then
        assertNotNull(memberInfo);
        assertEquals(existingMember.getMemberId(), memberInfo.getMemberId());
        assertEquals(existingMember.getUsername(), memberInfo.getUsername());

        verify(appleLoginStrategy, times(1)).getUserInfo(request);
        verify(memberRepository, never()).save(any(Member.class));
        verify(memberSocialLoginInfoRepository, never()).save(any(MemberSocialLoginInfo.class));
    }

    @Test
    @DisplayName("지원하지 않는 소셜 로그인은 예외가 발생해야 한다")
    void 지원하지_않는_소셜_로그인은_예외가_발생해야_한다() {
        // given
        socialLoginService = new SocialLoginServiceImpl(List.of(), memberSocialLoginInfoRepository,
                memberRepository);
        SocialLoginProviderType providerType = SocialLoginProviderType.KAKAO;
        Request request = Request.builder().build();

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            socialLoginService.login(providerType, request);
        });
    }
}