package kr.swyp.backend.chatbot.client;

import kr.swyp.backend.chatbot.client.dto.OpenAiDto.OpenAiChatRequest;
import kr.swyp.backend.chatbot.client.dto.OpenAiDto.OpenAiChatResponse;
import kr.swyp.backend.chatbot.config.OpenAiFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "openai-client",
        url = "https://api.openai.com",  // application.yml에 설정
        configuration = OpenAiFeignConfig.class
)
public interface OpenAiClient {

    @PostMapping("/v1/chat/completions")
    OpenAiChatResponse createChatCompletion(
            @RequestBody OpenAiChatRequest request
    );

}
