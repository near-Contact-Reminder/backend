package kr.swyp.backend.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatResponseDto {
    private List<String> contents; // 응답 답변들
    private String sender; // 답변하는 사람
}
