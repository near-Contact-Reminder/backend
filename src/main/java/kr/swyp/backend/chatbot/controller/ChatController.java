package kr.swyp.backend.chatbot.controller;

import kr.swyp.backend.chatbot.dto.ChatRequestDto;
import kr.swyp.backend.chatbot.dto.ChatResponseDto;
import kr.swyp.backend.chatbot.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;

    @PostMapping
    public ChatResponseDto ask(@RequestBody ChatRequestDto question) {
        return chatService.ask(question);
    }
}
