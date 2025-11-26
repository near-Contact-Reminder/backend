package kr.swyp.backend.term.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import kr.swyp.backend.authentication.provider.TokenProvider;
import kr.swyp.backend.common.desciptor.ErrorDescriptor;
import kr.swyp.backend.member.domain.Member;
import kr.swyp.backend.member.domain.MemberTermsAgreement;
import kr.swyp.backend.member.dto.MemberDetails;
import kr.swyp.backend.member.enums.RoleType;
import kr.swyp.backend.member.repository.MemberRepository;
import kr.swyp.backend.member.repository.MemberTermsAgreementRepository;
import kr.swyp.backend.term.domain.Term;
import kr.swyp.backend.term.dto.TermDto.TermAgreementRequest;
import kr.swyp.backend.term.dto.TermDto.TermsAgreementRequest;
import kr.swyp.backend.term.repository.TermRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
class TermControllerTest {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    private final FieldDescriptor[] termResponseDescriptor = {
            fieldWithPath("[].termId").description("약관 ID"),
            fieldWithPath("[].title").description("약관 제목"),
            fieldWithPath("[].version").description("약관 버전"),
            fieldWithPath("[].isRequired").description("필수 여부")
    };

    private final FieldDescriptor[] termsAgreementResponseDescriptor = {
            fieldWithPath("agreements[].termId").description("약관 ID"),
            fieldWithPath("agreements[].title").description("약관 제목"),
            fieldWithPath("agreements[].version").description("약관 버전"),
            fieldWithPath("agreements[].isRequired").description("필수 여부"),
            fieldWithPath("agreements[].isAgreed").description("동의 여부"),
            fieldWithPath("agreements[].agreedAt").description("동의 시각").optional()
    };

    private final FieldDescriptor[] termsAgreementRequestDescriptor = {
            fieldWithPath("agreements[].termId").description("약관 ID"),
            fieldWithPath("agreements[].isAgreed").description("동의 여부")
    };

    private final String url = "/terms";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TermRepository termRepository;

    @Autowired
    private MemberTermsAgreementRepository memberTermsAgreementRepository;

    private Member testMember;
    private String accessToken;
    private Term serviceTerms;
    private Term privacyTerms;
    private Term privacyPolicyTerms;

    @BeforeEach
    void setUp() {
        // 테스트용 회원 생성
        testMember = memberRepository.save(
                Member.builder()
                        .username("testuser@example.com")
                        .password("encoded_password")
                        .nickname("테스트유저")
                        .isActive(true)
                        .notificationAgreedAt(LocalDateTime.now())
                        .build()
        );

        // 역할 추가
        testMember.addRole(RoleType.USER);
        testMember = memberRepository.save(testMember);

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

        // 테스트용 약관 생성
        serviceTerms = termRepository.save(Term.builder()
                .title("서비스 이용 약관")
                .version("1.0")
                .isRequired(true)
                .build());

        privacyTerms = termRepository.save(Term.builder()
                .title("개인정보 수집 및 이용 동의서")
                .version("1.0")
                .isRequired(true)
                .build());

        privacyPolicyTerms = termRepository.save(Term.builder()
                .title("개인정보 처리방침")
                .version("1.0")
                .isRequired(false)
                .build());
    }

    @Test
    @DisplayName("모든 약관 목록을 조회할 수 있어야 한다")
    void 모든_약관_목록을_조회할_수_있어야_한다() throws Exception {
        // when
        ResultActions result = mockMvc.perform(get(url))
                .andExpect(status().isOk());

        // then
        result.andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$[0].title").exists())
                .andExpect(jsonPath("$[0].version").exists())
                .andExpect(jsonPath("$[0].isRequired").exists());

        // docs
        result.andDo(document("약관 목록 조회",
                "모든 약관의 목록을 조회한다.",
                "약관 목록 조회",
                false,
                false,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                responseFields(termResponseDescriptor)
        ));
    }

    @Test
    @DisplayName("내 약관 동의 상태를 조회할 수 있어야 한다")
    void 내_약관_동의_상태를_조회할_수_있어야_한다() throws Exception {
        // given - 일부 약관에만 동의한 상태
        memberTermsAgreementRepository.save(MemberTermsAgreement.builder()
                .memberId(testMember.getMemberId())
                .termsId(serviceTerms.getId())
                .isAgreed(true)
                .build());

        // when
        ResultActions result = mockMvc.perform(get(url + "/me")
                        .header(AUTHORIZATION_HEADER, TOKEN_PREFIX + accessToken))
                .andExpect(status().isOk());

        // then
        result.andExpect(jsonPath("$.agreements").isArray())
                .andExpect(jsonPath("$.agreements.length()").value(3))
                .andExpect(jsonPath("$.agreements[?(@.title == '서비스 이용 약관')].isAgreed").value(true))
                .andExpect(jsonPath("$.agreements[?(@.title == '개인정보 수집 및 이용 동의서')].isAgreed")
                        .value(false));

        // docs
        result.andDo(document("내 약관 동의 상태 조회",
                "현재 로그인한 회원의 약관 동의 상태를 조회한다. 모든 약관에 대해 동의 여부와 동의 시각을 포함한다.",
                "내 약관 동의 상태 조회",
                false,
                false,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                        headerWithName(AUTHORIZATION_HEADER).description("발급받은 JWT")),
                responseFields(termsAgreementResponseDescriptor)
        ));
    }

    @Test
    @DisplayName("약관 동의를 저장할 수 있어야 한다")
    void 약관_동의를_저장할_수_있어야_한다() throws Exception {
        // given
        TermsAgreementRequest request = TermsAgreementRequest.builder()
                .agreements(List.of(
                        TermAgreementRequest.builder()
                                .termId(serviceTerms.getId())
                                .isAgreed(true)
                                .build(),
                        TermAgreementRequest.builder()
                                .termId(privacyTerms.getId())
                                .isAgreed(true)
                                .build(),
                        TermAgreementRequest.builder()
                                .termId(privacyPolicyTerms.getId())
                                .isAgreed(true)
                                .build()
                ))
                .build();

        // when
        ResultActions result = mockMvc.perform(post(url + "/me")
                        .header(AUTHORIZATION_HEADER, TOKEN_PREFIX + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // then
        List<MemberTermsAgreement> agreements = memberTermsAgreementRepository.findAllByMemberId(
                testMember.getMemberId());
        org.assertj.core.api.Assertions.assertThat(agreements).hasSize(3);
        org.assertj.core.api.Assertions.assertThat(agreements)
                .allMatch(MemberTermsAgreement::getIsAgreed);

        // docs
        result.andDo(document("약관 동의 저장",
                "회원의 약관 동의를 저장하거나 업데이트한다. 필수 약관에 미동의 시 400 에러가 발생한다.",
                "약관 동의 저장",
                false,
                false,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                        headerWithName(AUTHORIZATION_HEADER).description("발급받은 JWT")),
                requestFields(termsAgreementRequestDescriptor)
        ));
    }

    @Test
    @DisplayName("필수 약관에 미동의 시 400 에러가 발생해야 한다")
    void 필수_약관에_미동의_시_400_에러가_발생해야_한다() throws Exception {
        // given
        TermsAgreementRequest request = TermsAgreementRequest.builder()
                .agreements(List.of(
                        TermAgreementRequest.builder()
                                .termId(serviceTerms.getId())
                                .isAgreed(false) // 필수 약관인데 미동의
                                .build()
                ))
                .build();

        // when
        ResultActions result = mockMvc.perform(post(url + "/me")
                        .header(AUTHORIZATION_HEADER, TOKEN_PREFIX + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // then
        result.andExpect(jsonPath("$.message").value("필수 약관에 동의해야 합니다: 서비스 이용 약관"));

        // docs
        result.andDo(document("약관 동의 실패 - 필수 약관 미동의",
                "필수 약관에 동의하지 않으면 400 에러가 발생한다.",
                "필수 약관 미동의",
                false,
                false,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                        headerWithName(AUTHORIZATION_HEADER).description("발급받은 JWT")),
                requestFields(termsAgreementRequestDescriptor),
                responseFields(ErrorDescriptor.errorResponseFieldDescriptors)
        ));
    }

    @Test
    @DisplayName("존재하지 않는 약관에 동의 시 404 에러가 발생해야 한다")
    void 존재하지_않는_약관에_동의_시_404_에러가_발생해야_한다() throws Exception {
        // given
        TermsAgreementRequest request = TermsAgreementRequest.builder()
                .agreements(List.of(
                        TermAgreementRequest.builder()
                                .termId(999L) // 존재하지 않는 약관 ID
                                .isAgreed(true)
                                .build()
                ))
                .build();

        // when
        ResultActions result = mockMvc.perform(post(url + "/me")
                        .header(AUTHORIZATION_HEADER, TOKEN_PREFIX + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        // then
        result.andExpect(jsonPath("$.message").value("약관을 찾을 수 없습니다. ID: 999"));

        // docs
        result.andDo(document("약관 동의 실패 - 존재하지 않는 약관",
                "존재하지 않는 약관 ID로 동의 시도 시 404 에러가 발생한다.",
                "존재하지 않는 약관",
                false,
                false,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                        headerWithName(AUTHORIZATION_HEADER).description("발급받은 JWT")),
                requestFields(termsAgreementRequestDescriptor),
                responseFields(ErrorDescriptor.errorResponseFieldDescriptors)
        ));
    }

    @Test
    @DisplayName("약관 동의를 업데이트할 수 있어야 한다")
    void 약관_동의를_업데이트할_수_있어야_한다() throws Exception {
        // given - 초기 동의 저장
        memberTermsAgreementRepository.save(MemberTermsAgreement.builder()
                .memberId(testMember.getMemberId())
                .termsId(serviceTerms.getId())
                .isAgreed(true)
                .build());

        // 선택 약관에 새로 동의
        TermsAgreementRequest request = TermsAgreementRequest.builder()
                .agreements(List.of(
                        TermAgreementRequest.builder()
                                .termId(serviceTerms.getId())
                                .isAgreed(true)
                                .build(),
                        TermAgreementRequest.builder()
                                .termId(privacyTerms.getId())
                                .isAgreed(true)
                                .build(),
                        TermAgreementRequest.builder()
                                .termId(privacyPolicyTerms.getId())
                                .isAgreed(true) // 새로 동의
                                .build()
                ))
                .build();

        // when
        ResultActions result = mockMvc.perform(post(url + "/me")
                        .header(AUTHORIZATION_HEADER, TOKEN_PREFIX + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // then
        List<MemberTermsAgreement> agreements = memberTermsAgreementRepository.findAllByMemberId(
                testMember.getMemberId());
        org.assertj.core.api.Assertions.assertThat(agreements).hasSize(3);

        // docs
        result.andDo(document("약관 동의 업데이트",
                "이미 동의한 약관을 포함하여 약관 동의를 업데이트한다.",
                "약관 동의 업데이트",
                false,
                false,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                        headerWithName(AUTHORIZATION_HEADER).description("발급받은 JWT")),
                requestFields(termsAgreementRequestDescriptor)
        ));
    }

    @Test
    @DisplayName("빈 약관 동의 목록으로 요청 시 400 에러가 발생해야 한다")
    void 빈_약관_동의_목록으로_요청_시_400_에러가_발생해야_한다() throws Exception {
        // given
        TermsAgreementRequest request = TermsAgreementRequest.builder()
                .agreements(List.of()) // 빈 목록
                .build();

        // when
        ResultActions result = mockMvc.perform(post(url + "/me")
                        .header(AUTHORIZATION_HEADER, TOKEN_PREFIX + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // then
        result.andExpect(jsonPath("$.message").exists());

        // docs
        result.andDo(document("약관 동의 실패 - 빈 목록",
                "빈 약관 동의 목록으로 요청 시 400 에러가 발생한다.",
                "빈 약관 동의 목록",
                false,
                false,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                        headerWithName(AUTHORIZATION_HEADER).description("발급받은 JWT")),
                requestFields(
                        fieldWithPath("agreements").description("약관 동의 목록 (빈 배열)")
                ),
                responseFields(ErrorDescriptor.errorResponseFieldDescriptors)
        ));
    }
}
