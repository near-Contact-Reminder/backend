package kr.swyp.backend.chatbot.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import kr.swyp.backend.authentication.provider.TokenProvider;
import kr.swyp.backend.chatbot.dto.ChatDto.ChatHistoryDto;
import kr.swyp.backend.chatbot.dto.ChatDto.ChatRequestDto;
import kr.swyp.backend.chatbot.dto.ChatDto.ChatResponseDto;
import kr.swyp.backend.chatbot.dto.ChatDto.ChatSessionDto;
import kr.swyp.backend.chatbot.dto.ChatDto.ConversationRequestDto;
import kr.swyp.backend.chatbot.dto.ChatDto.ConversationResponseDto;
import kr.swyp.backend.chatbot.service.ChatService;
import kr.swyp.backend.member.domain.Member;
import kr.swyp.backend.member.dto.MemberDetails;
import kr.swyp.backend.member.enums.RoleType;
import kr.swyp.backend.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@Transactional
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private MemberRepository memberRepository;

    @MockitoBean
    private ChatService chatService;

    private Member testMember;
    private String accessToken;

    @BeforeEach
    void setUp() {
        // 테스트 멤버 생성
        testMember = Member.builder()
                .username("test@example.com")
                .nickname("테스트유저")
                .build();
        testMember.addRole(RoleType.USER);
        memberRepository.save(testMember);

        // JWT 토큰 생성
        MemberDetails memberDetails = new MemberDetails(
                testMember.getMemberId(),
                testMember.getUsername(),
                "",
                List.of(new SimpleGrantedAuthority("USER"))
        );
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        memberDetails,
                        null,
                        List.of(new SimpleGrantedAuthority("USER"))
                );
        accessToken = tokenProvider.generateAccessToken(authentication);
    }

    @Test
    @DisplayName("메시지 추천을 할 수 있어야 한다.")
    void 메시지_추천을_할_수_있어야_한다() throws Exception {
        // given
        ChatRequestDto request = ChatRequestDto.builder()
                .message("상사에게 회의 일정 변경 요청하고 싶어")
                .build();

        ChatResponseDto mockResponse = ChatResponseDto.builder()
                .sender("Near")
                .contents(List.of("안녕하세요, 회의 일정 변경 요청드립니다.", "혹시 가능하시다면 시간 조정 부탁드려요."))
                .build();

        when(chatService.ask(eq(testMember.getMemberId()), any(ChatRequestDto.class)))
                .thenReturn(mockResponse);

        // when & then
        ResultActions result = mockMvc.perform(post("/chat/ask")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.sender").value("Near"))
                .andExpect(jsonPath("$.contents").isArray())
                .andDo(document("chat-ask-message",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName("Authorization").description("Bearer 토큰")
                        ),
                        requestFields(
                                fieldWithPath("message").description("추천받고 싶은 메시지 상황")
                        ),
                        responseFields(
                                fieldWithPath("contents").description("추천 메시지 목록"),
                                fieldWithPath("sender").description("응답자 (Near)")
                        )
                ));
    }

    @Test
    @DisplayName("새로운 채팅 세션 시작을 할 수 있어야 한다.")
    void 새로운_채팅_세션_시작을_할_수_있어야_한다() throws Exception {
        // given
        String initialMessage = "안녕하세요! 챗봇과 대화를 시작합니다.";

        ChatSessionDto mockResponse = ChatSessionDto.builder()
                .sessionId("test-session-id")
                .title(initialMessage)
                .createdAt(LocalDateTime.now())
                .isActive(true)
                .build();

        when(chatService.startNewSession(eq(testMember.getMemberId()), eq(initialMessage)))
                .thenReturn(mockResponse);

        // when & then
        ResultActions result = mockMvc.perform(post("/chat/sessions/start")
                .header("Authorization", "Bearer " + accessToken)
                .param("initialMessage", initialMessage));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").exists())
                .andExpect(jsonPath("$.title").value(initialMessage))
                .andExpect(jsonPath("$.isActive").value(true))
        ;
    }

    @Test
    @DisplayName("연속 대화를 할 수 있어야 한다.")
    void 연속_대화를_할_수_있어야_한다() throws Exception {
        // given
        ConversationRequestDto request = ConversationRequestDto.builder()
                .sessionId("test-session-id")
                .message("오늘 날씨가 어때?")
                .build();

        ConversationResponseDto mockResponse = ConversationResponseDto.builder()
                .sessionId("test-session-id")
                .message("오늘 날씨는 맑고 좋습니다!")
                .sender("Near")
                .timestamp(LocalDateTime.now())
                .build();

        when(chatService.continueConversation(eq(testMember.getMemberId()),
                any(ConversationRequestDto.class)))
                .thenReturn(mockResponse);

        // when & then
        ResultActions result = mockMvc.perform(post("/chat/sessions/continue")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("test-session-id"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.sender").value("Near"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andDo(document("chat-continue-conversation",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName("Authorization").description("Bearer 토큰")
                        ),
                        requestFields(
                                fieldWithPath("sessionId").description("세션 ID"),
                                fieldWithPath("message").description("사용자 메시지")
                        ),
                        responseFields(
                                fieldWithPath("sessionId").description("세션 ID"),
                                fieldWithPath("message").description("챗봇 응답 메시지"),
                                fieldWithPath("sender").description("응답자 (Near)"),
                                fieldWithPath("timestamp").description("응답 시간")
                        )
                ));
    }

    @Test
    @DisplayName("사용자 채팅 세션 목록을 조회할 수 있어야 한다.")
    void 사용자_채팅_세션_목록을_조회할_수_있어야_한다() throws Exception {
        // given
        List<ChatSessionDto> mockSessions = List.of(
                ChatSessionDto.builder()
                        .sessionId("test-session-id")
                        .title("테스트 세션")
                        .createdAt(LocalDateTime.now())
                        .isActive(true)
                        .build()
        );

        when(chatService.getUserSessions(eq(testMember.getMemberId())))
                .thenReturn(mockSessions);

        // when & then
        ResultActions result = mockMvc.perform(get("/chat/sessions")
                .header("Authorization", "Bearer " + accessToken));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].sessionId").value("test-session-id"))
                .andExpect(jsonPath("$[0].title").value("테스트 세션"))
                .andDo(document("chat-get-user-sessions",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName("Authorization").description("Bearer 토큰")
                        ),
                        responseFields(
                                fieldWithPath("[].sessionId").description("세션 ID"),
                                fieldWithPath("[].title").description("세션 제목"),
                                fieldWithPath("[].createdAt").description("세션 생성 시간"),
                                fieldWithPath("[].isActive").description("세션 활성 상태")
                        )
                ));
    }

    @Test
    @DisplayName("특정 세션의 대화 기록을 조회할 수 있어야 한다.")
    void 특정_세션의_대화_기록을_조회할_수_있어야_한다() throws Exception {
        // given
        String sessionId = "test-session-id";
        List<ConversationResponseDto> mockHistory = List.of(
                ConversationResponseDto.builder()
                        .sessionId(sessionId)
                        .message("친구에게 생일축하 메시지를 보내고 싶어")
                        .sender("User")
                        .timestamp(LocalDateTime.now())
                        .build()
        );

        when(chatService.getSessionHistory(eq(testMember.getMemberId()), eq(sessionId)))
                .thenReturn(mockHistory);

        // when & then
        ResultActions result = mockMvc.perform(get("/chat/sessions/{sessionId}/history", sessionId)
                .header("Authorization", "Bearer " + accessToken));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].sessionId").value(sessionId))
                .andExpect(jsonPath("$[0].message").value("친구에게 생일축하 메시지를 보내고 싶어"))
                .andExpect(jsonPath("$[0].sender").value("User"))
                .andDo(document("chat-get-session-history",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName("Authorization").description("Bearer 토큰")
                        ),
                        pathParameters(
                                parameterWithName("sessionId").description("조회할 세션 ID")
                        ),
                        responseFields(
                                fieldWithPath("[].sessionId").description("세션 ID"),
                                fieldWithPath("[].message").description("메시지 내용"),
                                fieldWithPath("[].sender").description("발신자 (User 또는 Near)"),
                                fieldWithPath("[].timestamp").description("메시지 시간")
                        )
                ));
    }

    @Test
    @DisplayName("대화 기록을 조회할 수 있어야 한다.")
    void 대화_기록을_조회할_수_있어야_한다() throws Exception {
        // given
        List<ChatHistoryDto> mockHistory = List.of(
                ChatHistoryDto.builder()
                        .id(1L)
                        .target("친구")
                        .topic("생일축하")
                        .question("친구에게 생일축하 메시지를 보내고 싶어")
                        .response("생일 축하해! 오늘 하루 행복한 하루 보내!")
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        when(chatService.getChatHistory(eq(testMember.getMemberId())))
                .thenReturn(mockHistory);

        // when & then
        ResultActions result = mockMvc.perform(get("/chat/history")
                .header("Authorization", "Bearer " + accessToken));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].target").value("친구"))
                .andExpect(jsonPath("$[0].topic").value("생일축하"))
                .andDo(document("chat-get-history",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName("Authorization").description("Bearer 토큰")
                        ),
                        responseFields(
                                fieldWithPath("[].id").description("기록 ID"),
                                fieldWithPath("[].target").description("메시지 대상"),
                                fieldWithPath("[].topic").description("메시지 주제"),
                                fieldWithPath("[].question").description("사용자 질문"),
                                fieldWithPath("[].response").description("AI 응답"),
                                fieldWithPath("[].createdAt").description("생성 시간")
                        )
                ));
    }

    @Test
    @DisplayName("인증 없이 요청 시 401 오류를 반환해야 한다.")
    void 인증_없이_요청_시_401_오류를_반환해야_한다() throws Exception {
        // given
        ChatRequestDto request = ChatRequestDto.builder()
                .message("테스트 메시지")
                .build();

        // when & then
        mockMvc.perform(post("/chat/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}