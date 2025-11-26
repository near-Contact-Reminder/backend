package kr.swyp.backend.authentication.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import feign.form.FormProperty;
import java.util.List;
import kr.swyp.backend.member.enums.SocialLoginProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

public class SocialLoginDto {

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.EXISTING_PROPERTY,
            property = "providerType",
            visible = true
    )
    @JsonSubTypes({
            @JsonSubTypes.Type(value = KakaoSocialLoginRequest.class, name = "KAKAO"),
            @JsonSubTypes.Type(value = AppleSocialLoginRequest.class, name = "APPLE")
    })
    @Getter
    @SuperBuilder
    @EqualsAndHashCode
    @AllArgsConstructor
    @NoArgsConstructor
    public abstract static class AbstractSocialLoginRequest {

        private SocialLoginProviderType providerType;
    }

    @Getter
    @SuperBuilder
    @EqualsAndHashCode(callSuper = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KakaoSocialLoginRequest extends AbstractSocialLoginRequest {

        private String accessToken;
    }

    @Getter
    @SuperBuilder
    @EqualsAndHashCode(callSuper = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppleSocialLoginRequest extends AbstractSocialLoginRequest {

        private String identityToken;
        private String authorizationCode;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KakaoSocialLoginResponse {

        private Long id;

        @JsonProperty("kakao_account")
        private KakaoAccount kakaoAccount;

        @Getter
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class KakaoAccount {

            private Profile profile;
            private String email;

            @Getter
            @Builder
            @AllArgsConstructor
            @NoArgsConstructor
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Profile {

                private String nickname;
            }
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AppleTokenRequest {

        @FormProperty("client_id")
        private String clientId;

        @FormProperty("client_secret")
        private String clientSecret;

        private String code;

        @FormProperty("grant_type")
        private String grantType;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AppleTokenResponse {

        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("token_type")
        private String tokenType;

        @JsonProperty("expires_in")
        private Long expiresIn;

        @JsonProperty("refresh_token")
        private String refreshToken;

        @JsonProperty("id_token")
        private String idToken;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AppleUserInfo {

        private String email;
        private boolean emailVerified;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AppleTokenVerificationResult {

        private Boolean valid;
        private String subject;
        private String email;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JwtHeader {

        private String kid;
        private String alg;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ApplePublicKeyResponse {

        private List<ApplePublicKey> keys;

        @Getter
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class ApplePublicKey {

            private String kty;
            private String kid;
            private String use;
            private String alg;
            @SuppressWarnings("checkstyle:MemberName")
            private String n;
            @SuppressWarnings("checkstyle:MemberName")
            private String e;

        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserInfo {
        private String providerId;
        private String email;
        private String nickname;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Request {
        private String accessToken;
        private String identityToken;
        private String authorizationCode;
    }
}

