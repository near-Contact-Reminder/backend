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
public class ChatExtractionResultDto {
    private String target; // 대화를 걸 상대
    private String topic; // 대화 주제
    private List<String> answers; // 대화 응답들
}
