package kr.swyp.backend.notification.service;

import java.util.UUID;
import kr.swyp.backend.notification.dto.FcmNotificationRequest;

public interface FcmService {

    /**
     * FCM 푸시 알림 전송.
     */
    void sendNotification(FcmNotificationRequest request);

    /**
     * 친구 챙김 알림 전송.
     */
    void sendFriendReminder(String fcmToken, UUID friendId, String friendName, String reason);
}
