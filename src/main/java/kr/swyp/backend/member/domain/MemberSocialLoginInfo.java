package kr.swyp.backend.member.domain;

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
import kr.swyp.backend.common.domain.BaseEntity;
import kr.swyp.backend.member.enums.SocialLoginProviderType;
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
@Table(name = "MEMBER_SOCIAL_LOGIN_INFO")
public class MemberSocialLoginInfo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MEMBER_ID")
    private Member member;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Comment("소셜 로그인 서비스 제공자")
    @Column(name = "PROVIDER_TYPE", columnDefinition = "varchar(255)")
    private SocialLoginProviderType providerType;

    @NotNull
    @Column(name = "PROVIDER_ID")
    @Comment("소셜 로그인 서비스 제공자 ID")
    private String providerId;
}
