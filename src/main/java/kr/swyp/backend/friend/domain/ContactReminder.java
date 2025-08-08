package kr.swyp.backend.friend.domain;

import static lombok.Builder.Default;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import kr.swyp.backend.common.domain.BaseEntity;
import kr.swyp.backend.friend.enums.ReminderType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "CONTACT_REMINDER")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContactReminder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "MEMBER_ID")
    private UUID memberId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FRIEND_ID")
    private Friend friend;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "REMINDER_TYPE", columnDefinition = "varchar(255)")
    private ReminderType reminderType;

    @NotNull
    @Column(name = "REMINDER_DATE")
    private LocalDate reminderDate;

    @Column(name = "FREQUENCY_DAYS")
    private Integer frequencyDays;

    @NotNull
    @Default
    @Column(name = "IS_ACTIVE")
    private Boolean isActive = true;

    @Column(name = "LAST_SENT_AT")
    private LocalDateTime lastSentAt;

    @Column(name = "ANNIVERSARY_ID")
    private Long anniversaryId;

    public void updateReminderDate(LocalDate newReminderDate) {
        this.reminderDate = newReminderDate;
    }

    public void markAsSent() {
        this.lastSentAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }
}