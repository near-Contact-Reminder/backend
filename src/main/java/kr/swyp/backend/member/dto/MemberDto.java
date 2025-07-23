package kr.swyp.backend.member.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;
import kr.swyp.backend.member.domain.Member;
import kr.swyp.backend.member.domain.MemberSocialLoginInfo;
import kr.swyp.backend.member.enums.SocialLoginProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class MemberDto {

    @Getter
    @Builder
    public static class MemberInfoResponse {

        private UUID memberId;
        private String username;
        private String nickname;
        private String imageUrl;
        private LocalDateTime notificationAgreedAt; // 알림 동의 여부
        private SocialLoginProviderType providerType; // 소셜 로그인 제공자

        public static MemberInfoResponse fromEntity(Member member,
                MemberSocialLoginInfo socialLoginInfo) {
            return MemberInfoResponse.builder()
                    .memberId(member.getMemberId())
                    .username(member.getUsername())
                    .nickname(member.getNickname())
                    .imageUrl(member.getImageUrl())
                    .notificationAgreedAt(member.getNotificationAgreedAt())
                    .providerType(socialLoginInfo.getProviderType())
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MemberWithdrawRequest {

        @NotNull(message = "탈퇴 사유는 필수입니다.")
        private String reasonType;
        private String customReason;
    }

    @Getter
    @Builder
    public static class CheckRateResponse {

        private Integer checkRate;

        public static CheckRateResponse fromEntity(int checkRate) {
            return CheckRateResponse.builder()
                    .checkRate(checkRate)
                    .build();
        }
    }
}

