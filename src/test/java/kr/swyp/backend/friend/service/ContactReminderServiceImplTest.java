package kr.swyp.backend.friend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import kr.swyp.backend.friend.domain.ContactReminder;
import kr.swyp.backend.friend.domain.Friend;
import kr.swyp.backend.friend.domain.FriendAnniversary;
import kr.swyp.backend.friend.domain.FriendContactFrequency;
import kr.swyp.backend.friend.enums.FriendContactWeek;
import kr.swyp.backend.friend.enums.FriendSource;
import kr.swyp.backend.friend.enums.ReminderType;
import kr.swyp.backend.friend.repository.ContactReminderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContactReminderServiceImplTest {

    @Mock
    private ContactReminderRepository contactReminderRepository;

    @InjectMocks
    private ContactReminderServiceImpl contactReminderService;

    private Friend friend;
    private UUID memberId;

    @BeforeEach
    void setUp() {
        memberId = UUID.randomUUID();
        friend = Friend.builder()
                .friendId(UUID.randomUUID())
                .memberId(memberId)
                .name("테스트친구")
                .friendSource(FriendSource.KAKAO)
                .contactFrequency(FriendContactFrequency.builder()
                        .contactWeek(FriendContactWeek.EVERY_WEEK)
                        .dayOfWeek(DayOfWeek.MONDAY)
                        .build())
                .nextContactAt(LocalDate.now().plusDays(7))
                .build();
    }

    @Test
    @DisplayName("매일 연락 주기로 ContactReminder를 생성할 수 있다")
    void 매일_연락_주기로_ContactReminder를_생성할_수_있다() {
        // given
        FriendContactFrequency dailyFrequency = FriendContactFrequency.builder()
                .contactWeek(FriendContactWeek.EVERY_DAY)
                .build();
        
        ContactReminder savedReminder = ContactReminder.builder()
                .id(1L)
                .memberId(memberId)
                .friend(friend)
                .reminderType(ReminderType.CONTACT)
                .reminderDate(friend.getNextContactAt())
                .frequencyDays(1)
                .isActive(true)
                .build();

        when(contactReminderRepository.save(any(ContactReminder.class))).thenReturn(savedReminder);

        // when
        ContactReminder result = contactReminderService.createContactReminder(friend, dailyFrequency);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getFrequencyDays()).isEqualTo(1);
        assertThat(result.getReminderType()).isEqualTo(ReminderType.CONTACT);
        assertThat(result.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("매주 연락 주기로 ContactReminder를 생성할 수 있다")
    void 매주_연락_주기로_ContactReminder를_생성할_수_있다() {
        // given
        FriendContactFrequency weeklyFrequency = FriendContactFrequency.builder()
                .contactWeek(FriendContactWeek.EVERY_WEEK)
                .dayOfWeek(DayOfWeek.MONDAY)
                .build();

        ArgumentCaptor<ContactReminder> captor = ArgumentCaptor.forClass(ContactReminder.class);
        when(contactReminderRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        ContactReminder result = contactReminderService.createContactReminder(friend, weeklyFrequency);

        // then
        ContactReminder captured = captor.getValue();
        assertThat(captured.getFrequencyDays()).isEqualTo(7);
        assertThat(captured.getReminderType()).isEqualTo(ReminderType.CONTACT);
    }

    @Test
    @DisplayName("2주 연락 주기로 ContactReminder를 생성할 수 있다")
    void 이주_연락_주기로_ContactReminder를_생성할_수_있다() {
        // given
        FriendContactFrequency biweeklyFrequency = FriendContactFrequency.builder()
                .contactWeek(FriendContactWeek.EVERY_TWO_WEEK)
                .dayOfWeek(DayOfWeek.FRIDAY)
                .build();

        ArgumentCaptor<ContactReminder> captor = ArgumentCaptor.forClass(ContactReminder.class);
        when(contactReminderRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        contactReminderService.createContactReminder(friend, biweeklyFrequency);

        // then
        ContactReminder captured = captor.getValue();
        assertThat(captured.getFrequencyDays()).isEqualTo(14);
    }

    @Test
    @DisplayName("매월 연락 주기로 ContactReminder를 생성할 수 있다")
    void 매월_연락_주기로_ContactReminder를_생성할_수_있다() {
        // given
        FriendContactFrequency monthlyFrequency = FriendContactFrequency.builder()
                .contactWeek(FriendContactWeek.EVERY_MONTH)
                .build();

        ArgumentCaptor<ContactReminder> captor = ArgumentCaptor.forClass(ContactReminder.class);
        when(contactReminderRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        contactReminderService.createContactReminder(friend, monthlyFrequency);

        // then
        ContactReminder captured = captor.getValue();
        assertThat(captured.getFrequencyDays()).isEqualTo(30);
    }

    @Test
    @DisplayName("6개월 연락 주기로 ContactReminder를 생성할 수 있다")
    void 육개월_연락_주기로_ContactReminder를_생성할_수_있다() {
        // given
        FriendContactFrequency sixMonthFrequency = FriendContactFrequency.builder()
                .contactWeek(FriendContactWeek.EVERY_SIX_MONTH)
                .build();

        ArgumentCaptor<ContactReminder> captor = ArgumentCaptor.forClass(ContactReminder.class);
        when(contactReminderRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        contactReminderService.createContactReminder(friend, sixMonthFrequency);

        // then
        ContactReminder captured = captor.getValue();
        assertThat(captured.getFrequencyDays()).isEqualTo(180);
    }

    @Test
    @DisplayName("기념일 알림을 생성할 수 있다 - 올해 기념일이 이미 지난 경우")
    void 기념일_알림을_생성할_수_있다_올해_이미_지난_경우() {
        // given
        LocalDate pastDate = LocalDate.now().minusMonths(6);
        FriendAnniversary anniversary = FriendAnniversary.builder()
                .id(1L)
                .friendId(friend.getFriendId())
                .title("결혼기념일")
                .date(pastDate)
                .build();

        ArgumentCaptor<ContactReminder> captor = ArgumentCaptor.forClass(ContactReminder.class);
        when(contactReminderRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        contactReminderService.createAnniversaryReminder(friend, anniversary);

        // then
        ContactReminder captured = captor.getValue();
        assertThat(captured.getReminderType()).isEqualTo(ReminderType.ANNIVERSARY);
        assertThat(captured.getFrequencyDays()).isEqualTo(365);
        assertThat(captured.getAnniversaryId()).isEqualTo(anniversary.getId());
        // 올해 기념일이 지났으므로 내년으로 설정되어야 함
        assertThat(captured.getReminderDate()).isEqualTo(pastDate.withYear(LocalDate.now().getYear() + 1));
    }

    @Test
    @DisplayName("기념일 알림을 생성할 수 있다 - 올해 기념일이 아직 안 지난 경우")
    void 기념일_알림을_생성할_수_있다_올해_아직_안_지난_경우() {
        // given
        LocalDate futureDate = LocalDate.now().plusMonths(3);
        FriendAnniversary anniversary = FriendAnniversary.builder()
                .id(2L)
                .friendId(friend.getFriendId())
                .title("첫만남 기념일")
                .date(futureDate)
                .build();

        ArgumentCaptor<ContactReminder> captor = ArgumentCaptor.forClass(ContactReminder.class);
        when(contactReminderRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        contactReminderService.createAnniversaryReminder(friend, anniversary);

        // then
        ContactReminder captured = captor.getValue();
        assertThat(captured.getReminderType()).isEqualTo(ReminderType.ANNIVERSARY);
        assertThat(captured.getFrequencyDays()).isEqualTo(365);
        // 올해 기념일이 아직 안 지났으므로 올해로 설정되어야 함
        assertThat(captured.getReminderDate()).isEqualTo(futureDate.withYear(LocalDate.now().getYear()));
    }

    @Test
    @DisplayName("생일 알림을 생성할 수 있다 - 올해 생일이 이미 지난 경우")
    void 생일_알림을_생성할_수_있다_올해_이미_지난_경우() {
        // given
        LocalDate pastBirthday = LocalDate.now().minusMonths(2);

        ArgumentCaptor<ContactReminder> captor = ArgumentCaptor.forClass(ContactReminder.class);
        when(contactReminderRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        contactReminderService.createBirthdayReminder(friend, pastBirthday);

        // then
        ContactReminder captured = captor.getValue();
        assertThat(captured.getReminderType()).isEqualTo(ReminderType.BIRTHDAY);
        assertThat(captured.getFrequencyDays()).isEqualTo(365);
        // 올해 생일이 지났으므로 내년으로 설정되어야 함
        assertThat(captured.getReminderDate()).isEqualTo(pastBirthday.withYear(LocalDate.now().getYear() + 1));
    }

    @Test
    @DisplayName("연락 주기 알림 날짜를 업데이트할 수 있다")
    void 연락_주기_알림_날짜를_업데이트할_수_있다() {
        // given
        UUID friendId = friend.getFriendId();
        FriendContactFrequency newFrequency = FriendContactFrequency.builder()
                .contactWeek(FriendContactWeek.EVERY_MONTH)
                .build();

        ContactReminder existingReminder = ContactReminder.builder()
                .id(1L)
                .friend(friend)
                .reminderType(ReminderType.CONTACT)
                .reminderDate(LocalDate.now().plusDays(7))
                .frequencyDays(7)
                .isActive(true)
                .build();

        when(contactReminderRepository.findByFriendFriendIdAndReminderTypeAndIsActiveTrue(
                friendId, ReminderType.CONTACT))
                .thenReturn(Optional.of(existingReminder));
        when(contactReminderRepository.save(any(ContactReminder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        contactReminderService.updateContactReminderDate(friendId, newFrequency);

        // then
        verify(contactReminderRepository, times(1)).save(existingReminder);
        assertThat(existingReminder.getReminderDate()).isEqualTo(LocalDate.now().plusMonths(1));
    }
}