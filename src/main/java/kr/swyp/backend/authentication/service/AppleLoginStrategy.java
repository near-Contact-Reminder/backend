package kr.swyp.backend.authentication.service;

import kr.swyp.backend.authentication.dto.SocialLoginDto;
import kr.swyp.backend.authentication.dto.SocialLoginDto.AppleTokenVerificationResult;
import kr.swyp.backend.authentication.dto.SocialLoginDto.AppleUserInfo;
import kr.swyp.backend.authentication.dto.SocialLoginDto.UserInfo;
import kr.swyp.backend.authentication.utils.AppleLoginUtil;
import kr.swyp.backend.member.enums.SocialLoginProviderType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppleLoginStrategy implements SocialLoginStrategy {

    private final AppleLoginUtil appleLoginUtil;

    @Override
    public UserInfo getUserInfo(SocialLoginDto.Request request) {
        AppleTokenVerificationResult verificationResult = appleLoginUtil.verifyAppleToken(
                request.getIdentityToken());
        String appleUserId = verificationResult.getSubject();

        // Apple의 경우, 초기 로그인 시에만 email 정보를 얻을 수 있는 경우가 많습니다.
        // authorizationCode를 이용해 추가 정보를 조회하는 로직을 여기에 추가할 수 있습니다.
        AppleUserInfo userInfo = appleLoginUtil.getUserInfoFromAuthCode(
                request.getAuthorizationCode());

        return UserInfo.builder()
                .providerId(appleUserId)
                .email(userInfo.getEmail())
                .nickname("하루") // Apple에서는 닉네임을 제공하지 않으므로 기본값 설정
                .build();
    }

    @Override
    public SocialLoginProviderType getProviderType() {
        return SocialLoginProviderType.APPLE;
    }
}
