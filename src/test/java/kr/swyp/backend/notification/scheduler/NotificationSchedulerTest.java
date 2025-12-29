package kr.swyp.backend.notification.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.TimeZone;
import kr.swyp.backend.friend.domain.Friend;
import kr.swyp.backend.friend.domain.FriendAnniversary;
import kr.swyp.backend.friend.domain.FriendContactFrequency;
import kr.swyp.backend.friend.enums.FriendContactWeek;
import kr.swyp.backend.friend.repository.FriendAnniversaryRepository;
import kr.swyp.backend.friend.repository.FriendRepository;
import kr.swyp.backend.member.domain.Member;
import kr.swyp.backend.member.enums.RoleType;
import kr.swyp.backend.member.repository.MemberRepository;
import kr.swyp.backend.notification.domain.Notification;
import kr.swyp.backend.notification.enums.NotificationType;
import kr.swyp.backend.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class NotificationSchedulerTest {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    @BeforeAll
    static void setUpTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

    @Autowired
    private NotificationScheduler notificationScheduler;

    @Autowired
    private FriendRepository friendRepository;

    @Autowired
    private FriendAnniversaryRepository friendAnniversaryRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private Member testMember;
    private Friend testFriend;

    @BeforeEach
    void setUp() {
        // 테스트용 회원 생성 (알림 동의 + FCM 토큰 있음)
        testMember = Member.builder()
                .username("test@test.com")
                .nickname("테스트유저")
                .password(" ")
                .isActive(true)
                .notificationAgreedAt(LocalDateTime.now())
                .fcmToken("test-fcm-token-123")
                .build();
        testMember.addRole(RoleType.USER);
        testMember = memberRepository.save(testMember);

        // 테스트용 친구 생성
        testFriend = Friend.builder()
                .memberId(testMember.getMemberId())
                .name("친구1")
                .contactFrequency(FriendContactFrequency.builder()
                        .contactWeek(FriendContactWeek.EVERY_WEEK)
                        .build())
                .position(1)
                .nextContactAt(LocalDate.now())
                .checkRate(0)
                .alarmTriggerCount(0)
                .build();
        testFriend = friendRepository.save(testFriend);
    }

    @Test
    @DisplayName("nextContactAt이 오늘인 친구에 대해 알림이 발송되어야 한다")
    void nextContactAt이_오늘인_친구에_대해_알림이_발송되어야_한다() {
        // given
        LocalDate today = LocalDate.now();
        testFriend = Friend.builder()
                .memberId(testMember.getMemberId())
                .name("오늘 연락할 친구")
                .contactFrequency(FriendContactFrequency.builder()
                        .contactWeek(FriendContactWeek.EVERY_WEEK)
                        .build())
                .position(1)
                .nextContactAt(today)
                .checkRate(0)
                .alarmTriggerCount(0)
                .build();
        testFriend = friendRepository.save(testFriend);

        int initialTriggerCount = testFriend.getAlarmTriggerCount();

        // when
        notificationScheduler.sendDailyFriendReminders();

        // then
        Friend updatedFriend = friendRepository.findById(testFriend.getFriendId()).orElseThrow();
        assertThat(updatedFriend.getAlarmTriggerCount()).isEqualTo(initialTriggerCount + 1);
    }

    @Test
    @DisplayName("연락 예정일과 기념일이 겹치는 경우 알림은 한 번만 발송되어야 한다")
    void 연락_예정일과_기념일이_겹치는_경우_알림은_한_번만_발송되어야_한다() {
        // given
        LocalDate today = LocalDate.now();

        Friend friend = Friend.builder()
                .memberId(testMember.getMemberId())
                .name("겹치는 친구")
                .contactFrequency(FriendContactFrequency.builder()
                        .contactWeek(FriendContactWeek.EVERY_WEEK)
                        .build())
                .position(1)
                .nextContactAt(today) // 오늘 연락 예정
                .checkRate(0)
                .alarmTriggerCount(0)
                .build();
        friend = friendRepository.save(friend);

        FriendAnniversary anniversary = FriendAnniversary.builder()
                .friendId(friend.getFriendId())
                .title("생일")
                .date(today) // 오늘이 기념일
                .build();
        friendAnniversaryRepository.save(anniversary);

        int initialTriggerCount = friend.getAlarmTriggerCount();

        // when
        notificationScheduler.sendDailyFriendReminders();

        // then
        Friend updatedFriend = friendRepository.findById(friend.getFriendId()).orElseThrow();
        // 알림은 한 번만 발송되므로 카운트는 1만 증가
        assertThat(updatedFriend.getAlarmTriggerCount()).isEqualTo(initialTriggerCount + 1);
    }

    @Test
    @DisplayName("FCM 토큰이 없는 회원에게는 알림이 발송되지 않아야 한다")
    void FCM_토큰이_없는_회원에게는_알림이_발송되지_않아야_한다() {
        // given
        Member memberWithoutToken = Member.builder()
                .username("notoken@test.com")
                .nickname("토큰없음")
                .password(" ")
                .isActive(true)
                .notificationAgreedAt(LocalDateTime.now())
                .fcmToken(null) // FCM 토큰 없음
                .build();
        memberWithoutToken.addRole(RoleType.USER);
        memberWithoutToken = memberRepository.save(memberWithoutToken);

        Friend friend = Friend.builder()
                .memberId(memberWithoutToken.getMemberId())
                .name("토큰없는 회원의 친구")
                .contactFrequency(FriendContactFrequency.builder()
                        .contactWeek(FriendContactWeek.EVERY_WEEK)
                        .build())
                .position(1)
                .nextContactAt(LocalDate.now())
                .checkRate(0)
                .alarmTriggerCount(0)
                .build();
        friend = friendRepository.save(friend);

        int initialTriggerCount = friend.getAlarmTriggerCount();

        // when
        notificationScheduler.sendDailyFriendReminders();

        // then
        Friend updatedFriend = friendRepository.findById(friend.getFriendId()).orElseThrow();
        // 알림이 발송되지 않으므로 카운트가 증가하지 않음
        assertThat(updatedFriend.getAlarmTriggerCount()).isEqualTo(initialTriggerCount);
    }

    @Test
    @DisplayName("알림 동의하지 않은 회원에게는 알림이 발송되지 않아야 한다")
    void 알림_동의하지_않은_회원에게는_알림이_발송되지_않아야_한다() {
        // given
        Member memberWithoutConsent = Member.builder()
                .username("noconsent@test.com")
                .nickname("동의안함")
                .password(" ")
                .isActive(true)
                .notificationAgreedAt(null) // 알림 동의 안 함
                .fcmToken("test-fcm-token-456")
                .build();
        memberWithoutConsent.addRole(RoleType.USER);
        memberWithoutConsent = memberRepository.save(memberWithoutConsent);

        Friend friend = Friend.builder()
                .memberId(memberWithoutConsent.getMemberId())
                .name("동의안한 회원의 친구")
                .contactFrequency(FriendContactFrequency.builder()
                        .contactWeek(FriendContactWeek.EVERY_WEEK)
                        .build())
                .position(1)
                .nextContactAt(LocalDate.now())
                .checkRate(0)
                .alarmTriggerCount(0)
                .build();
        friend = friendRepository.save(friend);

        int initialTriggerCount = friend.getAlarmTriggerCount();

        // when
        notificationScheduler.sendDailyFriendReminders();

        // then
        Friend updatedFriend = friendRepository.findById(friend.getFriendId()).orElseThrow();
        // 알림이 발송되지 않으므로 카운트가 증가하지 않음
        assertThat(updatedFriend.getAlarmTriggerCount()).isEqualTo(initialTriggerCount);
    }

    @Test
    @DisplayName("알림 발송 시 DB에 알림이 저장되어야 한다")
    void 알림_발송_시_DB에_알림이_저장되어야_한다() {
        // given
        LocalDate today = LocalDate.now();
        final Friend friend = friendRepository.save(Friend.builder()
                .memberId(testMember.getMemberId())
                .name("DB저장테스트친구")
                .contactFrequency(FriendContactFrequency.builder()
                        .contactWeek(FriendContactWeek.EVERY_WEEK)
                        .build())
                .position(2)
                .nextContactAt(today)
                .checkRate(0)
                .alarmTriggerCount(0)
                .build());

        long initialNotificationCount = notificationRepository.findByMemberIdOrderByCreatedAtDesc(
                testMember.getMemberId()).size();

        // when
        notificationScheduler.sendDailyFriendReminders();

        // then
        List<Notification> notifications = notificationRepository.findByMemberIdOrderByCreatedAtDesc(
                testMember.getMemberId());
        assertThat(notifications.size()).isGreaterThan((int) initialNotificationCount);

        // 특정 친구의 알림이 저장되었는지 확인
        Notification savedNotification = notifications.stream()
                .filter(n -> friend.getFriendId().equals(n.getFriendId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("알림이 저장되지 않았습니다."));

        assertThat(savedNotification.getMemberId()).isEqualTo(testMember.getMemberId());
        assertThat(savedNotification.getType()).isEqualTo(NotificationType.FRIEND_REMINDER);
        assertThat(savedNotification.getTitle()).isEqualTo("친구 챙기기");
        assertThat(savedNotification.getBody()).contains("DB저장테스트친구");
        assertThat(savedNotification.getIsRead()).isFalse();
    }

    @Test
    @DisplayName("FCM 토큰이 없어도 알림은 DB에 저장되어야 한다")
    void FCM_토큰이_없어도_알림은_DB에_저장되어야_한다() {
        // given
        Member memberWithoutToken = Member.builder()
                .username("notoken2@test.com")
                .nickname("토큰없음2")
                .password(" ")
                .isActive(true)
                .notificationAgreedAt(LocalDateTime.now())
                .fcmToken(null) // FCM 토큰 없음
                .build();
        memberWithoutToken.addRole(RoleType.USER);
        memberWithoutToken = memberRepository.save(memberWithoutToken);

        Friend friend = Friend.builder()
                .memberId(memberWithoutToken.getMemberId())
                .name("토큰없는회원친구")
                .contactFrequency(FriendContactFrequency.builder()
                        .contactWeek(FriendContactWeek.EVERY_WEEK)
                        .build())
                .position(1)
                .nextContactAt(LocalDate.now())
                .checkRate(0)
                .alarmTriggerCount(0)
                .build();
        friend = friendRepository.save(friend);

        // when
        notificationScheduler.sendDailyFriendReminders();

        // then - FCM 전송은 안되지만 DB에는 저장됨
        List<Notification> notifications = notificationRepository.findByMemberIdOrderByCreatedAtDesc(
                memberWithoutToken.getMemberId());
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getBody()).contains("토큰없는회원친구");
    }

    @Test
    @DisplayName("기념일 알림은 ANNIVERSARY 타입으로 저장되어야 한다")
    void 기념일_알림은_ANNIVERSARY_타입으로_저장되어야_한다() {
        // given
        LocalDate today = LocalDate.now();

        // 연락 예정일이 아닌 친구 (기념일만 해당)
        Friend friend = Friend.builder()
                .memberId(testMember.getMemberId())
                .name("기념일친구")
                .contactFrequency(FriendContactFrequency.builder()
                        .contactWeek(FriendContactWeek.EVERY_WEEK)
                        .build())
                .position(1)
                .nextContactAt(today.plusDays(7)) // 오늘이 아닌 날
                .checkRate(0)
                .alarmTriggerCount(0)
                .build();
        final Friend savedFriend = friendRepository.save(friend);

        FriendAnniversary anniversary = FriendAnniversary.builder()
                .friendId(savedFriend.getFriendId())
                .title("생일")
                .date(today) // 오늘이 기념일
                .build();
        friendAnniversaryRepository.save(anniversary);

        // when
        notificationScheduler.sendDailyFriendReminders();

        // then
        List<Notification> notifications = notificationRepository.findByMemberIdOrderByCreatedAtDesc(
                testMember.getMemberId());

        // 기념일 알림이 저장되었는지 확인
        boolean hasAnniversaryNotification = notifications.stream()
                .anyMatch(n -> n.getType() == NotificationType.ANNIVERSARY
                        && n.getFriendId().equals(savedFriend.getFriendId()));
        assertThat(hasAnniversaryNotification).isTrue();
    }
}
