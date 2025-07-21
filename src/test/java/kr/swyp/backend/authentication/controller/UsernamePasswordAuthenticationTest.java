package kr.swyp.backend.authentication.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
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
import java.time.LocalDateTime;
import kr.swyp.backend.authentication.dto.AuthenticationDto.UsernamePasswordAuthenticationRequest;
import kr.swyp.backend.member.domain.Member;
import kr.swyp.backend.member.enums.RoleType;
import kr.swyp.backend.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@Transactional
public class UsernamePasswordAuthenticationTest {

    private final FieldDescriptor[] authenticationResponseDescriptor = {
            fieldWithPath("accessToken").description("엑세스 토큰"),
            fieldWithPath("refreshTokenInfo").description("갱신 토큰 정보"),
            fieldWithPath("refreshTokenInfo.token").description("갱신 토큰"),
            fieldWithPath("refreshTokenInfo.expiresAt").description("갱신 토큰 만료 시각"),
    };

    private final String url = "/auth/login";

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("올바른 이메일와 비밀번호로 액세스 토큰이 발급 가능해야 한다.")
    void 올바른_이메일와_비밀번호로_액세스_토큰이_발급_가능해야_한다() throws Exception {
        // given
        String username = "test@test.com";
        String nickname = "test";
        String password = "password1234";

        this.createMember(username, nickname, password);

        var authRequest = UsernamePasswordAuthenticationRequest.builder()
                .username(username)
                .password(password)
                .build();

        // when
        ResultActions result = this.mockMvc.perform(
                post(url).content(
                                this.objectMapper.writeValueAsString(authRequest))
                        .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();

        // docs
        result.andDo(document("회원 엑세스 토큰 발급 성공",
                "사용자의 ID와 비밀번호로 토큰을 발급한다.",
                "사용자의 ID와 비밀번호로 토큰 발급",
                false,
                false,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(
                        fieldWithPath("username").description("사용자 아이디"),
                        fieldWithPath("password").description("비밀번호")
                ),
                responseFields(authenticationResponseDescriptor)));
    }

    @Test
    @DisplayName("올버르지 않은 이메일로 로그인이 불가하여야 한다.")
    void 올버르지_않은_이메일로_로그인이_불가하여야_한다() throws Exception {
        // given
        String username = "test";
        String password = "test1234";

        var authRequest = UsernamePasswordAuthenticationRequest.builder()
                .username(username)
                .password(password)
                .build();

        // when
        ResultActions result = this.mockMvc.perform(
                post(url)
                        .content(objectMapper.writeValueAsString(authRequest))
                        .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().is(HttpStatus.UNAUTHORIZED.value()))
                .andExpect(jsonPath("$.message").value("이메일 형식이 올바르지 않습니다."))
                .andExpect(jsonPath("$.code").value("CREDENTIAL_NOT_FOUND"))
                .andReturn();

        // docs
        result.andDo(document("올바르지 않은 아이디로 엑세스 토큰 발급 실패",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(
                        fieldWithPath("username").description("사용자 아이디"),
                        fieldWithPath("password").description("비밀번호")
                )));
    }

    @Test
    @DisplayName("올바르지 않은 비밀번호로 로그인이 불가하여야 한다.")
    void 올바르지_않은_비밀번호로_로그인이_불가하여야_한다() throws Exception {
        // given
        String username = "test@test.com";
        String nickname = "test";
        String password = "password";

        this.createMember(username, nickname, password);

        var authRequest = UsernamePasswordAuthenticationRequest.builder()
                .username(username)
                .password("test1234")
                .build();

        // when
        ResultActions result = this.mockMvc.perform(post(url)
                .content(this.objectMapper.writeValueAsString(authRequest))
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().is(HttpStatus.UNAUTHORIZED.value()))
                .andExpect(jsonPath("$.message").value("아이디와 비밀번호가 올바른지 확인해 주세요."))
                .andExpect(jsonPath("$.code").value("CREDENTIAL_NOT_FOUND"))
                .andReturn();

        // docs
        result.andDo(document("올바르지 않은 패스워드로 액세스 토큰 발급 실패",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(
                        fieldWithPath("username").description("사용자 아이디"),
                        fieldWithPath("password").description("비밀번호")
                )));
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