package kr.swyp.backend.member.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import kr.swyp.backend.authentication.provider.TokenProvider;
import kr.swyp.backend.common.desciptor.ErrorDescriptor;
import kr.swyp.backend.friend.repository.ContactReminderRepository;
import kr.swyp.backend.member.domain.Member;
import kr.swyp.backend.member.domain.MemberSocialLoginInfo;
import kr.swyp.backend.member.dto.MemberDetails;
import kr.swyp.backend.member.dto.MemberDto.MemberWithdrawRequest;
import kr.swyp.backend.member.enums.RoleType;
import kr.swyp.backend.member.enums.SocialLoginProviderType;
import kr.swyp.backend.member.repository.MemberRepository;
import kr.swyp.backend.member.repository.MemberSocialLoginInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@Transactional
class MemberControllerTest {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    private final FieldDescriptor[] memberInfoResponseDescriptor = {
            fieldWithPath("memberId").description("회원 UUID"),
            fieldWithPath("username").description("회원 이메일"),
            fieldWithPath("nickname").description("사용자 이름"),
            fieldWithPath("imageUrl").description("프로필 이미지 URL").optional(),
            fieldWithPath("notificationAgreedAt").description("알림 수신 동의 시각").optional(),
            fieldWithPath("providerType").description("소셜 로그인 제공자"),
    };

    private final String url = "/member";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberSocialLoginInfoRepository socialLoginInfoRepository;

    @MockBean
    private ContactReminderRepository contactReminderRepository;

    private Member testMember;
    private String accessToken;

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

        // 소셜 로그인 정보 추가
        socialLoginInfoRepository.save(MemberSocialLoginInfo.builder()
                .member(testMember)
                .providerType(SocialLoginProviderType.KAKAO)
                .providerId("kakao_123")
                .build());

        // JWT 생성
        accessToken = tokenProvider.generateAccessToken(
                new UsernamePasswordAuthenticationToken(
                        new MemberDetails(
                                testMember.getMemberId(),
                                testMember.getUsername(),
                                testMember.getPassword(),
                                List.of(new SimpleGrantedAuthority(RoleType.USER.name()))
                        ),
                        null,
                        List.of(new SimpleGrantedAuthority(RoleType.USER.name()))
                )
        );
    }

    @Test
    @DisplayName("올바른 회원은 회원 정보가 정상적으로 조회되어야 한다.")
    void 올바른_회원은_회원_정보가_정상적으로_조회되어야_한다() throws Exception {
        // when
        ResultActions result = mockMvc.perform(get(url + "/me")
                        .header(AUTHORIZATION_HEADER, TOKEN_PREFIX + accessToken))
                .andExpect(status().isOk());

        // then
        result.andExpect(jsonPath("$.username").value("testuser@example.com"))
                .andExpect(jsonPath("$.nickname").value("테스트유저"))
                .andExpect(jsonPath("$.providerType").value("KAKAO"));
        // docs
        result.andDo(document("회원 정보 조회 성공",
                "올바른 엑세스 토큰으로 회원 정보를 조회할 수 있다.",
                "올바른 엑세스 토큰으로 회원 정보를 조회",
                false,
                false,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                        headerWithName(AUTHORIZATION_HEADER).description("발급받은 JWT")),
                responseFields(memberInfoResponseDescriptor)
        ));
    }

    @Test
    @DisplayName("존재하지 않는 회원을 조회할 경우 회원 정보 조회에 실패해야 한다")
    void 존재하지_않는_회원을_조회할_경우_회원_정보_조회가_실패해야_한다() throws Exception {
        // given
        UUID randomId = UUID.randomUUID();

        String token = tokenProvider.generateAccessToken(
                new UsernamePasswordAuthenticationToken(
                        new MemberDetails(randomId, "non@exist.com", "", List.of(
                                new SimpleGrantedAuthority(RoleType.USER.name())
                        )),
                        null,
                        List.of(new SimpleGrantedAuthority(RoleType.USER.name()))
                ));

        // when
        ResultActions result = mockMvc.perform(get(url + "/me")
                .header(AUTHORIZATION_HEADER, TOKEN_PREFIX + token));

        // then
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("해당 회원을 찾을 수 없습니다."))
                .andDo(document("회원 정보 조회 실패",
                        "존재하지 않는 회원의 엑세스 토큰으로 회원 정보를 조회할 수 없다.",
                        "존재하지 않는 회원의 엑세스 토큰으로 회원 정보를 조회",
                        false,
                        false,
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                headerWithName(AUTHORIZATION_HEADER).description("발급받은 JWT")),
                        responseFields(ErrorDescriptor.errorResponseFieldDescriptors)
                ));
    }

    @Test
    @DisplayName("회원탈퇴를 할 수 있어야 한다.")
    void 회원탈퇴를_할_수_있어야_한다() throws Exception {
        // given
        String token = tokenProvider.generateAccessToken(
                new UsernamePasswordAuthenticationToken(
                        new MemberDetails(testMember.getMemberId(), testMember.getUsername(), "",
                                List.of(new SimpleGrantedAuthority(RoleType.USER.name()))),
                        null,
                        List.of(new SimpleGrantedAuthority(RoleType.USER.name()))
                ));

        MemberWithdrawRequest request = MemberWithdrawRequest.builder()
                .reasonType("test")
                .customReason("test")
                .build();

        // when
        ResultActions result = mockMvc.perform(delete(url + "/withdraw")
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION_HEADER, TOKEN_PREFIX + token));

        // then
        result.andExpect(status().isNoContent());

        // docs
        result.andDo(document("회원 탈퇴 성공",
                "회원 탈퇴를 성공적으로 수행할 수 있다.",
                "회원 탈퇴",
                false,
                false,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                        headerWithName(AUTHORIZATION_HEADER).description("발급받은 JWT")),
                requestFields(
                        fieldWithPath("reasonType").description("탈퇴 사유"),
                        fieldWithPath("customReason").description("탈퇴 사유 상세").optional()
                )
        ));
    }

    @Test
    @DisplayName("회원 체크율을 계산하고 저장할 수 있다.")
    void 회원_체크율을_계산하고_저장할_수_있다() throws Exception {

        // when
        ResultActions result = mockMvc.perform(get("/member/check-rate")
                        .header(AUTHORIZATION_HEADER, TOKEN_PREFIX + accessToken))
                .andExpect(status().isOk());

        // then
        result.andExpect(jsonPath("$.checkRate").isNumber());

        // docs
        result.andDo(document("회원 체크율 계산 및 저장",
                "회원의 친구 챙김 기록을 기반으로 체크율을 계산하고 저장한 후 응답한다.",
                "회원의 체크율(%)을 반환한다.",
                false,
                false,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                        headerWithName(AUTHORIZATION_HEADER).description("발급받은 JWT")
                ),
                responseFields(
                        fieldWithPath("checkRate").description("회원의 평균 챙김 체크율 (0~100%)")
                )
        ));
    }

    @Test
    @DisplayName("활성 알림이 있는 경우 마이그레이션 상태가 true를 반환해야 한다.")
    void 활성_알림이_있는_경우_마이그레이션_상태가_true를_반환해야_한다() throws Exception {
        // given
        when(contactReminderRepository.findByMemberIdAndIsActiveTrue(testMember.getMemberId()))
                .thenReturn(Collections.singletonList(null)); // Mock에서는 비어있지 않은 리스트 반환

        // when
        ResultActions result = mockMvc.perform(get("/member/reminder/migration-status")
                        .header(AUTHORIZATION_HEADER, TOKEN_PREFIX + accessToken))
                .andExpect(status().isOk());

        // then
        result.andExpect(jsonPath("$.isMigrated").value(true));

        // docs
        result.andDo(document("알림 마이그레이션 상태 확인 - 마이그레이션 완료",
                "사용자가 스케줄링된 알림이 있는지 확인한다. 활성 알림이 있으면 마이그레이션 완료 상태이다.",
                "사용자의 알림 마이그레이션 상태를 반환한다.",
                false,
                false,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                        headerWithName(AUTHORIZATION_HEADER).description("발급받은 JWT")
                ),
                responseFields(
                        fieldWithPath("isMigrated").description("마이그레이션 완료 여부 (true: 활성 알림 있음, false: 활성 알림 없음)")
                )
        ));
    }

    @Test
    @DisplayName("활성 알림이 없는 경우 마이그레이션 상태가 false를 반환해야 한다.")
    void 활성_알림이_없는_경우_마이그레이션_상태가_false를_반환해야_한다() throws Exception {
        // given
        when(contactReminderRepository.findByMemberIdAndIsActiveTrue(testMember.getMemberId()))
                .thenReturn(Collections.emptyList());

        // when
        ResultActions result = mockMvc.perform(get("/member/reminder/migration-status")
                        .header(AUTHORIZATION_HEADER, TOKEN_PREFIX + accessToken))
                .andExpect(status().isOk());

        // then
        result.andExpect(jsonPath("$.isMigrated").value(false));

        // docs
        result.andDo(document("알림 마이그레이션 상태 확인 - 마이그레이션 미완료",
                "사용자가 스케줄링된 알림이 있는지 확인한다. 활성 알림이 없으면 마이그레이션 미완료 상태이다.",
                "사용자의 알림 마이그레이션 상태를 반환한다.",
                false,
                false,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                        headerWithName(AUTHORIZATION_HEADER).description("발급받은 JWT")
                ),
                responseFields(
                        fieldWithPath("isMigrated").description("마이그레이션 완료 여부 (true: 활성 알림 있음, false: 활성 알림 없음)")
                )
        ));
    }
}
