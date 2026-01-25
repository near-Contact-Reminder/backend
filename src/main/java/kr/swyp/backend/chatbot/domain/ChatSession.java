package kr.swyp.backend.chatbot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import kr.swyp.backend.common.domain.BaseEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "CHAT_SESSION")
public class ChatSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "SESSION_ID", unique = true)
    @Comment("세션 고유 식별자")
    private String sessionId;

    @Column(name = "MEMBER_ID")
    @Comment("사용자 아이디")
    private UUID memberId;

    @Column(name = "TITLE")
    @Comment("채팅 세션 제목")
    private String title;

    @Column(name = "IS_ACTIVE")
    @Comment("활성 상태")
    @Builder.Default
    private Boolean isActive = true;
}