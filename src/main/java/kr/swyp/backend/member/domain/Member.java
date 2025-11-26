package kr.swyp.backend.member.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import kr.swyp.backend.common.domain.BaseEntity;
import kr.swyp.backend.member.enums.RoleType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "MEMBER")
public class Member extends BaseEntity implements UserDetails {

    @Id
    @UuidGenerator
    @Column(name = "MEMBER_ID", columnDefinition = "BINARY(16)")
    @Comment("회원 고유 식별자")
    private UUID memberId;

    @NotNull
    @Comment("사용자 로그인 이메일")
    @Column(name = "USERNAME", unique = true)
    private String username;

    @Comment("사용자 이름")
    @Column(name = "NICKNAME")
    private String nickname;

    @NotNull
    @Comment("사용자 로그인 PW")
    @Column(name = "PASSWORD")
    private String password;

    @Comment("프로필 이미지 URL")
    @Column(name = "IMAGE_URL")
    private String imageUrl;

    @NotNull
    @Comment("활성화 여부")
    @Column(name = "IS_ACTIVE")
    private Boolean isActive;

    @Comment("알림 수신 동의 시각")
    @Column(name = "NOTIFICATION_AGREED_AT")
    private LocalDateTime notificationAgreedAt;

    @Comment("탈퇴 시각")
    @Column(name = "WITHDRAWN_AT")
    private LocalDateTime withdrawnAt;

    @Comment("FCM 토큰")
    @Column(name = "FCM_TOKEN")
    private String fcmToken;

    @Default
    @OneToMany(mappedBy = "member", fetch = FetchType.EAGER,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Role> roles = new ArrayList<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getAuthority()))
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.isActive && this.withdrawnAt == null;
    }

    public void addRole(RoleType roleType) {
        Role role = Role.builder()
                .member(this)
                .roleType(roleType)
                .build();
        this.roles.add(role);
    }

    public void updateWithdrawnAt() {
        this.withdrawnAt = LocalDateTime.now();
    }

    public void updateNotificationAgreedAt(LocalDateTime notificationAgreedAt) {
        this.notificationAgreedAt = notificationAgreedAt;
    }

    public void reactivate() {
        this.withdrawnAt = null;
        this.isActive = true;
    }

    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}