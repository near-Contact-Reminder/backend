package kr.swyp.backend.friend.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import kr.swyp.backend.common.domain.BaseEntity;
import kr.swyp.backend.friend.domain.converter.FriendContactFrequencyConverter;
import kr.swyp.backend.friend.enums.FriendContactWeek;
import kr.swyp.backend.friend.enums.FriendSource;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "FRIEND")
public class Friend extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "FRIEND_ID", columnDefinition = "BINARY(16)")
    private UUID friendId;

    @Column(name = "MEMBER_ID")
    @Comment("회원")
    @NotNull
    private UUID memberId;  // MEMBER 테이블 참조

    @Column(name = "NAME")
    @Comment("친구 이름")
    @NotNull
    private String name;

    @Enumerated(EnumType.STRING)
    @Comment("카카오 친구 or 연락처 친구")
    @Column(name = "FRIEND_SOURCE", columnDefinition = "varchar(255)")
    private FriendSource friendSource;

    @NotNull
    @Default
    @Comment("연락 주기")
    @Column(name = "CONTACT_FREQUENCY")
    @Convert(converter = FriendContactFrequencyConverter.class)
    private FriendContactFrequency contactFrequency = FriendContactFrequency.builder().build();

    @NotNull
    @Column(name = "POSITION")
    @Comment("가까운 인연들 노출 순서")
    private Integer position;

    @NotNull
    @Column(name = "NEXT_CONTACT_AT")
    @Comment("다음 챙김 예정일")
    private LocalDate nextContactAt;

    @NotNull
    @Min(0)
    @Default
    @Column(name = "CHECK_RATE")
    @Comment("친구별 챙김 체크율 (0~100%)")
    private Integer checkRate = 0;

    @NotNull
    @Min(0)
    @Comment("트리거된 알람 개수")
    @Default
    @Column(name = "ALARM_TRIGGER_COUNT")
    private Integer alarmTriggerCount = 0;

    @OneToOne(mappedBy = "friend", cascade = CascadeType.ALL, orphanRemoval = true)
    private FriendDetail friendDetail;

    @Default
    @OneToMany(mappedBy = "friend", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FriendCheckingLog> checkSchedules = new ArrayList<>();

    public void addFriendDetail(FriendDetail friendDetail) {
        this.friendDetail = friendDetail;
    }

    public void updateCheckRate(int checkRate) {
        this.checkRate = checkRate;
    }

    public void updatePosition(int position) {
        this.position = position;
    }

    public void updateAlarmTriggerCount() {
        this.alarmTriggerCount += 1;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updateFriendContactFrequency(FriendContactFrequency friendContactFrequency) {
        this.contactFrequency = friendContactFrequency;
    }

    public void updateNextContactAt() {
        LocalDate now = LocalDate.now();
        DayOfWeek targetDayOfWeek = contactFrequency.getDayOfWeek();

        LocalDate calculatedDate = switch (contactFrequency.getContactWeek()) {
            case EVERY_DAY -> now.plusDays(1);
            case EVERY_WEEK -> now.plusWeeks(1);
            case EVERY_TWO_WEEK -> now.plusWeeks(2);
            case EVERY_MONTH -> now.plusMonths(1);
            case EVERY_SIX_MONTH -> now.plusMonths(6);
            default ->
                    throw new IllegalArgumentException("연락 챙김 주기가 올바르지 않습니다," + contactFrequency);
        };

        // 매일 주기가 아닌 경우 특정 요일로 조정
        if (contactFrequency.getContactWeek() != FriendContactWeek.EVERY_DAY
                && targetDayOfWeek != null) {
            // 계산된 날짜의 요일
            DayOfWeek calculatedDayOfWeek = calculatedDate.getDayOfWeek();

            // 목표 요일까지 날짜 조정 (요일 간의 차이를 계산)
            int daysToAdd = targetDayOfWeek.getValue() - calculatedDayOfWeek.getValue();

            // 음수인 경우 다음 주로 이동
            if (daysToAdd < 0) {
                daysToAdd += 7;
            }

            // 요일 조정
            calculatedDate = calculatedDate.plusDays(daysToAdd);
        }
        this.nextContactAt = calculatedDate;
    }
}

