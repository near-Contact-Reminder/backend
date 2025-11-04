package kr.swyp.backend.chatbot.service;

import kr.swyp.backend.chatbot.client.dto.ChatDto.ChatRequest;
import kr.swyp.backend.chatbot.client.dto.ChatDto.ChatResponse;

public interface ChatClientService {

    ChatResponse createChatCompletion(String authorization, ChatRequest request);
}
