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
@Table(name = "CHAT_HISTORY")
public class ChatHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "MEMBER_ID")
    @Comment("유저 아이디")
    private UUID memberId;

    @Column(name = "TARGET")
    @Comment("대상")
    private String target;

    @Column(name = "TOPIC")
    @Comment("주제")
    private String topic;

    @Column(name = "QUESTION", columnDefinition = "TEXT")
    @Comment("질문")
    private String question;
    
    @Column(name = "RESPONSE", columnDefinition = "TEXT")
    @Comment("AI 응답")
    private String response;

    @Column(name = "SESSION_ID")
    @Comment("채팅 세션 ID")
    private String sessionId;

    @Column(name = "MESSAGE_TYPE")
    @Comment("메시지 타입 (USER/BOT)")
    private String messageType;
}
