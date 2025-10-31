package kr.swyp.backend.chatbot.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAiFeignConfig {

    @Value("${openai.api.key}")
    private String apiKey;

    @Bean
    public RequestInterceptor openAIRequestInterceptor() {
        return template -> {
            template.header("Authorization", "Bearer " + apiKey);
            template.header("Content-Type", "application/json");
        };
    }
}
