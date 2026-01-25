package kr.swyp.backend.chatbot.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ChatDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatRequest {

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
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Builder
    public static class ChatResponse {

        private String id;
        private List<Choice> choices;

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Choice {

            private Message message;

            @Getter
            @NoArgsConstructor
            @AllArgsConstructor
            @Builder
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Message {

                private String role;
                private String content;
            }
        }
    }

}
