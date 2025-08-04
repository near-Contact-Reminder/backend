package kr.swyp.backend.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ChatRequestDto {
    private String message; // 질문하는 메시지
    private Long userId; // 유저 아이디
}
