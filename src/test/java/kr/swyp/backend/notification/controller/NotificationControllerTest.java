package kr.swyp.backend.notification.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
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
import kr.swyp.backend.notification.dto.ForceSendNotificationRequest;
import kr.swyp.backend.notification.dto.SendTestNotificationRequest;
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
class NotificationControllerTest {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String AUTHORIZATION_VALUE_PREFIX = "Bearer ";

    private final String url = "/notifications";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    private Member testMember;

    @BeforeEach
    void setUp() {
        // 테스트용 회원 생성 (FCM 토큰 포함)
        testMember = memberRepository.save(
                Member.builder()
                        .username("testuser@example.com")
                        .password("encoded_password")
                        .nickname("테스트유저")
                        .isActive(true)
                        .fcmToken("test-fcm-token-12345")
                        .build()
        );

        // 역할 추가
        testMember.addRole(RoleType.USER);
    }

    @Test
    @DisplayName("JWT 인증된 사용자에게 테스트 FCM 알림을 전송할 수 있어야 한다.")
    void JWT_인증된_사용자에게_테스트_FCM_알림을_전송할_수_있어야_한다() throws Exception {
        // given
        UUID memberId = testMember.getMemberId();
        String accessToken = createAccessToken(memberId);

        SendTestNotificationRequest request = SendTestNotificationRequest.builder()
                .title("테스트 알림")
                .body("이것은 테스트 알림입니다.")
                .build();

        // when
        ResultActions result = mockMvc.perform(post(url + "/test/send")
                .header(AUTHORIZATION_HEADER, AUTHORIZATION_VALUE_PREFIX + accessToken)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("FCM 알림이 성공적으로 전송되었습니다."))
                .andExpect(jsonPath("$.memberId").value(memberId.toString()));

        // docs
        result.andDo(document("테스트 FCM 알림 전송",
                "JWT 토큰으로 인증된 사용자에게 테스트 FCM 푸시 알림을 전송한다.",
                "테스트 FCM 알림 전송",
                false,
                false,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                        headerWithName(AUTHORIZATION_HEADER).description("발급받은 JWT 토큰")),
                requestFields(
                        fieldWithPath("title").description("알림 제목"),
                        fieldWithPath("body").description("알림 내용")
                ),
                responseFields(
                        fieldWithPath("message").description("알림 전송 완료 메시지"),
                        fieldWithPath("memberId").description("알림을 받은 회원 ID")
                )));
    }

    @Test
    @DisplayName("FCM 토큰이 없는 사용자는 알림 전송에 실패해야 한다.")
    void FCM_토큰이_없는_사용자는_알림_전송에_실패해야_한다() throws Exception {
        // given
        // FCM 토큰이 없는 회원 생성
        Member memberWithoutFcm = memberRepository.save(
                Member.builder()
                        .username("nofcm@example.com")
                        .password("encoded_password")
                        .nickname("FCM없는유저")
                        .isActive(true)
                        .build()
        );
        memberWithoutFcm.addRole(RoleType.USER);

        UUID memberId = memberWithoutFcm.getMemberId();
        String accessToken = createAccessToken(memberId);

        SendTestNotificationRequest request = SendTestNotificationRequest.builder()
                .title("테스트 알림")
                .body("이것은 테스트 알림입니다.")
                .build();

        // when
        ResultActions result = mockMvc.perform(post(url + "/test/send")
                .header(AUTHORIZATION_HEADER, AUTHORIZATION_VALUE_PREFIX + accessToken)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_STATE"))
                .andExpect(jsonPath("$.message").value("FCM 토큰이 등록되지 않았습니다."));
    }

    @Test
    @DisplayName("제목이 없으면 알림 전송에 실패해야 한다.")
    void 제목이_없으면_알림_전송에_실패해야_한다() throws Exception {
        // given
        UUID memberId = testMember.getMemberId();
        String accessToken = createAccessToken(memberId);

        SendTestNotificationRequest request = SendTestNotificationRequest.builder()
                .title("")  // 빈 제목
                .body("이것은 테스트 알림입니다.")
                .build();

        // when
        ResultActions result = mockMvc.perform(post(url + "/test/send")
                .header(AUTHORIZATION_HEADER, AUTHORIZATION_VALUE_PREFIX + accessToken)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("본문이 없으면 알림 전송에 실패해야 한다.")
    void 본문이_없으면_알림_전송에_실패해야_한다() throws Exception {
        // given
        UUID memberId = testMember.getMemberId();
        String accessToken = createAccessToken(memberId);

        SendTestNotificationRequest request = SendTestNotificationRequest.builder()
                .title("테스트 알림")
                .body("")  // 빈 본문
                .build();

        // when
        ResultActions result = mockMvc.perform(post(url + "/test/send")
                .header(AUTHORIZATION_HEADER, AUTHORIZATION_VALUE_PREFIX + accessToken)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("특정 사용자에게 강제로 FCM 알림을 전송할 수 있어야 한다.")
    void 특정_사용자에게_강제로_FCM_알림을_전송할_수_있어야_한다() throws Exception {
        // given
        UUID memberId = testMember.getMemberId();
        String accessToken = createAccessToken(memberId);
        UUID friendId = UUID.randomUUID();

        ForceSendNotificationRequest request = ForceSendNotificationRequest.builder()
                .memberId(memberId)
                .title("강제 알림 테스트")
                .body("이것은 강제 알림입니다.")
                .friendId(friendId)
                .build();

        // when
        ResultActions result = mockMvc.perform(post(url + "/force/send")
                .header(AUTHORIZATION_HEADER, AUTHORIZATION_VALUE_PREFIX + accessToken)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("FCM 알림이 성공적으로 전송되었습니다."))
                .andExpect(jsonPath("$.memberId").value(memberId.toString()));

        // docs
        result.andDo(document("강제 FCM 알림 전송",
                "특정 사용자에게 강제로 FCM 푸시 알림을 전송한다. (관리자용)",
                "강제 FCM 알림 전송",
                false,
                false,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                        headerWithName(AUTHORIZATION_HEADER).description("발급받은 JWT 토큰")),
                requestFields(
                        fieldWithPath("memberId").description("알림을 받을 회원 ID"),
                        fieldWithPath("title").description("알림 제목"),
                        fieldWithPath("body").description("알림 내용"),
                        fieldWithPath("friendId").description("친구 ID (선택사항, 알림 클릭 시 친구 상세 페이지로 이동)").optional()
                ),
                responseFields(
                        fieldWithPath("message").description("알림 전송 완료 메시지"),
                        fieldWithPath("memberId").description("알림을 받은 회원 ID")
                )));
    }

    @Test
    @DisplayName("friendId 없이도 강제 FCM 알림을 전송할 수 있어야 한다.")
    void friendId_없이도_강제_FCM_알림을_전송할_수_있어야_한다() throws Exception {
        // given
        UUID memberId = testMember.getMemberId();
        String accessToken = createAccessToken(memberId);

        ForceSendNotificationRequest request = ForceSendNotificationRequest.builder()
                .memberId(memberId)
                .title("강제 알림 테스트")
                .body("이것은 강제 알림입니다.")
                .build();

        // when
        ResultActions result = mockMvc.perform(post(url + "/force/send")
                .header(AUTHORIZATION_HEADER, AUTHORIZATION_VALUE_PREFIX + accessToken)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("FCM 알림이 성공적으로 전송되었습니다."))
                .andExpect(jsonPath("$.memberId").value(memberId.toString()));
    }

    @Test
    @DisplayName("존재하지 않는 회원에게는 강제 알림 전송이 실패해야 한다.")
    void 존재하지_않는_회원에게는_강제_알림_전송이_실패해야_한다() throws Exception {
        // given
        UUID memberId = testMember.getMemberId();
        String accessToken = createAccessToken(memberId);
        UUID nonExistentMemberId = UUID.randomUUID();

        ForceSendNotificationRequest request = ForceSendNotificationRequest.builder()
                .memberId(nonExistentMemberId)
                .title("강제 알림 테스트")
                .body("이것은 강제 알림입니다.")
                .build();

        // when
        ResultActions result = mockMvc.perform(post(url + "/force/send")
                .header(AUTHORIZATION_HEADER, AUTHORIZATION_VALUE_PREFIX + accessToken)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("회원을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("FCM 토큰이 없는 회원에게는 강제 알림 전송이 실패해야 한다.")
    void FCM_토큰이_없는_회원에게는_강제_알림_전송이_실패해야_한다() throws Exception {
        // given
        UUID memberId = testMember.getMemberId();
        String accessToken = createAccessToken(memberId);

        Member memberWithoutFcm = memberRepository.save(
                Member.builder()
                        .username("nofcm2@example.com")
                        .password("encoded_password")
                        .nickname("FCM없는유저2")
                        .isActive(true)
                        .build()
        );
        memberWithoutFcm.addRole(RoleType.USER);

        ForceSendNotificationRequest request = ForceSendNotificationRequest.builder()
                .memberId(memberWithoutFcm.getMemberId())
                .title("강제 알림 테스트")
                .body("이것은 강제 알림입니다.")
                .build();

        // when
        ResultActions result = mockMvc.perform(post(url + "/force/send")
                .header(AUTHORIZATION_HEADER, AUTHORIZATION_VALUE_PREFIX + accessToken)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_STATE"))
                .andExpect(jsonPath("$.message").value("FCM 토큰이 등록되지 않았습니다."));
    }

    @Test
    @DisplayName("강제 알림 전송 시 제목이 없으면 실패해야 한다.")
    void 강제_알림_전송_시_제목이_없으면_실패해야_한다() throws Exception {
        // given
        UUID memberId = testMember.getMemberId();
        String accessToken = createAccessToken(memberId);

        ForceSendNotificationRequest request = ForceSendNotificationRequest.builder()
                .memberId(memberId)
                .title("")
                .body("이것은 강제 알림입니다.")
                .build();

        // when
        ResultActions result = mockMvc.perform(post(url + "/force/send")
                .header(AUTHORIZATION_HEADER, AUTHORIZATION_VALUE_PREFIX + accessToken)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("강제 알림 전송 시 본문이 없으면 실패해야 한다.")
    void 강제_알림_전송_시_본문이_없으면_실패해야_한다() throws Exception {
        // given
        UUID memberId = testMember.getMemberId();
        String accessToken = createAccessToken(memberId);

        ForceSendNotificationRequest request = ForceSendNotificationRequest.builder()
                .memberId(memberId)
                .title("강제 알림 테스트")
                .body("")
                .build();

        // when
        ResultActions result = mockMvc.perform(post(url + "/force/send")
                .header(AUTHORIZATION_HEADER, AUTHORIZATION_VALUE_PREFIX + accessToken)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("강제 알림 전송 시 회원 ID가 없으면 실패해야 한다.")
    void 강제_알림_전송_시_회원_ID가_없으면_실패해야_한다() throws Exception {
        // given
        UUID memberId = testMember.getMemberId();
        String accessToken = createAccessToken(memberId);

        ForceSendNotificationRequest request = ForceSendNotificationRequest.builder()
                .memberId(null)
                .title("강제 알림 테스트")
                .body("이것은 강제 알림입니다.")
                .build();

        // when
        ResultActions result = mockMvc.perform(post(url + "/force/send")
                .header(AUTHORIZATION_HEADER, AUTHORIZATION_VALUE_PREFIX + accessToken)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
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
