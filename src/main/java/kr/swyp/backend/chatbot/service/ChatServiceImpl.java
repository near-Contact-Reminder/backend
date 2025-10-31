package kr.swyp.backend.chatbot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import kr.swyp.backend.chatbot.client.OpenAiClient;
import kr.swyp.backend.chatbot.client.dto.OpenAiDto.OpenAiChatRequest;
import kr.swyp.backend.chatbot.client.dto.OpenAiDto.OpenAiChatResponse;
import kr.swyp.backend.chatbot.domain.ChatHistory;
import kr.swyp.backend.chatbot.domain.ChatSession;
import kr.swyp.backend.chatbot.dto.ChatDto.ChatExtractionResultDto;
import kr.swyp.backend.chatbot.dto.ChatDto.ChatHistoryDto;
import kr.swyp.backend.chatbot.dto.ChatDto.ChatRequestDto;
import kr.swyp.backend.chatbot.dto.ChatDto.ChatResponseDto;
import kr.swyp.backend.chatbot.dto.ChatDto.ChatSessionDto;
import kr.swyp.backend.chatbot.dto.ChatDto.ConversationRequestDto;
import kr.swyp.backend.chatbot.dto.ChatDto.ConversationResponseDto;
import kr.swyp.backend.chatbot.repository.ChatHistoryRepository;
import kr.swyp.backend.chatbot.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatServiceImpl implements ChatService {

    private final OpenAiClient chatClient;
    private final ChatHistoryRepository chatHistoryRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatPromptService chatPromptService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ChatResponseDto ask(UUID memberId, ChatRequestDto request) {
        String message = request.getMessage();
        String sessionId = request.getSessionId();

        if (!StringUtils.hasText(sessionId)) {
            // 세션이 없으면 새 세션 생성 및 저장
            sessionId = UUID.randomUUID().toString();

            // 세션 제목은 초기 메시지로 짧게 생성
            String title = generateSessionTitle(message);

            ChatSession newSession = ChatSession.builder()
                    .sessionId(sessionId)
                    .memberId(memberId)
                    .title(title)
                    .build();

            chatSessionRepository.save(newSession);
            log.info("[챗봇] 사용자 {} 새로운 세션 생성: {}", memberId, sessionId);
        }

        log.info("[챗봇] 사용자 {} (세션 {}) 질문: {}", memberId, sessionId, message);

        try {

            // 최근 대화 기록 조회 (세션 단위)
            List<ChatHistory> recentHistory = chatHistoryRepository
                    .findTop5ByMemberIdAndSessionIdOrderByIdDesc(memberId, sessionId);

            String contextualPrompt = buildContextualPrompt(message, recentHistory);

            // Request DTO 생성
            OpenAiChatRequest openAiRequest = OpenAiChatRequest.builder()
                    .model("gpt-4")
                    .messages(List.of(
                            OpenAiChatRequest.Message.builder()
                                    .role("system")
                                    .content(chatPromptService.createMessageExtractionPrompt(
                                            message))
                                    .build(),
                            OpenAiChatRequest.Message.builder()
                                    .role("user")
                                    .content(contextualPrompt)
                                    .build()
                    ))
                    .temperature(0.7)
                    .build();

            // OpenFeign 호출 (Authorization 헤더 자동 추가)
            OpenAiChatResponse openAiResponse = chatClient.createChatCompletion(openAiRequest);

            // 응답 추출
            String responseContent = openAiResponse.getChoices().get(0)
                    .getMessage()
                    .getContent();

            log.info("[챗봇] OpenAI 응답: {}", responseContent);

            ChatExtractionResultDto result = parseResponse(responseContent);

            // 대화 기록 저장
            saveChatHistory(memberId, message, result, sessionId);

            return ChatResponseDto.builder()
                    .contents(result.getAnswers())
                    .sender("Near")
                    .build();

        } catch (Exception e) {
            log.error("[챗봇] 사용자 {} 요청 처리 중 오류 발생: {}", memberId, e.getMessage(), e);
            return createErrorResponse();
        }
    }

    private String buildContextualPrompt(String message, List<ChatHistory> history) {
        if (history.isEmpty()) {
            return message;
        }

        StringBuilder context = new StringBuilder();
        context.append("이전 대화 기록:\n");

        // 최신 대화부터 역순으로 보여주기 (가장 최근 대화가 더 중요)
        IntStream.range(0, history.size())
                .boxed()
                .sorted(Collections.reverseOrder())
                .forEach(i -> {
                    ChatHistory h = history.get(i);
                    context.append(String.format("[대화 %d]\n", history.size() - i));
                    context.append("사용자: ").append(h.getQuestion()).append("\n");
                    context.append("대상: ").append(h.getTarget()).append(", 주제: ")
                            .append(h.getTopic()).append("\n");
                    if (h.getResponse() != null) {
                        context.append("AI 응답: ").append(h.getResponse()).append("\n");
                    }
                    context.append("\n");
                });

        context.append("현재 사용자 메시지: ").append(message);
        context.append("\n\n위의 대화 기록을 참고하여, 사용자의 대화 패턴과 선호도를 이해하고 더 개인화된 응답을 제공해주세요.");

        return context.toString();
    }

    private ChatExtractionResultDto parseResponse(String responseJson)
            throws JsonProcessingException {
        try {
            return objectMapper.readValue(responseJson, ChatExtractionResultDto.class);
        } catch (JsonProcessingException e) {
            log.warn("[챗봇] AI 응답 파싱 실패: {}", responseJson);
            throw e;
        }
    }

    private void saveChatHistory(UUID memberId, String message, ChatExtractionResultDto result,
            String sessionId) {
        // AI 응답을 문자열로 변환 (JSON 배열을 읽기 좋은 형태로)
        String formattedResponse = String.join("\n", result.getAnswers());

        ChatHistory history = ChatHistory.builder()
                .memberId(memberId)
                .target(result.getTarget())
                .question(message)
                .topic(result.getTopic())
                .sessionId(sessionId)
                .response(formattedResponse)
                .build();

        chatHistoryRepository.save(history);
        log.debug("[챗봇] 사용자 {} 대화 기록 저장 완료", memberId);
    }

    private ChatResponseDto createErrorResponse() {
        return ChatResponseDto.builder()
                .contents(List.of(
                        "죄송해요, 요청을 처리하는 중에 문제가 발생했어요.",
                        "다시 한 번 시도해 주시겠어요?"
                ))
                .sender("Near")
                .build();
    }

    @Override
    public List<ChatHistoryDto> getChatHistory(UUID memberId) {
        log.info("[챗봇] 사용자 {} 대화 기록 조회 중", memberId);

        List<ChatHistory> histories = chatHistoryRepository
                .findTop5ByMemberIdOrderByIdDesc(memberId);

        return histories.stream()
                .map(history -> ChatHistoryDto.builder()
                        .id(history.getId())
                        .target(history.getTarget())
                        .topic(history.getTopic())
                        .question(history.getQuestion())
                        .response(history.getResponse())
                        .createdAt(history.getCreatedAt())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public ChatSessionDto startNewSession(UUID memberId, String initialMessage) {
        log.info("[챗봇] 사용자 {}의 새로운 채팅 세션 시작", memberId);

        String sessionId = UUID.randomUUID().toString();
        String title = generateSessionTitle(initialMessage);

        ChatSession session = ChatSession.builder()
                .sessionId(sessionId)
                .memberId(memberId)
                .title(title)
                .isActive(true)
                .build();

        chatSessionRepository.save(session);

        return ChatSessionDto.builder()
                .sessionId(sessionId)
                .title(title)
                .createdAt(session.getCreatedAt())
                .isActive(session.getIsActive())
                .build();
    }

    @Override
    @Transactional
    public ConversationResponseDto continueConversation(UUID memberId,
            ConversationRequestDto request) {
        String sessionId = request.getSessionId();
        String userMessage = request.getMessage();

        log.info("[챗봇] 세션 {}에서 사용자 {}의 연속 대화", sessionId, memberId);

        // 세션 검증
        ChatSession session = chatSessionRepository.findBySessionIdAndIsActiveTrue(sessionId)
                .orElseThrow(
                        () -> new IllegalArgumentException("활성화된 세션을 찾을 수 없습니다: " + sessionId));

        if (!session.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("해당 세션에 접근할 권한이 없습니다.");
        }

        try {
            // 사용자 메시지 저장
            saveConversationMessage(sessionId, memberId, userMessage, "USER");

            // 세션 내 대화 기록 조회
            List<ChatHistory> sessionHistory = chatHistoryRepository
                    .findBySessionIdOrderByCreatedAtAsc(sessionId);

            String conversationContext = buildConversationContext(sessionHistory, userMessage);

            // Request DTO 생성
            OpenAiChatRequest openAiRequest = OpenAiChatRequest.builder()
                    .model("gpt-4")
                    .messages(List.of(
                            OpenAiChatRequest.Message.builder()
                                    .role("system")
                                    .content(chatPromptService.createConversationPrompt(
                                            conversationContext))
                                    .build(),
                            OpenAiChatRequest.Message.builder()
                                    .role("user")
                                    .content(userMessage)
                                    .build()
                    ))
                    .temperature(0.7)
                    .build();

            // OpenFeign 호출 (Authorization 헤더 자동 추가)
            OpenAiChatResponse openAiResponse = chatClient.createChatCompletion(openAiRequest);

            // 응답 추출
            String responseContent = openAiResponse.getChoices().get(0)
                    .getMessage()
                    .getContent();

            log.debug("[챗봇] OpenAI 응답: {}", responseContent);

            // AI 응답 저장
            ChatHistory botMessage = saveConversationMessage(sessionId, memberId, responseContent,
                    "BOT");

            return ConversationResponseDto.builder()
                    .sessionId(sessionId)
                    .message(responseContent)
                    .sender("Near")
                    .timestamp(botMessage.getCreatedAt())
                    .build();

        } catch (Exception e) {
            log.error("[챗봇] 세션 {} 대화 처리 중 오류: {}", sessionId, e.getMessage(), e);

            // 오류 시에도 응답 반환
            String errorResponse = "죄송해요, 응답을 생성하는 중에 문제가 발생했어요. 다시 시도해 주세요.";
            ChatHistory botMessage = saveConversationMessage(sessionId, memberId, errorResponse,
                    "BOT");

            return ConversationResponseDto.builder()
                    .sessionId(sessionId)
                    .message(errorResponse)
                    .sender("Near")
                    .timestamp(botMessage.getCreatedAt())
                    .build();
        }
    }

    @Override
    public List<ChatSessionDto> getUserSessions(UUID memberId) {
        log.info("[챗봇] 사용자 {}의 채팅 세션 목록 조회", memberId);

        return chatSessionRepository.findByMemberIdAndIsActiveTrueOrderByCreatedAtDesc(memberId)
                .stream()
                .map(session -> ChatSessionDto.builder()
                        .sessionId(session.getSessionId())
                        .title(session.getTitle())
                        .createdAt(session.getCreatedAt())
                        .isActive(session.getIsActive())
                        .build())
                .toList();
    }

    @Override
    public List<ConversationResponseDto> getSessionHistory(UUID memberId, String sessionId) {
        log.info("[챗봇] 사용자 {}의 세션 {} 대화 기록 조회", memberId, sessionId);

        // 세션 접근 권한 검증
        ChatSession session = chatSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다: " + sessionId));

        if (!session.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("해당 세션에 접근할 권한이 없습니다.");
        }

        return chatHistoryRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream()
                .map(history -> ConversationResponseDto.builder()
                        .sessionId(sessionId)
                        .message("USER".equals(history.getMessageType())
                                ? history.getQuestion() : history.getResponse())
                        .sender("USER".equals(history.getMessageType()) ? "User" : "Near")
                        .timestamp(history.getCreatedAt())
                        .build())
                .toList();
    }

    private String generateSessionTitle(String initialMessage) {
        // 첫 메시지에서 제목 생성 (최대 50자)
        if (initialMessage.length() > 50) {
            return initialMessage.substring(0, 47) + "...";
        }
        return initialMessage;
    }

    private ChatHistory saveConversationMessage(String sessionId, UUID memberId, String message,
            String messageType) {
        ChatHistory.ChatHistoryBuilder builder = ChatHistory.builder()
                .sessionId(sessionId)
                .memberId(memberId)
                .messageType(messageType);

        if ("USER".equals(messageType)) {
            builder.question(message);
        } else {
            builder.response(message);
        }

        return chatHistoryRepository.save(builder.build());
    }

    private String buildConversationContext(List<ChatHistory> sessionHistory,
            String currentMessage) {
        if (sessionHistory.isEmpty()) {
            return "새로운 대화입니다. 사용자: " + currentMessage;
        }

        StringBuilder context = new StringBuilder();
        context.append("이전 대화 내용:\n");

        sessionHistory.stream()
                .forEach(history -> {
                    if ("USER".equals(history.getMessageType()) && history.getQuestion() != null) {
                        context.append("사용자: ").append(history.getQuestion()).append("\n");
                    } else if ("BOT".equals(history.getMessageType())
                            && history.getResponse() != null) {
                        context.append("Near: ").append(history.getResponse()).append("\n");
                    }
                });

        context.append("\n현재 사용자 메시지: ").append(currentMessage);
        context.append("\n\n위 대화 맥락을 고려하여 자연스럽고 도움이 되는 응답을 해주세요.");

        return context.toString();
    }
}
