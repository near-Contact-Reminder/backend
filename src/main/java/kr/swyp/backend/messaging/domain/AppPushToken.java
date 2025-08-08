package kr.swyp.backend.messaging.domain;

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
import kr.swyp.backend.messaging.enums.AppPushTokenOsType;
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
@Table(name = "APP_PUSH_TOKEN")
public class AppPushToken extends BaseEntity {

    @Id
    @Comment("앱 토큰 ID")
    @Column(name = "APP_TOKEN_ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long appTokenId;

    @NotNull
    @Comment("회원 고유 식별자")
    @Column(name = "MEMBER_ID")
    private UUID memberId;

    @NotNull
    @Comment("앱 토큰 값")
    @Column(name = "TOKEN")
    private String token;

    @NotNull
    @Comment("앱 토큰 OS 타입")
    @Enumerated(EnumType.STRING)
    @Column(name = "OS_TYPE", columnDefinition = "varchar(255)")
    private AppPushTokenOsType osType;

    public void updateToken(String token, AppPushTokenOsType osType) {
        this.token = token;
        this.osType = osType;
    }
}
