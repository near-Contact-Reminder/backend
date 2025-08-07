package kr.swyp.backend.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.swyp.backend.chatbot.domain.ChatHistory;
import kr.swyp.backend.chatbot.domain.ChatSession;
import kr.swyp.backend.chatbot.dto.ChatDto.ChatExtractionResultDto;
import kr.swyp.backend.chatbot.dto.ChatDto.ChatRequestDto;
import kr.swyp.backend.chatbot.dto.ChatDto.ChatResponseDto;
import kr.swyp.backend.chatbot.dto.ChatDto.ChatSessionDto;
import kr.swyp.backend.chatbot.dto.ChatDto.ConversationRequestDto;
import kr.swyp.backend.chatbot.dto.ChatDto.ConversationResponseDto;
import kr.swyp.backend.chatbot.repository.ChatHistoryRepository;
import kr.swyp.backend.chatbot.repository.ChatSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.test.context.ActiveProfiles;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class ChatServiceImplTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatHistoryRepository chatHistoryRepository;

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @Mock
    private ChatPromptService chatPromptService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ChatServiceImpl chatService;

    private UUID memberId;
    private String sessionId;
    private ChatHistory chatHistory;
    private ChatSession chatSession;

    @BeforeEach
    void setUp() {
        memberId = UUID.randomUUID();
        sessionId = UUID.randomUUID().toString();

        chatHistory = ChatHistory.builder()
                .id(1L)
                .memberId(memberId)
                .sessionId(sessionId)
                .target("친구")
                .topic("생일축하")
                .question("친구에게 생일축하 메시지를 보내고 싶어")
                .response("생일 축하해! 오늘 하루 행복한 하루 보내!")
                .messageType("USER")
                .build();

        chatSession = ChatSession.builder()
                .id(1L)
                .sessionId(sessionId)
                .memberId(memberId)
                .title("친구에게 생일축하 메시지를 보내고 싶어")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("메시지 추천 요청 시 정상적으로 응답을 반환한다.")
    void 메시지_추천_요청을_할_수_있어야_한다() throws JsonProcessingException {
        // given
        ChatRequestDto request = ChatRequestDto.builder()
                .message("친구에게 생일축하 메시지를 보내고 싶어")
                .build();

        String aiResponseJson = """
                {
                  "target": "친구",
                  "topic": "생일축하",
                  "answers": [
                    "생일 축하해! 오늘 하루 행복한 하루 보내!",
                    "또 한 살 먹었네! 생일 축하하고 맛있는 거 많이 먹어~",
                    "생일 축하합니다! 새로운 한 해도 건강하고 행복하게 보내세요"
                  ]
                }
                """;

        ChatExtractionResultDto extractionResult = ChatExtractionResultDto.builder()
                .target("친구")
                .topic("생일축하")
                .answers(List.of(
                        "생일 축하해! 오늘 하루 행복한 하루 보내!",
                        "또 한 살 먹었네! 생일 축하하고 맛있는 거 많이 먹어~",
                        "생일 축하합니다! 새로운 한 해도 건강하고 행복하게 보내세요"
                ))
                .build();

        given(chatHistoryRepository.findTop5ByMemberIdOrderByIdDesc(memberId))
                .willReturn(List.of());
        given(chatPromptService.createMessageExtractionPrompt(anyString()))
                .willReturn("test prompt");
        given(chatClient.prompt()).willReturn(requestSpec);
        given(requestSpec.system(anyString())).willReturn(requestSpec);
        given(requestSpec.user(anyString())).willReturn(requestSpec);
        given(requestSpec.advisors(any(SimpleLoggerAdvisor.class))).willReturn(requestSpec);
        given(requestSpec.call()).willReturn(callResponseSpec);
        given(callResponseSpec.content()).willReturn(aiResponseJson);
        given(objectMapper.readValue(aiResponseJson, ChatExtractionResultDto.class))
                .willReturn(extractionResult);
        given(chatHistoryRepository.save(any(ChatHistory.class)))
                .willReturn(chatHistory);

        // when
        ChatResponseDto response = chatService.ask(memberId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContents()).hasSize(3);
        assertThat(response.getSender()).isEqualTo("Near");
        assertThat(response.getContents()).contains("생일 축하해! 오늘 하루 행복한 하루 보내!");

        verify(chatHistoryRepository).save(any(ChatHistory.class));
    }

    @Test
    @DisplayName("새로운 채팅 세션을 시작할 수 있다.")
    void 새로운_채팅_세션을_시작할_수_있어야_한다() {
        // given
        String initialMessage = "안녕하세요! 챗봇과 대화를 시작합니다.";

        given(chatSessionRepository.save(any(ChatSession.class)))
                .willReturn(chatSession);

        // when
        ChatSessionDto result = chatService.startNewSession(memberId, initialMessage);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getSessionId()).isNotNull();
        assertThat(result.getTitle()).isEqualTo(initialMessage);
        assertThat(result.getIsActive()).isTrue();

        verify(chatSessionRepository).save(any(ChatSession.class));
    }

    @Test
    @DisplayName("기존 세션에서 대화를 계속할 수 있다.")
    void 기존_세션에서_대화를_계속할_수_있어야_한다() {
        // given
        ConversationRequestDto request = ConversationRequestDto.builder()
                .sessionId(sessionId)
                .message("오늘 날씨가 어때?")
                .build();

        String aiResponse = "오늘 날씨에 대해 구체적으로 알려드리기 어렵지만, 날씨 앱을 확인해보시는 것은 어떨까요?";

        given(chatSessionRepository.findBySessionIdAndIsActiveTrue(sessionId))
                .willReturn(Optional.of(chatSession));
        given(chatHistoryRepository.findBySessionIdOrderByCreatedAtAsc(sessionId))
                .willReturn(List.of());
        given(chatPromptService.createConversationPrompt(anyString()))
                .willReturn("conversation prompt");

        // continueConversation에서는 advisors를 사용하지 않음
        given(chatClient.prompt()).willReturn(requestSpec);
        given(requestSpec.system(anyString())).willReturn(requestSpec);
        given(requestSpec.user(anyString())).willReturn(requestSpec);
        given(requestSpec.call()).willReturn(callResponseSpec);
        given(callResponseSpec.content()).willReturn(aiResponse);
        given(chatHistoryRepository.save(any(ChatHistory.class)))
                .willReturn(chatHistory);

        // when
        ConversationResponseDto response = chatService.continueConversation(memberId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getSessionId()).isEqualTo(sessionId);
        assertThat(response.getMessage()).isEqualTo(aiResponse);
        assertThat(response.getSender()).isEqualTo("Near");

        verify(chatHistoryRepository, times(2)).save(any(ChatHistory.class));
    }

    @Test
    @DisplayName("존재하지 않는 세션으로 대화 시 예외가 발생한다.")
    void 존재하지_않는_세션으로_대화_시_예외가_발생시켜야_한다() {
        // given
        ConversationRequestDto request = ConversationRequestDto.builder()
                .sessionId("nonexistent-session")
                .message("안녕하세요")
                .build();

        given(chatSessionRepository.findBySessionIdAndIsActiveTrue("nonexistent-session"))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatService.continueConversation(memberId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("활성화된 세션을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("다른 사용자의 세션에 접근 시 예외가 발생한다.")
    void 다른_사용자의_세션에_접근_시_예외가_발생시켜야_한다() {
        // given
        UUID otherMemberId = UUID.randomUUID();
        ConversationRequestDto request = ConversationRequestDto.builder()
                .sessionId(sessionId)
                .message("안녕하세요")
                .build();

        given(chatSessionRepository.findBySessionIdAndIsActiveTrue(sessionId))
                .willReturn(Optional.of(chatSession));

        // when & then
        assertThatThrownBy(() -> chatService.continueConversation(otherMemberId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 세션에 접근할 권한이 없습니다");
    }

    @Test
    @DisplayName("사용자의 채팅 세션 목록을 조회할 수 있다.")
    void 사용자의_채팅_세션_목록을_조회할_수_있어야_한다() {
        // given
        List<ChatSession> sessions = List.of(chatSession);
        given(chatSessionRepository.findByMemberIdAndIsActiveTrueOrderByCreatedAtDesc(memberId))
                .willReturn(sessions);

        // when
        List<ChatSessionDto> result = chatService.getUserSessions(memberId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSessionId()).isEqualTo(sessionId);
        assertThat(result.get(0).getTitle()).isEqualTo("친구에게 생일축하 메시지를 보내고 싶어");
    }

    @Test
    @DisplayName("특정 세션의 대화 기록을 조회할 수 있다.")
    void 특정_세션의_대화_기록을_조회할_수_있어야_한다() {
        // given
        List<ChatHistory> histories = List.of(chatHistory);
        given(chatSessionRepository.findBySessionId(sessionId))
                .willReturn(Optional.of(chatSession));
        given(chatHistoryRepository.findBySessionIdOrderByCreatedAtAsc(sessionId))
                .willReturn(histories);

        // when
        List<ConversationResponseDto> result = chatService.getSessionHistory(memberId, sessionId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSessionId()).isEqualTo(sessionId);
        assertThat(result.get(0).getMessage()).isEqualTo("친구에게 생일축하 메시지를 보내고 싶어");
        assertThat(result.get(0).getSender()).isEqualTo("User");
    }

}