package kr.swyp.backend.chatbot.client;

import kr.swyp.backend.chatbot.client.dto.ChatDto.ChatRequest;
import kr.swyp.backend.chatbot.client.dto.ChatDto.ChatResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "openai-client",
        url = "https://api.openai.com"
)
public interface ChatClient {

    @PostMapping(value = "/v1/chat/completions",
            consumes = MediaType.APPLICATION_JSON_VALUE,   // Content-Type 고정
            produces = MediaType.APPLICATION_JSON_VALUE)
    ChatResponse createChatCompletion(
            @RequestHeader("Authorization") String authorization,
            @RequestBody ChatRequest request
    );

}
