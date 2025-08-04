package kr.swyp.backend.chatbot.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "GPTHistory")
public class ChatHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "USER_ID")
    @Comment("유저 아이디")
    private Long userId;

    @Column(name = "TARGET")
    @Comment("대상")
    private String target;

    @Column(name = "TOPIC")
    @Comment("주제")
    private String topic;

    @Column(name = "QUESTION")
    @Comment("질문")
    private String question;
}
