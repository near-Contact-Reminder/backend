package kr.swyp.backend.notification.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
}
