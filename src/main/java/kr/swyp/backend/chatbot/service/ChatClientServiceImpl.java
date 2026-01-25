package kr.swyp.backend.chatbot.service;

import kr.swyp.backend.chatbot.client.ChatClient;
import kr.swyp.backend.chatbot.client.dto.ChatDto.ChatRequest;
import kr.swyp.backend.chatbot.client.dto.ChatDto.ChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatClientServiceImpl implements ChatClientService {

    private final ChatClient chatClient;

    @Override
    public ChatResponse createChatCompletion(String authorization,
            ChatRequest request) {
        return chatClient.createChatCompletion(authorization, request);
    }
}
