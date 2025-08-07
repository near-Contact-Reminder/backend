package kr.swyp.backend.chatbot.service;

import java.util.List;
import java.util.UUID;
import kr.swyp.backend.chatbot.dto.ChatDto.ChatHistoryDto;
import kr.swyp.backend.chatbot.dto.ChatDto.ChatRequestDto;
import kr.swyp.backend.chatbot.dto.ChatDto.ChatResponseDto;
import kr.swyp.backend.chatbot.dto.ChatDto.ChatSessionDto;
import kr.swyp.backend.chatbot.dto.ChatDto.ConversationRequestDto;
import kr.swyp.backend.chatbot.dto.ChatDto.ConversationResponseDto;

public interface ChatService {

    ChatResponseDto ask(UUID memberId, ChatRequestDto request);
    
    List<ChatHistoryDto> getChatHistory(UUID memberId);
    
    // 새로운 채팅 세션 시작
    ChatSessionDto startNewSession(UUID memberId, String initialMessage);
    
    // 기존 세션에서 대화 계속
    ConversationResponseDto continueConversation(UUID memberId, ConversationRequestDto request);
    
    // 사용자의 채팅 세션 목록 조회
    List<ChatSessionDto> getUserSessions(UUID memberId);
    
    // 특정 세션의 대화 기록 조회
    List<ConversationResponseDto> getSessionHistory(UUID memberId, String sessionId);
}
