package kr.swyp.backend.friend.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import kr.swyp.backend.friend.domain.ContactReminder;
import kr.swyp.backend.friend.domain.Friend;
import kr.swyp.backend.friend.domain.FriendAnniversary;
import kr.swyp.backend.friend.domain.FriendContactFrequency;
import kr.swyp.backend.friend.enums.FriendContactWeek;
import kr.swyp.backend.friend.enums.ReminderType;
import kr.swyp.backend.friend.repository.ContactReminderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContactReminderServiceImpl implements ContactReminderService {

    private final ContactReminderRepository contactReminderRepository;

    @Override
    @Transactional
    public ContactReminder createContactReminder(Friend friend,
            FriendContactFrequency contactFrequency) {
        Integer frequencyDays = calculateFrequencyDays(contactFrequency);
        LocalDate nextReminderDate = friend.getNextContactAt();

        ContactReminder reminder = ContactReminder.builder()
                .memberId(friend.getMemberId())
                .friend(friend)
                .reminderType(ReminderType.CONTACT)
                .reminderDate(nextReminderDate)
                .frequencyDays(frequencyDays)
                .isActive(true)
                .build();

        return contactReminderRepository.save(reminder);
    }

    @Override
    @Transactional
    public ContactReminder createAnniversaryReminder(Friend friend, FriendAnniversary anniversary) {
        // 기념일은 매년 반복되므로 365일 주기
        LocalDate nextReminderDate = calculateNextAnniversaryDate(anniversary.getDate());

        ContactReminder reminder = ContactReminder.builder()
                .memberId(friend.getMemberId())
                .friend(friend)
                .reminderType(ReminderType.ANNIVERSARY)
                .reminderDate(nextReminderDate)
                .frequencyDays(365)
                .anniversaryId(anniversary.getId())
                .isActive(true)
                .build();

        return contactReminderRepository.save(reminder);
    }

    @Override
    @Transactional
    public ContactReminder createBirthdayReminder(Friend friend, LocalDate birthday) {
        // 생일은 매년 반복되므로 365일 주기
        LocalDate nextReminderDate = calculateNextBirthdayDate(birthday);

        ContactReminder reminder = ContactReminder.builder()
                .memberId(friend.getMemberId())
                .friend(friend)
                .reminderType(ReminderType.BIRTHDAY)
                .reminderDate(nextReminderDate)
                .frequencyDays(365)
                .isActive(true)
                .build();

        return contactReminderRepository.save(reminder);
    }

    @Override
    @Transactional
    public void updateContactReminderDate(UUID friendId, FriendContactFrequency contactFrequency) {
        contactReminderRepository.findByFriendFriendIdAndReminderTypeAndIsActiveTrue(
                friendId, ReminderType.CONTACT)
                .ifPresent(reminder -> {
                    LocalDate nextDate = calculateNextContactDate(contactFrequency);
                    reminder.updateReminderDate(nextDate);
                    contactReminderRepository.save(reminder);
                });
    }

    private Integer calculateFrequencyDays(FriendContactFrequency contactFrequency) {
        return switch (contactFrequency.getContactWeek()) {
            case EVERY_DAY -> 1;
            case EVERY_WEEK -> 7;
            case EVERY_TWO_WEEK -> 14;
            case EVERY_MONTH -> 30;
            case EVERY_SIX_MONTH -> 180;
        };
    }

    private LocalDate calculateNextContactDate(FriendContactFrequency contactFrequency) {
        LocalDate now = LocalDate.now();
        return switch (contactFrequency.getContactWeek()) {
            case EVERY_DAY -> now.plusDays(1);
            case EVERY_WEEK -> now.plusWeeks(1);
            case EVERY_TWO_WEEK -> now.plusWeeks(2);
            case EVERY_MONTH -> now.plusMonths(1);
            case EVERY_SIX_MONTH -> now.plusMonths(6);
        };
    }

    private LocalDate calculateNextAnniversaryDate(LocalDate anniversaryDate) {
        LocalDate now = LocalDate.now();
        LocalDate thisYearAnniversary = anniversaryDate.withYear(now.getYear());
        
        // 올해 기념일이 이미 지났으면 내년으로 설정
        if (thisYearAnniversary.isBefore(now) || thisYearAnniversary.isEqual(now)) {
            return thisYearAnniversary.plusYears(1);
        }
        return thisYearAnniversary;
    }

    private LocalDate calculateNextBirthdayDate(LocalDate birthday) {
        LocalDate now = LocalDate.now();
        LocalDate thisYearBirthday = birthday.withYear(now.getYear());
        
        // 올해 생일이 이미 지났으면 내년으로 설정
        if (thisYearBirthday.isBefore(now) || thisYearBirthday.isEqual(now)) {
            return thisYearBirthday.plusYears(1);
        }
        return thisYearBirthday;
    }
}