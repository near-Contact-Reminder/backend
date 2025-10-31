package kr.swyp.backend.chatbot.client.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class OpenAiDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpenAiChatRequest {

        private String model;
        private List<Message> messages;
        private Double temperature;

        @Getter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Message {

            private String role;
            private String content;
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpenAiChatResponse {

        private String id;
        private List<Choice> choices;

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Choice {

            private Message message;
        }

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Message {

            private String role;
            private String content;
        }
    }

}
