package kr.swyp.backend.notification.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import kr.swyp.backend.member.dto.MemberDetails;
import kr.swyp.backend.notification.dto.ForceSendNotificationRequest;
import kr.swyp.backend.notification.dto.NotificationResponseDto;
import kr.swyp.backend.notification.dto.SendTestNotificationRequest;
import kr.swyp.backend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
@PreAuthorize("hasAnyAuthority('USER')")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 임시 테스트용 FCM 알림 전송 엔드포인트
     * JWT 토큰으로 인증된 사용자에게 FCM 푸시 알림을 전송합니다.
     */
    @PostMapping("/test/send")
    public ResponseEntity<Map<String, String>> sendTestNotification(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @Valid @RequestBody SendTestNotificationRequest request) {

        notificationService.sendTestNotificationToMember(
                memberDetails.getMemberId(),
                request.getTitle(),
                request.getBody()
        );

        return ResponseEntity.ok(Map.of(
                "message", "FCM 알림이 성공적으로 전송되었습니다.",
                "memberId", memberDetails.getMemberId().toString()
        ));
    }

    /**
     * 특정 사용자에게 강제로 FCM 알림을 전송하는 엔드포인트 (관리자용).
     * memberId를 직접 지정하여 해당 사용자에게 푸시 알림을 전송합니다.
     */
    @PostMapping("/force/send")
    public ResponseEntity<Map<String, String>> forceSendNotification(
            @Valid @RequestBody ForceSendNotificationRequest request) {

        notificationService.forceSendNotification(
                request.getMemberId(),
                request.getTitle(),
                request.getBody(),
                request.getFriendId()
        );

        return ResponseEntity.ok(Map.of(
                "message", "FCM 알림이 성공적으로 전송되었습니다.",
                "memberId", request.getMemberId().toString()
        ));
    }

    /**
     * 사용자의 알림 목록을 조회합니다.
     *
     * @param unreadOnly 읽지 않은 알림만 조회할지 여부 (기본값: false)
     */
    @GetMapping
    public ResponseEntity<List<NotificationResponseDto>> getNotifications(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {

        List<NotificationResponseDto> notifications;
        if (unreadOnly) {
            notifications = notificationService.getUnreadNotifications(memberDetails.getMemberId());
        } else {
            notifications = notificationService.getNotifications(memberDetails.getMemberId());
        }

        return ResponseEntity.ok(notifications);
    }

    /**
     * 특정 알림을 읽음 처리합니다.
     */
    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Map<String, String>> markAsRead(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long notificationId) {

        notificationService.markAsRead(memberDetails.getMemberId(), notificationId);

        return ResponseEntity.ok(Map.of(
                "message", "알림을 읽음 처리하였습니다.",
                "notificationId", notificationId.toString()
        ));
    }
}
