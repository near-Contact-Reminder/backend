package kr.swyp.backend.authentication.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import kr.swyp.backend.authentication.dto.AuthenticationDto.RefreshTokenAuthenticationRequest;
import kr.swyp.backend.authentication.dto.AuthenticationDto.UsernamePasswordAuthenticationRequest;
import kr.swyp.backend.authentication.provider.TokenProvider;
import kr.swyp.backend.authentication.repository.RefreshTokenRepository;
import kr.swyp.backend.authentication.service.RefreshTokenServiceImpl;
import kr.swyp.backend.common.desciptor.ErrorDescriptor;
import kr.swyp.backend.member.domain.Member;
import kr.swyp.backend.member.dto.MemberDetails;
import kr.swyp.backend.member.enums.RoleType;
import kr.swyp.backend.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@Transactional
class RefreshTokenAuthenticationTest {

    private final FieldDescriptor[] authenticationResponseDescriptor = {
            fieldWithPath("accessToken").description("엑세스 토큰"),
            fieldWithPath("refreshTokenInfo").description("갱신 토큰 정보"),
            fieldWithPath("refreshTokenInfo.token").description("갱신 토큰"),
            fieldWithPath("refreshTokenInfo.expiresAt").description("갱신 토큰 만료 시각"),
    };

    private final String usernamePasswordAuthenticationUrl = "/auth/login";
    private final String refreshTokenAuthenticationUrl = "/auth/renew";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenServiceImpl refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenProvider tokenProvider;

    @Test
    @DisplayName("올바른 갱신 토큰을 사용하여 토큰을 갱신할 수 있어야 한다.")
    void 올바른_갱신_토큰을_사용하여_토큰을_갱신할_수_있어야_한다() throws Exception {
        // given
        String username = "test@test.com";
        String nickname = "test";
        String password = "password1234";

        createMember(username, nickname, password);

        var authRequest = UsernamePasswordAuthenticationRequest.builder()
                .username(username)
                .password(password)
                .build();

        ResultActions result = this.mockMvc.perform(
                post(usernamePasswordAuthenticationUrl).content(
                                objectMapper.writeValueAsString(authRequest))
                        .contentType(MediaType.APPLICATION_JSON));

        MvcResult mvcResult = result.andExpect(status().isOk()).andReturn();
        JsonNode jsonNode = objectMapper.readTree(mvcResult.getResponse().getContentAsString());

        var renewRequest = RefreshTokenAuthenticationRequest.builder()
                .refreshToken(jsonNode.get("refreshTokenInfo").get("token").asText())
                .build();

        // when
        result = this.mockMvc.perform(
                post(refreshTokenAuthenticationUrl).content(
                                objectMapper.writeValueAsString(renewRequest))
                        .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshTokenInfo.token").isString())
                .andExpect(jsonPath("$.refreshTokenInfo.expiresAt").isString());

        JsonNode renewJsonNode = objectMapper.readTree(
                result.andReturn().getResponse().getContentAsString());
        String newAccessToken = renewJsonNode.get("accessToken").asText();
        Authentication authentication = tokenProvider.getAuthentication(newAccessToken);
        MemberDetails principal = (MemberDetails) authentication.getPrincipal();

        assertThat(principal.getMemberId()).isNotNull();
        assertThat(principal.getUsername()).isEqualTo(username);

        // docs
        result.andDo(document("갱신 토큰을 사용하여 새로운 토큰 발급",
                "사용자의 갱신 토큰을 사용하여 토큰을 갱신(재발급)한다.",
                "사용자의 토큰 갱신",
                false,
                false,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(
                        fieldWithPath("refreshToken").description("갱신 토큰")
                ),
                responseFields(authenticationResponseDescriptor)));
    }

    @Test
    @DisplayName("이미 사용한 갱신 토큰을 사용하여 갱신하려고 하면 오류가 발생해야 한다.")
    void 이미_사용한_갱신_토큰을_사용하여_갱신하려고_하면_오류가_발생해야_한다() throws Exception {
        // given
        String username = "test@test.com";
        String nickname = "test";
        String password = "password1234";

        createMember(username, nickname, password);

        // 회원가입 후 로그인하여 토큰 발급.
        var authRequest = UsernamePasswordAuthenticationRequest.builder()
                .username(username)
                .password(password)
                .build();

        ResultActions result = this.mockMvc.perform(
                post(usernamePasswordAuthenticationUrl).content(
                                objectMapper.writeValueAsString(authRequest))
                        .contentType(MediaType.APPLICATION_JSON));

        MvcResult mvcResult = result.andExpect(status().isOk()).andReturn();
        JsonNode jsonNode = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
        String refreshToken = jsonNode.get("refreshTokenInfo").get("token").asText();

        var renewRequest = RefreshTokenAuthenticationRequest.builder()
                .refreshToken(refreshToken)
                .build();

        ResultActions firstRenewResult = this.mockMvc.perform(
                post(refreshTokenAuthenticationUrl).content(
                                objectMapper.writeValueAsString(renewRequest))
                        .contentType(MediaType.APPLICATION_JSON));

        MvcResult firstRenewMvcResult = firstRenewResult.andReturn();

        // when
        ResultActions secondRenewResult = this.mockMvc.perform(
                post(refreshTokenAuthenticationUrl).content(
                                objectMapper.writeValueAsString(renewRequest))
                        .contentType(MediaType.APPLICATION_JSON));

        // then
        firstRenewResult
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshTokenInfo.token").isString());
        secondRenewResult
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").isString());

        // docs
        secondRenewResult.andDo(document("이미 사용한 갱신 토큰으로 토큰 발급 실패",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(
                        fieldWithPath("refreshToken").description("갱신 토큰")
                ),
                responseFields(ErrorDescriptor.errorResponseFieldDescriptors)));
    }

    @Test
    @DisplayName("존재하지 않는 갱신 토큰을 사용하여 갱신하려고 하면 오류가 발생해야 한다.")
    void 존재하지_않는_갱신_토큰을_사용하여_갱신하려고_하면_오류가_발생해야_한다() throws Exception {
        // given
        String username = "test@test.com";
        String nickname = "test";
        String password = "password1234";

        createMember(username, nickname, password);

        // 회원가입 후 로그인하여 토큰 발급.
        var authRequest = UsernamePasswordAuthenticationRequest.builder()
                .username(username)
                .password(password)
                .build();

        ResultActions result = this.mockMvc.perform(
                post(usernamePasswordAuthenticationUrl).content(
                                objectMapper.writeValueAsString(authRequest))
                        .contentType(MediaType.APPLICATION_JSON));

        MvcResult mvcResult = result.andExpect(status().isOk()).andReturn();
        JsonNode jsonNode = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
        String refreshToken = jsonNode.get("refreshTokenInfo").get("token").asText();

        var renewRequest = RefreshTokenAuthenticationRequest.builder()
                .refreshToken(refreshToken)
                .build();

        ResultActions firstRenewResult = this.mockMvc.perform(
                post(refreshTokenAuthenticationUrl).content(
                                objectMapper.writeValueAsString(renewRequest))
                        .contentType(MediaType.APPLICATION_JSON));

        MvcResult firstRenewMvcResult = firstRenewResult.andReturn();

        // when
        ResultActions secondRenewResult = this.mockMvc.perform(
                post(refreshTokenAuthenticationUrl).content(
                                objectMapper.writeValueAsString(renewRequest))
                        .contentType(MediaType.APPLICATION_JSON));

        // then
        firstRenewResult
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshTokenInfo.token").isString());
        secondRenewResult
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").isString());

        // docs
        secondRenewResult.andDo(document("이미 사용한 갱신 토큰으로 토큰 발급 실패",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(
                        fieldWithPath("refreshToken").description("갱신 토큰")
                ),
                responseFields(ErrorDescriptor.errorResponseFieldDescriptors)));
    }

    @Test
    @DisplayName("빈 갱신 토큰 문자열을 사용하여 갱신하려고 하면 오류가 발생해야 한다.")
    void 빈_갱신_토큰_문자열을_사용하여_갱신하려고_하면_오류가_발생해야_한다() throws Exception {
        // given
        String username = "test@test.com";
        String nickname = "test";
        String password = "password1234";

        createMember(username, nickname, password);

        var authRequest = UsernamePasswordAuthenticationRequest.builder()
                .username(username)
                .password(password)
                .build();

        this.mockMvc.perform(
                post(usernamePasswordAuthenticationUrl).content(
                                objectMapper.writeValueAsString(authRequest))
                        .contentType(MediaType.APPLICATION_JSON));

        var renewRequest = RefreshTokenAuthenticationRequest.builder()
                .refreshToken("")
                .build();
        // when
        ResultActions result = this.mockMvc.perform(
                post(refreshTokenAuthenticationUrl).content(
                                objectMapper.writeValueAsString(renewRequest))
                        .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("존재하지 않는 토큰입니다."));

        // docs
        result.andDo(document("올바르지 않은 갱신 토큰을 이용하여 토큰 발급 실패",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(
                        fieldWithPath("refreshToken").description("갱신 토큰")
                ),
                responseFields(ErrorDescriptor.errorResponseFieldDescriptors)));
    }

    private void createMember(String username, String nickname, String password) {
        String encodedPassword = passwordEncoder.encode(password);

        Member member = Member.builder()
                .username(username)
                .nickname(nickname)
                .password(encodedPassword)
                .isActive(true)
                .notificationAgreedAt(LocalDateTime.now())
                .build();

        member.addRole(RoleType.USER);

        memberRepository.save(member);
    }
}