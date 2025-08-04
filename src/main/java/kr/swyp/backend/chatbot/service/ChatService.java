package kr.swyp.backend.chatbot.service;

import kr.swyp.backend.chatbot.dto.ChatRequestDto;
import kr.swyp.backend.chatbot.dto.ChatResponseDto;

public interface ChatService {
    ChatResponseDto ask(ChatRequestDto request);
}
