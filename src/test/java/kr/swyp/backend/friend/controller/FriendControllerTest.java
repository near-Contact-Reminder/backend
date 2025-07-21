package kr.swyp.backend.friend.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.swyp.backend.authentication.provider.TokenProvider;
import kr.swyp.backend.common.domain.File;
import kr.swyp.backend.common.dto.FileDto.FileUploadRequest;
import kr.swyp.backend.common.repository.FileRepository;
import kr.swyp.backend.friend.domain.Friend;
import kr.swyp.backend.friend.domain.FriendAnniversary;
import kr.swyp.backend.friend.domain.FriendCheckingLog;
import kr.swyp.backend.friend.domain.FriendContactFrequency;
import kr.swyp.backend.friend.domain.FriendDetail;
import kr.swyp.backend.friend.dto.FriendDto.FriendCreateListRequest;
import kr.swyp.backend.friend.dto.FriendDto.FriendCreateListRequest.FriendRequest;
import kr.swyp.backend.friend.dto.FriendDto.FriendCreateListRequest.FriendRequest.FriendAnniversaryCreateRequest;
import kr.swyp.backend.friend.dto.FriendDto.FriendDetailUpdateRequest;
import kr.swyp.backend.friend.dto.FriendDto.FriendDetailUpdateRequest.FriendAnniversaryDetailUpdateRequest;
import kr.swyp.backend.friend.enums.FriendContactWeek;
import kr.swyp.backend.friend.enums.FriendRelation;
import kr.swyp.backend.friend.enums.FriendSource;
import kr.swyp.backend.friend.repository.FriendAnniversaryRepository;
import kr.swyp.backend.friend.repository.FriendCheckingLogRepository;
import kr.swyp.backend.friend.repository.FriendDetailRepository;
import kr.swyp.backend.friend.repository.FriendRepository;
import kr.swyp.backend.member.domain.Member;
import kr.swyp.backend.member.domain.MemberSocialLoginInfo;
import kr.swyp.backend.member.dto.MemberDetails;
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
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
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
class FriendControllerTest {

    private final FieldDescriptor[] friendInitRequestDescriptor = {
            fieldWithPath("friendList[].name").description("친구 이름"),
            fieldWithPath("friendList[].source").description("연락처 출처"),
            fieldWithPath("friendList[].contactFrequency.contactWeek").description("연락 주기"),
            fieldWithPath("friendList[].contactFrequency.dayOfWeek").description("연락 요일"),
            fieldWithPath("friendList[].imageUploadRequest").description("파일 업로드 요청"),
            fieldWithPath("friendList[].imageUploadRequest.fileName").description("파일 이름"),
            fieldWithPath("friendList[].imageUploadRequest.contentType").description("파일 타입"),
            fieldWithPath("friendList[].imageUploadRequest.fileSize").description("파일 크기"),
            fieldWithPath("friendList[].imageUploadRequest.category").description("파일 카테고리"),
            fieldWithPath("friendList[].anniversary").description("기념일"),
            fieldWithPath("friendList[].anniversary.title").description("기념일 제목"),
            fieldWithPath("friendList[].anniversary.date").description("기념일 날짜"),
            fieldWithPath("friendList[].phone").description("전화번호"),
            fieldWithPath("friendList[].birthDay").description("생일"),
            fieldWithPath("friendList[].relation").description("관계"),
            fieldWithPath("friendList[].memo").description("메모"),
    };

    private final FieldDescriptor[] friendInitResponseDescriptor = {
            fieldWithPath("friendList[].friendId").description("친구 ID"),
            fieldWithPath("friendList[].name").description("친구 이름"),
            fieldWithPath("friendList[].source").description("연락처 출처"),
            fieldWithPath("friendList[].contactFrequency").description("연락 주기"),
            fieldWithPath("friendList[].contactFrequency.contactWeek").description("연락 주기"),
            fieldWithPath("friendList[].contactFrequency.dayOfWeek").description("연락 요일"),
            fieldWithPath("friendList[].anniversary.id").description("기념일 id"),
            fieldWithPath("friendList[].anniversary.title").description("기념일 제목"),
            fieldWithPath("friendList[].anniversary.date").description("기념일 날짜"),
            fieldWithPath("friendList[].nextContactAt").description("다음 연락 날짜"),
            fieldWithPath("friendList[].preSignedImageUrl").description("프리사인드 이미지 URL"),
            fieldWithPath("friendList[].fileName").description("파일 이름"),
            fieldWithPath("friendList[].phone").description("전화번호"),
            fieldWithPath("friendList[].memo").description("메모"),
    };

    private final FieldDescriptor[] friendListResponseDescriptor = new FieldDescriptor[]{
            fieldWithPath("[].friendId").description("친구 ID"),
            fieldWithPath("[].position").description("친구 노출 순서"),
            fieldWithPath("[].source").description("친구 출처"),
            fieldWithPath("[].name").description("친구 이름"),
            fieldWithPath("[].imageUrl").description("친구 프로필 이미지 URL"),
            fieldWithPath("[].fileName").description("친구 프로필 이미지 파일 이름"),
            fieldWithPath("[].lastContactAt").description("마지막 연락 날짜"),
    };

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String AUTHORIZATION_VALUE_PREFIX = "Bearer ";

    private final String url = "/friend";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FriendRepository friendRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberSocialLoginInfoRepository socialLoginInfoRepository;

    @Autowired
    private FriendCheckingLogRepository friendCheckingLogRepository;

    @Autowired
    private FriendDetailRepository friendDetailRepository;

    @Autowired
    private FileRepository fileRepository;

    private Member testMember;
    private Friend testFriend;

    @Autowired
    private FriendAnniversaryRepository friendAnniversaryRepository;

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

        // 친구 설정
        testFriend = friendRepository.save(
                Friend.builder()
                        .name("테스트친구")
                        .friendSource(FriendSource.KAKAO)
                        .contactFrequency(FriendContactFrequency.builder()
                                .contactWeek(FriendContactWeek.EVERY_WEEK)
                                .dayOfWeek(DayOfWeek.MONDAY)
                                .build())
                        .alarmTriggerCount(0)
                        .position(0)
                        .nextContactAt(LocalDate.now().plusDays(7))
                        .memberId(testMember.getMemberId())
                        .build()
        );

        FriendDetail friendDetail = FriendDetail.builder()
                .friend(testFriend)
                .relation(FriendRelation.FRIEND)
                .birthday(LocalDate.now().minusYears(30))
                .memo("테스트 메모")
                .build();

        testFriend.addFriendDetail(friendDetail);
    }

    @Test
    @DisplayName("친구를 추가할 수 있어야 한다.")
    void 친구를_추가할_수_있어야_한다() throws Exception {
        // given
        FriendCreateListRequest friendCreateListRequest =
                FriendCreateListRequest.builder()
                        .friendList(List.of(FriendRequest.builder()
                                        .name("test")
                                        .source(FriendSource.KAKAO)
                                        .relation(FriendRelation.FRIEND)
                                        .birthDay(LocalDate.now())
                                        .contactFrequency(FriendContactFrequency.builder()
                                                .contactWeek(FriendContactWeek.EVERY_WEEK)
                                                .dayOfWeek(DayOfWeek.MONDAY)
                                                .build())
                                        .imageUploadRequest(FileUploadRequest.builder()
                                                .fileName("test.jpg")
                                                .contentType("image/jpeg")
                                                .fileSize(1024L)
                                                .category("test")
                                                .build())
                                        .anniversary(FriendAnniversaryCreateRequest.builder()
                                                .title("test")
                                                .date(LocalDate.now())
                                                .build())
                                        .phone("01000000000")
                                        .memo("test")
                                        .build(),
                                FriendRequest.builder()
                                        .name("test")
                                        .source(FriendSource.APPLE)
                                        .relation(FriendRelation.FRIEND)
                                        .birthDay(LocalDate.now())
                                        .contactFrequency(FriendContactFrequency.builder()
                                                .contactWeek(FriendContactWeek.EVERY_WEEK)
                                                .dayOfWeek(DayOfWeek.MONDAY)
                                                .build())
                                        .imageUploadRequest(FileUploadRequest.builder()
                                                .fileName("test.jpg")
                                                .contentType("image/jpeg")
                                                .fileSize(1024L)
                                                .category("test")
                                                .build())
                                        .anniversary(FriendAnniversaryCreateRequest.builder()
                                                .title("test")
                                                .date(LocalDate.now())
                                                .build())
                                        .phone("01000000000")
                                        .memo("test")
                                        .build()
                        )).build();

        // when
        ResultActions result = mockMvc.perform(post(url + "/init")
                .header(AUTHORIZATION_HEADER,
                        AUTHORIZATION_VALUE_PREFIX + createAccessToken(testMember.getMemberId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(friendCreateListRequest)));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.friendList").isNotEmpty())
                .andExpect(jsonPath("$.friendList[0].friendId").isNotEmpty())
                .andExpect(jsonPath("$.friendList[1].friendId").isNotEmpty());

        // docs
        result.andDo(document("친구 초기 설정",
                "친구를 초기 설정한다.",
                "친구 초기 설정",
                false,
                false,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                        headerWithName(AUTHORIZATION_HEADER).description("발급받은 JWT")),
                requestFields(friendInitRequestDescriptor),
                responseFields(friendInitResponseDescriptor)
        ));
    }

    @Test
    @DisplayName("챙기기 버튼 반영을 성공적으로 처리해야 한다.")
    void 챙기기_버튼_반영을_성공적으로_처리해야_한다() throws Exception {
        // given
        UUID memberId = testMember.getMemberId();
        UUID friendId = testFriend.getFriendId();
        String accessToken = createAccessToken(memberId);

        // when
        ResultActions result = mockMvc.perform(post("/friend/record/{friendId}", friendId)
                .header(AUTHORIZATION_HEADER, AUTHORIZATION_VALUE_PREFIX + accessToken)
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("챙기기 버튼 반영이 완료되었습니다."));

        // docs
        result.andDo(document("친구 챙기기 버튼 반영",
                "사용자가 친구를 챙기기로 표시하면 해당 친구의 체크 로그를 기록하고 체크율을 업데이트한다.",
                "챙기기 버튼 클릭 시 성공 메시지를 응답한다.",
                false,
                false,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                        headerWithName(AUTHORIZATION_HEADER).description("발급받은 JWT")
                ),
                pathParameters(
                        parameterWithName("friendId").description("챙기기를 반영할 친구의 UUID")
                ),
                responseFields(
                        fieldWithPath("message").description("성공 시 응답 메시지")
                )
        ));
    }

    @Test
    @DisplayName("친구 챙김 로그를 성공적으로 조회해야 한다.")
    void 친구_챙김_로그를_성공적으로_조회해야_한다() throws Exception {
        // given
        UUID memberId = testMember.getMemberId();
        UUID friendId = testFriend.getFriendId();
        String accessToken = createAccessToken(memberId);

        // 사전 데이터 삽입
        friendCheckingLogRepository.save(FriendCheckingLog.of(testFriend, true));
        friendCheckingLogRepository.save(FriendCheckingLog.of(testFriend, false));

        // when
        ResultActions result = mockMvc.perform(get("/friend/record/{friendId}", friendId)
                .header(AUTHORIZATION_HEADER, AUTHORIZATION_VALUE_PREFIX + accessToken)
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // docs
        result.andDo(document("친구 챙김 로그 조회",
                "사용자가 친구의 챙김 기록을 조회한다.",
                "특정 친구의 챙김 로그 리스트를 반환한다.",
                false,
                false,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                        headerWithName(AUTHORIZATION_HEADER).description("발급받은 JWT")
                ),
                pathParameters(
                        parameterWithName("friendId").description("챙김 기록을 조회할 친구의 UUID")
                ),
                responseFields(
                        fieldWithPath("[].createdAt").description("챙김한 날짜"),
                        fieldWithPath("[].isChecked").description("챙김 여부")
                )
        ));
    }

    @Test
    @DisplayName("트리거된 알람 개수 반영을 성공적으로 처리해야 한다.")
    void 트리거된_알람_개수_반영을_성공적으로_처리해야_한다() throws Exception {
        // given
        UUID memberId = testMember.getMemberId();
        UUID friendId = testFriend.getFriendId();
        String accessToken = createAccessToken(memberId);

        // when
        ResultActions result = mockMvc.perform(post("/friend/reminder/{friendId}", friendId)
                .header(AUTHORIZATION_HEADER, AUTHORIZATION_VALUE_PREFIX + accessToken)
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("트리거된 알람 개수 반영이 완료되었습니다."));

        // docs
        result.andDo(document("친구 알람 트리거 반영",
                "사용자가 친구 알람을 확인했을 때 알람 트리거 횟수를 업데이트한다.",
                "알람 확인 요청 시 성공 메시지를 반환한다.",
                false,
                false,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                        headerWithName(AUTHORIZATION_HEADER).description("발급받은 JWT")
                ),
                pathParameters(
                        parameterWithName("friendId").description("알람 트리거를 반영할 친구의 UUID")
                ),
                responseFields(
                        fieldWithPath("message").description("성공 시 응답 메시지")
                )
        ));
    }

    @Test
    @DisplayName("친구 목록을 성공적으로 조회해야 한다.")
    void 친구_목록을_성공적으로_조회해야_한다() throws Exception {
        // given
        UUID memberId = testMember.getMemberId();
        String accessToken = createAccessToken(memberId);

        // File 저장
        File imageFile = fileRepository.save(File.builder()
                .fileName("img")
                .contentType("image/jpeg")
                .fileSize(1024L)
                .category("profile")
                .build());

        // Friend 저장
        Friend friend = friendRepository.save(Friend.builder()
                .memberId(memberId)
                .position(1)
                .name("friend1")
                .friendSource(FriendSource.KAKAO)
                .contactFrequency(FriendContactFrequency.builder()
                        .contactWeek(FriendContactWeek.EVERY_WEEK)
                        .dayOfWeek(DayOfWeek.MONDAY)
                        .build())
                .nextContactAt(LocalDate.now().plusDays(7))
                .build());

        // FriendDetail 저장
        friendDetailRepository.save(FriendDetail.builder()
                .friend(friend)
                .relation(FriendRelation.FRIEND)
                .birthday(LocalDate.now())
                .imageFileId(imageFile.getId())
                .build());

        // when
        ResultActions result = mockMvc.perform(get("/friend/list")
                .header(AUTHORIZATION_HEADER, AUTHORIZATION_VALUE_PREFIX + accessToken)
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].friendId").exists())
                .andExpect(jsonPath("$[0].source").value("KAKAO"))
                .andExpect(jsonPath("$[0].name").value("테스트친구"));

        // docs
        result.andDo(document("친구 목록 조회",
                "등록된 친구 목록을 조회한다.",
                "사용자의 챙길 친구 리스트를 조회해 각 친구의 정보를 반환한다.",
                false,
                false,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                        headerWithName(AUTHORIZATION_HEADER).description("발급받은 JWT")
                ),
                responseFields(friendListResponseDescriptor)
        ));
    }

    @Test
    @DisplayName("친구 포지션을 성공적으로 변경해야 한다.")
    void 친구_포지션을_성공적으로_변경해야_한다() throws Exception {
        // given
        UUID memberId = testMember.getMemberId();
        UUID friendId = testFriend.getFriendId();
        String accessToken = createAccessToken(memberId);

        int newPosition = 3;

        Map<String, Integer> requestBody = Map.of("newPosition", newPosition);

        // when
        ResultActions result = mockMvc.perform(patch("/friend/list/{id}", friendId)
                .header(AUTHORIZATION_HEADER, AUTHORIZATION_VALUE_PREFIX + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("포지션 변경이 완료되었습니다."));

        // docs
        result.andDo(document("친구 포지션 변경",

                "특정 친구의 포지션을 변경한다.",
                "친구 리스트에서 노출 순서를 사용자 요청에 따라 업데이트한다.",
                false,
                false,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                        headerWithName(AUTHORIZATION_HEADER).description("발급받은 JWT")
                ),
                pathParameters(
                        parameterWithName("id").description("포지션을 변경할 친구의 ID")
                ),
                requestFields(
                        fieldWithPath("newPosition").description("변경할 포지션 값")
                ),
                responseFields(
                        fieldWithPath("message").description("성공 시 응답 메시지")
                )
        ));
    }

    @Test
    @DisplayName("친구를 상세조회 할 수 있어야 한다.")
    void 친구를_상세조회_할_수_있어야_한다() throws Exception {
        // given
        UUID memberId = testMember.getMemberId();
        String accessToken = createAccessToken(memberId);

        // File 저장
        File imageFile = fileRepository.save(File.builder()
                .fileName("img")
                .contentType("image/jpeg")
                .fileSize(1024L)
                .category("profile")
                .build());

        // Friend 저장
        Friend friend = friendRepository.save(Friend.builder()
                .memberId(memberId)
                .position(1)
                .name("friend1")
                .friendSource(FriendSource.KAKAO)
                .contactFrequency(FriendContactFrequency.builder()
                        .contactWeek(FriendContactWeek.EVERY_WEEK)
                        .dayOfWeek(DayOfWeek.MONDAY)
                        .build())
                .nextContactAt(LocalDate.now().plusDays(7))
                .build());

        // FriendDetail 저장
        FriendDetail friendDetail = friendDetailRepository.save(FriendDetail.builder()
                .friend(friend)
                .relation(FriendRelation.FRIEND)
                .imageFileId(imageFile.getId())
                .build());

        friend.addFriendDetail(friendDetail);

        // FriendAnniversary 저장
        friendAnniversaryRepository.save(
                FriendAnniversary.builder()
                        .friendId(friend.getFriendId())
                        .date(LocalDate.now())
                        .title("test")
                        .build());

        // when
        ResultActions result = mockMvc.perform(get("/friend/{friendId}", friend.getFriendId())
                .header(AUTHORIZATION_HEADER, AUTHORIZATION_VALUE_PREFIX + accessToken)
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.friendId").value(friend.getFriendId().toString()))
                .andExpect(jsonPath("$.name").value("friend1"));

        // docs
        result.andDo(document("친구 상세 조회",
                "특정 친구의 상세 정보를 조회한다.",
                "친구 상세 정보 조회",
                false,
                false,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                        headerWithName(AUTHORIZATION_HEADER).description("발급받은 JWT")
                ),
                pathParameters(
                        parameterWithName("friendId").description("상세 조회할 친구의 UUID")
                ),
                responseFields(
                        fieldWithPath("friendId").description("친구 ID"),
                        fieldWithPath("name").description("친구 이름"),
                        fieldWithPath("contactFrequency.contactWeek").description("연락 주기"),
                        fieldWithPath("contactFrequency.dayOfWeek").description("연락 요일"),
                        fieldWithPath("imageUrl").description("친구 프로필 이미지 URL"),
                        fieldWithPath("relation").description("친구와의 관계"),
                        fieldWithPath("birthday").description("친구의 생일").optional(),
                        fieldWithPath("anniversaryList").description("기념일 목록"),
                        fieldWithPath("anniversaryList[].id").description("기념일 id"),
                        fieldWithPath("anniversaryList[].title").description("기념일 제목"),
                        fieldWithPath("anniversaryList[].date").description("기념일 날짜"),
                        fieldWithPath("memo").description("친구에 대한 메모"),
                        fieldWithPath("phone").description("친구의 전화번호"),
                        fieldWithPath("lastContactAt").description("마지막 연락 날짜")
                )));
    }

    @Test
    @DisplayName("친구를 업데이트 할 수 있어야 한다.")
    void 친구를_업데이트_할_수_있어야_한다() throws Exception {
        // given
        UUID memberId = testMember.getMemberId();
        String accessToken = createAccessToken(memberId);

        // File 저장
        File imageFile = fileRepository.save(File.builder()
                .fileName("img")
                .contentType("image/jpeg")
                .fileSize(1024L)
                .category("profile")
                .build());

        // Friend 저장
        Friend friend = friendRepository.save(Friend.builder()
                .memberId(memberId)
                .position(1)
                .name("friend1")
                .friendSource(FriendSource.KAKAO)
                .contactFrequency(FriendContactFrequency.builder()
                        .contactWeek(FriendContactWeek.EVERY_WEEK)
                        .dayOfWeek(DayOfWeek.MONDAY)
                        .build())
                .nextContactAt(LocalDate.now().plusDays(7))
                .build());

        // FriendDetail 저장
        FriendDetail friendDetail = friendDetailRepository.save(FriendDetail.builder()
                .friend(friend)
                .relation(FriendRelation.FRIEND)
                .phone("01000000000")
                .imageFileId(imageFile.getId())
                .build());

        friend.addFriendDetail(friendDetail);

        // FriendAnniversary 저장
        FriendAnniversary friendAnniversary = friendAnniversaryRepository.save(
                FriendAnniversary.builder()
                        .friendId(friend.getFriendId())
                        .date(LocalDate.now())
                        .title("test")
                        .build());

        FriendDetailUpdateRequest request = FriendDetailUpdateRequest.builder()
                .name("friend2")
                .relation(FriendRelation.FRIEND)
                .contactFrequency(FriendContactFrequency.builder()
                        .contactWeek(FriendContactWeek.EVERY_DAY)
                        .dayOfWeek(DayOfWeek.MONDAY)
                        .build())
                .birthday(LocalDate.of(1997, 11, 19))
                .anniversaryList(List.of(FriendAnniversaryDetailUpdateRequest.builder()
                        .id(friendAnniversary.getId())
                        .title("test2")
                        .date(LocalDate.of(1997, 11, 19))
                        .build()))
                .memo("test memo")
                .phone("01011111111")
                .build();

        // when
        ResultActions result = mockMvc.perform(put("/friend/{friendId}", friend.getFriendId())
                .header(AUTHORIZATION_HEADER, AUTHORIZATION_VALUE_PREFIX + accessToken)
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.friendId").value(friend.getFriendId().toString()))
                .andExpect(jsonPath("$.name").value("friend2"));

        // docs
        result.andDo(document("친구 업데이트",
                "특정 상세 정보를 업데이트 한다.",
                "친구 업데이트",
                false,
                false,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                        headerWithName(AUTHORIZATION_HEADER).description("발급받은 JWT")
                ),
                pathParameters(
                        parameterWithName("friendId").description("상세 조회할 친구의 UUID")
                ),
                requestFields(
                        fieldWithPath("name").description("친구 이름"),
                        fieldWithPath("relation").description("친구와의 관계"),
                        fieldWithPath("contactFrequency.contactWeek").description("연락 주기"),
                        fieldWithPath("contactFrequency.dayOfWeek").description("연락 요일"),
                        fieldWithPath("birthday").description("친구의 생일").optional(),
                        fieldWithPath("anniversaryList[].id").description("기념일 id"),
                        fieldWithPath("anniversaryList[].title").description("기념일 제목"),
                        fieldWithPath("anniversaryList[].date").description("기념일 날짜"),
                        fieldWithPath("memo").description("친구에 대한 메모"),
                        fieldWithPath("phone").description("친구의 전화번호")
                ),
                responseFields(
                        fieldWithPath("friendId").description("친구 ID"),
                        fieldWithPath("name").description("친구 이름"),
                        fieldWithPath("contactFrequency.contactWeek").description("연락 주기"),
                        fieldWithPath("contactFrequency.dayOfWeek").description("연락 요일"),
                        fieldWithPath("imageUrl").description("친구 프로필 이미지 URL"),
                        fieldWithPath("relation").description("친구와의 관계"),
                        fieldWithPath("birthday").description("친구의 생일").optional(),
                        fieldWithPath("anniversaryList").description("기념일 목록"),
                        fieldWithPath("anniversaryList[].id").description("기념일 id"),
                        fieldWithPath("anniversaryList[].title").description("기념일 제목"),
                        fieldWithPath("anniversaryList[].date").description("기념일 날짜"),
                        fieldWithPath("memo").description("친구에 대한 메모"),
                        fieldWithPath("phone").description("친구의 전화번호"),
                        fieldWithPath("lastContactAt").description("마지막 연락 날짜")
                )));
    }

    @Test
    @DisplayName("친구를 삭제할 수 있어야 한다.")
    void 친구를_삭제할_수_있어야_한다() throws Exception {
        // given
        UUID memberId = testMember.getMemberId();
        String accessToken = createAccessToken(memberId);

        // when
        ResultActions result = mockMvc.perform(
                delete("/friend/{friendId}", testFriend.getFriendId())
                        .header(AUTHORIZATION_HEADER, AUTHORIZATION_VALUE_PREFIX + accessToken)
                        .contentType(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isNoContent());

        // docs
        result.andDo(document("친구 삭제",
                "특정 친구를 삭제한다.",
                "친구 삭제",
                false,
                false,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                        headerWithName(AUTHORIZATION_HEADER).description("발급받은 JWT")
                ),
                pathParameters(
                        parameterWithName("friendId").description("삭제할 친구의 UUID")
                )
        ));
    }

    @Test
    @DisplayName("이번 달 챙길 친구 목록을 성공적으로 조회해야 한다")
    void 이번_달_챙길_친구_목록을_성공적으로_조회해야_한다() throws Exception {
        // given
        // 친구와 관련된 데이터 설정
        LocalDate now = LocalDate.now();
        FriendDetail friendDetail = testFriend.getFriendDetail();

        // 현재 달에 생일이 있도록 설정
        friendDetail.updateBirthday(now.withDayOfMonth(15));
        friendDetailRepository.save(friendDetail);

        // 친구 기념일 추가
        FriendAnniversary anniversary = FriendAnniversary.builder()
                .friendId(testFriend.getFriendId())
                .title("테스트 기념일")
                .date(now.withDayOfMonth(20))
                .build();
        friendAnniversaryRepository.save(anniversary);

        // 친구의 다음 연락일을 이번 달로 설정
        testFriend.updateNextContactAt();
        friendRepository.save(testFriend);

        // when & then
        ResultActions result = mockMvc.perform(get(url + "/monthly")
                        .header(AUTHORIZATION_HEADER,
                                AUTHORIZATION_VALUE_PREFIX + createAccessToken(testMember.getMemberId()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].friendId").exists())
                .andExpect(jsonPath("$[*].name").exists())
                .andExpect(jsonPath("$[*].type").exists())
                .andExpect(jsonPath("$[*].nextContactAt").exists());

        // docs
        result.andDo(document("이번 달 챙길 친구 목록 조회",
                "이번 달에 챙겨야 할 친구 목록을 조회한다.",
                "생일이나 기념일이 이번 달에 있는 친구 목록을 조회한다.",
                false,
                false,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                        headerWithName(AUTHORIZATION_HEADER).description("발급받은 JWT")
                ),
                responseFields(
                        fieldWithPath("[].friendId").description("친구 ID"),
                        fieldWithPath("[].name").description("친구 이름"),
                        fieldWithPath("[].type").description("유형 (birthday 또는 기념일 제목)"),
                        fieldWithPath("[].nextContactAt").description("다음 연락 예정일")
                )
        ));
    }


    @Test
    @DisplayName("이번 달 챙김 완료한 친구 목록을 성공적으로 조회해야 한다.")
    void 이번_달_챙김_완료한_친구_목록을_성공적으로_조회해야_한다() throws Exception {
        // given
        // 테스트 데이터 준비
        Friend friend = Friend.builder()
                .memberId(testMember.getMemberId())
                .name("테스트 친구")
                .friendSource(FriendSource.APPLE)
                .contactFrequency(FriendContactFrequency.builder()
                        .contactWeek(FriendContactWeek.EVERY_MONTH)
                        .dayOfWeek(DayOfWeek.MONDAY)
                        .build())
                .position(1)
                .nextContactAt(LocalDate.now().plusDays(7))
                .build();

        friendRepository.save(friend);

        // 친구 상세 정보 추가 (생일 포함)
        FriendDetail friendDetail = FriendDetail.builder()
                .friend(friend)
                .relation(FriendRelation.FRIEND)
                .birthday(LocalDate.now().minusYears(30))
                .memo("테스트 메모")
                .build();
        friend.addFriendDetail(friendDetail);
        friendDetailRepository.save(friendDetail);

        // 기념일 추가
        FriendAnniversary anniversary = FriendAnniversary.builder()
                .friendId(friend.getFriendId())
                .title("결혼기념일")
                .date(LocalDate.now().minusYears(5))
                .build();
        friendAnniversaryRepository.save(anniversary);

        // 체크 로그 추가 (isChecked = true)
        FriendCheckingLog checkingLog = FriendCheckingLog.of(friend, true);
        friendCheckingLogRepository.save(checkingLog);

        String accessToken = createAccessToken(testMember.getMemberId());

        // when
        ResultActions result = mockMvc.perform(get(url + "/monthly/complete")
                        .header(AUTHORIZATION_HEADER, AUTHORIZATION_VALUE_PREFIX + accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // then
        result.andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").isNumber())
                .andExpect(jsonPath("$[0].friendId").exists())
                .andExpect(jsonPath("$[0].name").value("테스트 친구"))
                .andExpect(jsonPath("$[0].type").exists())
                .andExpect(jsonPath("$[0].nextContactAt").exists());

        // docs
        result.andDo(document("이번 달 챙김 완료한 친구 목록 조회 성공",
                "이번 달 챙김 완료한 친구 목록(생일 및 기념일)을 조회한다.",
                "이번 달에 챙김 표시가 된 친구들의 생일 및 기념일 정보를 반환한다.",
                false,
                false,
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                        headerWithName(AUTHORIZATION_HEADER).description("발급받은 JWT")
                ),
                responseFields(
                        fieldWithPath("[].friendId").description("친구 ID"),
                        fieldWithPath("[].name").description("친구 이름"),
                        fieldWithPath("[].type").description("챙김 타입 (birthday 또는 기념일 제목)"),
                        fieldWithPath("[].nextContactAt").description("다음 연락 예정일")
                )
        ));
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