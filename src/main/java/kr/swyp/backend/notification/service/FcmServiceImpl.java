package kr.swyp.backend.notification.service;

import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import kr.swyp.backend.common.config.FcmProperties;
import kr.swyp.backend.notification.dto.FcmNotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmServiceImpl implements FcmService {

    private final FcmProperties fcmProperties;

    @Override
    public void sendNotification(FcmNotificationRequest request) {
        if (!fcmProperties.isEnabled()) {
            log.debug("FCM is disabled. Skipping notification.");
            return;
        }

        try {
            Message.Builder messageBuilder = Message.builder()
                    .setToken(request.getFcmToken())
                    .setNotification(Notification.builder()
                            .setTitle(request.getTitle())
                            .setBody(request.getBody())
                            .build());

            // 데이터 페이로드 추가
            if (request.getData() != null && !request.getData().isEmpty()) {
                messageBuilder.putAllData(request.getData());
            }

            // Friend ID 추가 (iOS에서 알림 클릭 시 친구 상세 페이지로 이동에 사용)
            if (request.getFriendId() != null) {
                messageBuilder.putData("friendId", request.getFriendId().toString());
            }

            // body를 data 필드에도 추가 (iOS 요구사항)
            messageBuilder.putData("body", request.getBody());

            // date 추가 (iOS 요구사항 - ISO8601 형식)
            String dateString = ZonedDateTime.now()
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            messageBuilder.putData("date", dateString);

            // iOS APNS 설정 (sound, badge)
            ApnsConfig apnsConfig = ApnsConfig.builder()
                    .setAps(Aps.builder()
                            .setSound("default")
                            .setBadge(1)
                            .build())
                    .build();
            messageBuilder.setApnsConfig(apnsConfig);

            Message message = messageBuilder.build();
            String response = FirebaseMessaging.getInstance().send(message);

            log.info("Successfully sent FCM notification. Response: {}", response);
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM notification to token: {}", request.getFcmToken(), e);
        }
    }

    @Override
    public void sendFriendReminder(String fcmToken, UUID friendId, String friendName,
            String reason) {
        if (fcmToken == null || fcmToken.isEmpty()) {
            log.debug("FCM token is empty. Skipping notification.");
            return;
        }

        Map<String, String> data = new HashMap<>();
        data.put("type", "FRIEND_REMINDER");
        data.put("reason", reason);

        String body = friendName + "님과 연락할 시간이에요!";

        FcmNotificationRequest request = FcmNotificationRequest.builder()
                .fcmToken(fcmToken)
                .title("친구 챙기기")
                .body(body)
                .friendId(friendId)
                .data(data)
                .build();

        sendNotification(request);
    }
}
