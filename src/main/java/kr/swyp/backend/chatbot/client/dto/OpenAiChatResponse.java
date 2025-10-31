package kr.swyp.backend.chatbot.client.dto;

import java.util.List;
import lombok.Getter;

@Getter
public class OpenAiChatResponse {
    private String id;
    private List<Choice> choices;

    @Getter
    public static class Choice {
        private Message message;
    }

    @Getter
    public static class Message {
        private String role;
        private String content;
    }
}
