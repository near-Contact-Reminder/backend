package kr.swyp.backend.messaging.dto;

import jakarta.validation.constraints.NotNull;
import kr.swyp.backend.messaging.enums.AppPushTokenOsType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class TokenDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterAppPushTokenRequest {

        @NotNull
        private String token;

        @NotNull
        private AppPushTokenOsType osType;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnregisterAppPushTokenRequest {

        @NotNull
        private String token;
    }
}
