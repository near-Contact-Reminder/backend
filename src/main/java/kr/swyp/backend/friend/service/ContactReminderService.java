package kr.swyp.backend.friend.service;

import java.time.LocalDate;
import java.util.UUID;
import kr.swyp.backend.friend.domain.ContactReminder;
import kr.swyp.backend.friend.domain.Friend;
import kr.swyp.backend.friend.domain.FriendAnniversary;
import kr.swyp.backend.friend.domain.FriendContactFrequency;

public interface ContactReminderService {

    /**
     * 친구의 연락 주기에 따른 알림을 생성합니다.
     *
     * @param friend 친구 정보
     * @param contactFrequency 연락 주기
     */
    ContactReminder createContactReminder(Friend friend, FriendContactFrequency contactFrequency);

    /**
     * 기념일에 대한 연간 알림을 생성합니다.
     *
     * @param friend 친구 정보
     * @param anniversary 기념일 정보
     */
    ContactReminder createAnniversaryReminder(Friend friend, FriendAnniversary anniversary);

    /**
     * 생일에 대한 연간 알림을 생성합니다.
     *
     * @param friend 친구 정보
     * @param birthday 생일 날짜
     */
    ContactReminder createBirthdayReminder(Friend friend, LocalDate birthday);

    /**
     * 연락 주기 알림의 다음 알림 날짜를 업데이트합니다.
     *
     * @param friendId 친구 ID
     * @param contactFrequency 연락 주기
     */
    void updateContactReminderDate(UUID friendId, FriendContactFrequency contactFrequency);
}