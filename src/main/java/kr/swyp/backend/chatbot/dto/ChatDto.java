package kr.swyp.backend.chatbot.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ChatDto {

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ChatExtractionResultDto {

        private String target; // 대화를 걸 상대
        private String topic; // 대화 주제
        private List<String> answers; // 대화 응답들
    }

    @Builder
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChatRequestDto {
        private String SessionId;
        private String message; // 질문하는 메시지
    }

    @Getter
    @Builder
    public static class ChatResponseDto {
        private List<String> contents; // 응답 답변들
        private String sender; // 답변하는 사람
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChatHistoryDto {

        private Long id;
        private String target;
        private String topic;
        private String question;
        private String response;
        private LocalDateTime createdAt;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChatSessionDto {

        private String sessionId;
        private String title;
        private LocalDateTime createdAt;
        private Boolean isActive;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ConversationRequestDto {

        private String sessionId;
        private String message;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ConversationResponseDto {

        private String sessionId;
        private String message;
        private String sender;
        private LocalDateTime timestamp;
    }
}
