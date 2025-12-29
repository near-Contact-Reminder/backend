package kr.swyp.backend.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import kr.swyp.backend.common.domain.BaseEntity;
import kr.swyp.backend.notification.enums.NotificationType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "NOTIFICATION")
public class Notification extends BaseEntity {

    @Id
    @Comment("알림 ID")
    @Column(name = "NOTIFICATION_ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    @NotNull
    @Comment("회원 고유 식별자")
    @Column(name = "MEMBER_ID")
    private UUID memberId;

    @Comment("친구 ID (선택)")
    @Column(name = "FRIEND_ID")
    private UUID friendId;

    @NotNull
    @Comment("알림 타입")
    @Column(name = "TYPE")
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @NotNull
    @Comment("알림 제목")
    @Column(name = "TITLE")
    private String title;

    @NotNull
    @Comment("알림 내용")
    @Column(name = "BODY", length = 500)
    private String body;

    @Builder.Default
    @Comment("읽음 여부")
    @Column(name = "IS_READ")
    private Boolean isRead = false;

    public void markAsRead() {
        this.isRead = true;
    }
}
