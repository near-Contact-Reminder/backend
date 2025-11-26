package kr.swyp.backend.notification.dto;

import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FcmNotificationRequest {

    private String fcmToken;
    private String title;
    private String body;
    private Map<String, String> data;
    private UUID friendId;
}
