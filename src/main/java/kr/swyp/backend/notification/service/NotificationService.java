package kr.swyp.backend.notification.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import kr.swyp.backend.member.domain.Member;
import kr.swyp.backend.member.repository.MemberRepository;
import kr.swyp.backend.notification.dto.FcmNotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final MemberRepository memberRepository;
    private final FcmService fcmService;

    /**
     * 특정 회원에게 테스트 FCM 알림을 전송합니다.
     *
     * @param memberId 회원 ID
     * @param title    알림 제목
     * @param body     알림 내용
     */
    @Transactional(readOnly = true)
    public void sendTestNotificationToMember(UUID memberId, String title, String body) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        if (member.getFcmToken() == null || member.getFcmToken().isEmpty()) {
            log.warn("Member {} does not have FCM token", memberId);
            throw new IllegalStateException("FCM 토큰이 등록되지 않았습니다.");
        }

        Map<String, String> data = new HashMap<>();
        data.put("type", "TEST_NOTIFICATION");

        FcmNotificationRequest request = FcmNotificationRequest.builder()
                .fcmToken(member.getFcmToken())
                .title(title)
                .body(body)
                .data(data)
                .build();

        fcmService.sendNotification(request);
        log.info("Test notification sent to member: {}", memberId);
    }

    /**
     * 특정 회원에게 강제로 FCM 알림을 전송합니다 (관리자용).
     *
     * @param memberId 회원 ID
     * @param title    알림 제목
     * @param body     알림 내용
     * @param friendId 친구 ID (선택사항, 알림 클릭 시 친구 상세 페이지로 이동)
     */
    @Transactional(readOnly = true)
    public void forceSendNotification(UUID memberId, String title, String body, UUID friendId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        if (member.getFcmToken() == null || member.getFcmToken().isEmpty()) {
            log.warn("Member {} does not have FCM token", memberId);
            throw new IllegalStateException("FCM 토큰이 등록되지 않았습니다.");
        }

        Map<String, String> data = new HashMap<>();
        data.put("type", "FORCE_NOTIFICATION");

        FcmNotificationRequest request = FcmNotificationRequest.builder()
                .fcmToken(member.getFcmToken())
                .title(title)
                .body(body)
                .friendId(friendId)
                .data(data)
                .build();

        fcmService.sendNotification(request);
        log.info("Force notification sent to member: {}, friendId: {}", memberId, friendId);
    }
}
