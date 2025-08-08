package kr.swyp.backend.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import kr.swyp.backend.member.enums.RoleType;
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
@AllArgsConstructor
@Table(name = "MEMBER_ROLE")
public class Role {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MEMBER_ID")
    private Member member;

    @Id
    @NotNull
    @Comment("권한 유형")
    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE_TYPE", columnDefinition = "varchar(255)")
    private RoleType roleType;

    /**
     * Spring Security에서 사용할 권한 문자열 반환.
     *
     * @return 권한 문자열
     */
    public String getAuthority() {
        return this.roleType.getAuthority();
    }
}