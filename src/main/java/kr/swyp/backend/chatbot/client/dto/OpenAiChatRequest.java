package kr.swyp.backend.chatbot.client.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OpenAiChatRequest {
    private String model;
    private List<Message> messages;
    private Double temperature;

    @Getter
    @Builder
    public static class Message {
        private String role;
        private String content;
    }
}
