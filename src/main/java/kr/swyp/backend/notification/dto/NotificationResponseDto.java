package kr.swyp.backend.notification.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import kr.swyp.backend.notification.domain.Notification;
import kr.swyp.backend.notification.enums.NotificationType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationResponseDto {

    private Long notificationId;
    private UUID friendId;
    private String friendName;
    private NotificationType type;
    private String title;
    private String body;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public static NotificationResponseDto from(Notification notification, String friendName) {
        return NotificationResponseDto.builder()
                .notificationId(notification.getNotificationId())
                .friendId(notification.getFriendId())
                .friendName(friendName)
                .type(notification.getType())
                .title(notification.getTitle())
                .body(notification.getBody())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
