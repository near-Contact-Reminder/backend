package kr.swyp.backend.friend.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReminderType {
    CONTACT("연락 주기 알림"),
    ANNIVERSARY("기념일 알림"),
    BIRTHDAY("생일 알림");

    private final String description;
}