package kr.swyp.backend.config;

import kr.swyp.backend.chatbot.service.ChatPromptService;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public ChatClient mockChatClient() {
        return Mockito.mock(ChatClient.class);
    }

    @Bean
    @Primary
    public ChatPromptService mockChatPromptService() {
        return Mockito.mock(ChatPromptService.class);
    }
}