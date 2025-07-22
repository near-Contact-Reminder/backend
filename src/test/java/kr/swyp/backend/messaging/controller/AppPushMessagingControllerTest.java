package kr.swyp.backend.messaging.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import kr.swyp.backend.authentication.provider.TokenProvider;
import kr.swyp.backend.member.domain.Member;
import kr.swyp.backend.member.dto.MemberDetails;
import kr.swyp.backend.member.enums.RoleType;
import kr.swyp.backend.member.repository.MemberRepository;
import kr.swyp.backend.messaging.dto.TokenDto.RegisterAppPushTokenRequest;
import kr.swyp.backend.messaging.dto.TokenDto.UnregisterAppPushTokenRequest;
import kr.swyp.backend.messaging.enums.AppPushTokenOsType;
import kr.swyp.backend.messaging.service.AppPushMessagingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@Transactional
class AppPushMessagingControllerTest {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String AUTHORIZATION_VALUE_PREFIX = "Bearer ";

    private final String url = "/messaging";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AppPushMessagingServiceImpl appPushMessagingService;

    private Member testMember;

    @BeforeEach
    void setUp() {
        // 테스트용 회원 생성
        testMember = memberRepository.save(
                Member.builder()
                        .username("testuser@example.com")
                        .password("encoded_password")
                        .nickname("테스트유저")
                        .isActive(true)
                        .build()
        );

        // 역할 추가
        testMember.addRole(RoleType.USER);
    }

    @Test
    @DisplayName("앱 푸시 토큰 등록을 할 수 있어야 한다.")
    void 앱_푸시_토큰_등록을_할_수_있어야_한다() throws Exception {
        // given
        UUID memberId = testMember.getMemberId();
        String accessToken = createAccessToken(memberId);
        String token = "test-token";
        AppPushTokenOsType osType = AppPushTokenOsType.IOS;

        RegisterAppPushTokenRequest request = RegisterAppPushTokenRequest.builder()
                .token(token)
                .osType(osType)
                .build();

        System.out.println(objectMapper.writeValueAsString(request));
        // when
        ResultActions result = mockMvc.perform(post(url + "/register")
                .header(AUTHORIZATION_HEADER, AUTHORIZATION_VALUE_PREFIX + accessToken)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("기기 등록이 완료되었습니다."));

        // docs
        result.andDo(document("앱 푸시 토큰 등록",
                "앱 푸시 토큰을 등록한다.",
                "앱 푸시 토큰 등록",
                false,
                false,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                        headerWithName(AUTHORIZATION_HEADER).description("발급받은 JWT")),
                requestFields(
                        fieldWithPath("token").description("앱 푸시 토큰"),
                        fieldWithPath("osType").description("앱 푸시 토큰의 운영체제 타입 (IOS, ANDROID)")
                ),
                responseFields(
                        fieldWithPath("message").description("기기 등록 완료 메시지")
                )));
    }

    @Test
    @DisplayName("앱 푸시 등록을 취소할 수 있어야 한다.")
    void 앱_푸시_등록을_취소할_수_있어야_한다() throws Exception {
        // given
        UUID memberId = testMember.getMemberId();
        String accessToken = createAccessToken(memberId);
        String token = "test-token";
        AppPushTokenOsType osType = AppPushTokenOsType.IOS;

        // 먼저 토큰을 등록
        RegisterAppPushTokenRequest registerRequest = RegisterAppPushTokenRequest.builder()
                .token(token)
                .osType(osType)
                .build();

        appPushMessagingService.registerDevice(memberId, registerRequest);

        UnregisterAppPushTokenRequest request = UnregisterAppPushTokenRequest.builder()
                .token(token)
                .build();

        // when
        ResultActions result = mockMvc.perform(delete(url + "/unregister")
                .header(AUTHORIZATION_HEADER, AUTHORIZATION_VALUE_PREFIX + accessToken)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isNoContent());

        // docs
        result.andDo(document("앱 푸시 등록 취소",
                "앱 푸시 등록을 취소한다.",
                "앱 푸시 등록 취소",
                false,
                false,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                        headerWithName(AUTHORIZATION_HEADER).description("발급받은 JWT")),
                requestFields(
                        fieldWithPath("token").description("취소할 앱 푸시 토큰")
                )));
    }


    private String createAccessToken(UUID memberId) {
        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(RoleType.USER.name()));
        MemberDetails memberDetails = new MemberDetails(memberId, "test", "", authorities);
        Authentication authentication = new UsernamePasswordAuthenticationToken(memberDetails, "",
                authorities);
        return tokenProvider.generateAccessToken(authentication);
    }
}