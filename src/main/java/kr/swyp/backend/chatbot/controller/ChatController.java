package kr.swyp.backend.chatbot.controller;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import kr.swyp.backend.chatbot.dto.ChatDto.ChatHistoryDto;
import kr.swyp.backend.chatbot.dto.ChatDto.ChatRequestDto;
import kr.swyp.backend.chatbot.dto.ChatDto.ChatResponseDto;
import kr.swyp.backend.chatbot.dto.ChatDto.ChatSessionDto;
import kr.swyp.backend.chatbot.dto.ChatDto.ConversationRequestDto;
import kr.swyp.backend.chatbot.dto.ChatDto.ConversationResponseDto;
import kr.swyp.backend.chatbot.service.ChatService;
import kr.swyp.backend.member.dto.MemberDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/ask")
    public ChatResponseDto ask(@RequestBody ChatRequestDto question,
            @AuthenticationPrincipal MemberDetails memberDetails) {
        return chatService.ask(memberDetails.getMemberId(), question);
    }

    @GetMapping("/history")
    public List<ChatHistoryDto> getChatHistory(
            @AuthenticationPrincipal MemberDetails memberDetails) {
        return chatService.getChatHistory(memberDetails.getMemberId());
    }

    // 새로운 채팅 세션 시작
    @PostMapping("/sessions/start")
    public ChatSessionDto startNewSession(
            @RequestParam @NotBlank String initialMessage,
            @AuthenticationPrincipal MemberDetails memberDetails) {
        return chatService.startNewSession(memberDetails.getMemberId(), initialMessage);
    }

    // 기존 세션에서 대화 계속
    @PostMapping("/sessions/continue")
    public ConversationResponseDto continueConversation(
            @RequestBody ConversationRequestDto request,
            @AuthenticationPrincipal MemberDetails memberDetails) {
        return chatService.continueConversation(memberDetails.getMemberId(), request);
    }

    // 사용자의 채팅 세션 목록 조회
    @GetMapping("/sessions")
    public List<ChatSessionDto> getUserSessions(
            @AuthenticationPrincipal MemberDetails memberDetails) {
        return chatService.getUserSessions(memberDetails.getMemberId());
    }

    // 특정 세션의 대화 기록 조회
    @GetMapping("/sessions/{sessionId}/history")
    public List<ConversationResponseDto> getSessionHistory(
            @PathVariable String sessionId,
            @AuthenticationPrincipal MemberDetails memberDetails) {
        return chatService.getSessionHistory(memberDetails.getMemberId(), sessionId);
    }
}
