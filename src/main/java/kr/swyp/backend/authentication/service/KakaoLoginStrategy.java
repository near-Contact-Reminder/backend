package kr.swyp.backend.authentication.service;

import static kr.swyp.backend.authentication.dto.SocialLoginDto.Request;
import static kr.swyp.backend.authentication.dto.SocialLoginDto.UserInfo;

import com.fasterxml.jackson.core.JsonProcessingException;
import kr.swyp.backend.authentication.dto.SocialLoginDto.KakaoSocialLoginResponse;
import kr.swyp.backend.member.enums.SocialLoginProviderType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KakaoLoginStrategy implements SocialLoginStrategy {

    private final KakaoClientService kakaoClientService;

    @Override
    public UserInfo getUserInfo(Request request) {
        try {
            KakaoSocialLoginResponse response = kakaoClientService.getAccessTokenInfo(
                    request.getAccessToken());
            return UserInfo.builder()
                    .providerId(String.valueOf(response.getId()))
                    .email(response.getKakaoAccount().getEmail())
                    .nickname(response.getKakaoAccount().getProfile().getNickname())
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Kakao user info processing failed", e);
        }
    }

    @Override
    public SocialLoginProviderType getProviderType() {
        return SocialLoginProviderType.KAKAO;
    }
}
