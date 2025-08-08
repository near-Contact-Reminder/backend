package kr.swyp.backend.friend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import kr.swyp.backend.common.domain.BaseEntity;
import kr.swyp.backend.friend.enums.FriendRelation;
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
@Table(name = "FRIEND_DETAIL")
public class FriendDetail extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FRIEND_ID")
    @Comment("FRIEND 테이블 외래키")
    private Friend friend;

    @Column(name = "PHONE")
    @Comment("전화번호")
    private String phone;

    @NotNull
    @Comment("관계 (예: 친구, 가족 등)")
    @Enumerated(EnumType.STRING)
    @Column(name = "RELATION", columnDefinition = "varchar(255)")
    private FriendRelation relation;

    @Column(name = "BIRTHDAY")
    @Comment("생일")
    private LocalDate birthday;

    @Column(name = "MEMO")
    @Comment("친구에 대한 메모")
    private String memo;

    @Column(name = "IMAGE_FILE_ID")
    @Comment("친구 이미지 파일 ID")
    private Long imageFileId;

    public void updateRelation(FriendRelation relation) {
        this.relation = relation;
    }

    public void updateBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public void updateMemo(String memo) {
        this.memo = memo;
    }

    public void updatePhone(String phone) {
        this.phone = phone;
    }
}