package kr.swyp.backend.chatbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.swyp.backend.chatbot.domain.ChatHistory;
import kr.swyp.backend.chatbot.dto.ChatExtractionResultDto;
import kr.swyp.backend.chatbot.dto.ChatRequestDto;
import kr.swyp.backend.chatbot.dto.ChatResponseDto;
import kr.swyp.backend.chatbot.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService{
    private final ChatClient chatClient;
    private final ChatHistoryRepository chatHistoryRepository;
    private final ObjectMapper objectMapper;

    @Override
    public ChatResponseDto ask(ChatRequestDto request) {
        String message = request.getMessage();

        String systemPrompt = """
        You are an AI assistant.
        Given the user input, extract the 'target' (who the message is for) and the 'topic'.
        Then, suggest 3~5 possible message suggestions the user might want to send, depending on the target and topic.
        
        User input: "%s"
        
        Please respond in JSON format like this:
        {
          "target": "<extracted target>",
          "topic": "<extracted topic>",
          "answers": [
            "<natural message 1>",
            "<natural message 2>",
            ...
          ]
        }
        """.formatted(message);

        String responseJson = chatClient.prompt()
                .system(systemPrompt)
                .user(message)
                .call()
                .content();

        ChatExtractionResultDto result;
        try {
            result = objectMapper.readValue(responseJson, ChatExtractionResultDto.class);
        } catch (Exception e) {
            return ChatResponseDto.builder()
                    .contents(List.of("죄송해요, 요청을 이해하지 못했어요!"))
                    .sender("Near")
                    .build();
        }

        ChatHistory history = ChatHistory.builder()
                .userId(request.getUserId())
                .target(result.getTarget())
                .question(message)
                .topic(result.getTopic())
                .build();

        chatHistoryRepository.save(history);

        return ChatResponseDto.builder()
                .contents(result.getAnswers())
                .sender("Near")
                .build();
    }
    /**
     * 너는 귀엽고 친절한 AI 마스코트 '니어(Near)'야. \
     * 누군가 "너는 누구야?"라고 물으면 "저는 AI 마스코트 니어예요!"처럼 \
     * 명확하면서도 자연스럽게 자신을 소개해야 해. \
     * 항상 밝고 따뜻한 말투를 사용하고, 설명보다는 대화를 이어가도록 해. \
     * 답변은 5줄 이하로 간결하게 해.
     */
}
