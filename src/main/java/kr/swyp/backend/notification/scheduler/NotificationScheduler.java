package kr.swyp.backend.notification.scheduler;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import kr.swyp.backend.friend.domain.Friend;
import kr.swyp.backend.friend.domain.FriendAnniversary;
import kr.swyp.backend.friend.repository.FriendAnniversaryRepository;
import kr.swyp.backend.friend.repository.FriendRepository;
import kr.swyp.backend.member.domain.Member;
import kr.swyp.backend.member.repository.MemberRepository;
import kr.swyp.backend.notification.service.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final FriendRepository friendRepository;
    private final FriendAnniversaryRepository friendAnniversaryRepository;
    private final MemberRepository memberRepository;
    private final FcmService fcmService;

    /**
     * 매일 오전 9시에 실행되는 친구 챙김 알림 스케줄러.
     * Cron: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    @Transactional
    public void sendDailyFriendReminders() {
        log.info("Starting daily friend reminder notifications...");

        LocalDate today = LocalDate.now(KOREA_ZONE);
        Set<UUID> processedFriendIds = new HashSet<>();

        // 1. nextContactAt이 오늘인 친구들 처리
        List<Friend> friendsToContact = friendRepository.findAllByNextContactAt(today);
        log.info("Found {} friends to contact today based on nextContactAt",
                friendsToContact.size());

        for (Friend friend : friendsToContact) {
            sendReminderForFriend(friend, "연락 예정일입니다.");
            processedFriendIds.add(friend.getFriendId());
        }

        // 2. 오늘이 기념일인 친구들 처리
        int month = today.getMonthValue();
        int day = today.getDayOfMonth();
        List<FriendAnniversary> todayAnniversaries =
                friendAnniversaryRepository.findAllByMonthAndDay(month, day);
        log.info("Found {} anniversaries today", todayAnniversaries.size());

        for (FriendAnniversary anniversary : todayAnniversaries) {
            UUID friendId = anniversary.getFriendId();

            // 이미 연락 예정일로 알림을 보낸 친구는 스킵
            if (processedFriendIds.contains(friendId)) {
                continue;
            }

            friendRepository.findById(friendId).ifPresent(friend -> {
                String reason = "오늘은 " + anniversary.getTitle() + "입니다.";
                sendReminderForFriend(friend, reason);
                processedFriendIds.add(friendId);
            });
        }

        log.info("Daily friend reminder notifications completed. Total notifications sent: {}",
                processedFriendIds.size());
    }

    private void sendReminderForFriend(Friend friend, String reason) {
        UUID memberId = friend.getMemberId();

        memberRepository.findById(memberId).ifPresent(member -> {
            // 알림 동의를 한 회원이고, FCM 토큰이 있는 경우에만 알림 전송
            if (member.getNotificationAgreedAt() != null && member.getFcmToken() != null
                    && !member.getFcmToken().isEmpty()) {

                fcmService.sendFriendReminder(
                        member.getFcmToken(),
                        friend.getFriendId(),
                        friend.getName(),
                        reason
                );

                // 알림 트리거 카운트 증가
                friend.updateAlarmTriggerCount();

                log.debug("Sent reminder for friend {} to member {}",
                        friend.getName(), memberId);
            } else {
                log.debug("Skipped sending reminder for friend {} to member {} "
                                + "(no notification consent or FCM token)",
                        friend.getName(), memberId);
            }
        });
    }
}
