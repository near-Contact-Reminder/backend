package kr.swyp.backend.chatbot.client;

import kr.swyp.backend.chatbot.client.dto.OpenAiDto.OpenAiChatRequest;
import kr.swyp.backend.chatbot.client.dto.OpenAiDto.OpenAiChatResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "openai-client",
        url = "https://api.openai.com"
)
public interface OpenAiClient {

    @PostMapping(value = "/v1/chat/completions",
            consumes = MediaType.APPLICATION_JSON_VALUE,   // Content-Type 고정
            produces = MediaType.APPLICATION_JSON_VALUE)
    OpenAiChatResponse createChatCompletion(
            @RequestHeader("Authorization") String authorization,
            @RequestBody OpenAiChatRequest request
    );

}
