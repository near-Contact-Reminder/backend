package kr.swyp.backend.term.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import kr.swyp.backend.member.domain.Member;
import kr.swyp.backend.member.domain.MemberTermsAgreement;
import kr.swyp.backend.member.enums.RoleType;
import kr.swyp.backend.member.repository.MemberRepository;
import kr.swyp.backend.member.repository.MemberTermsAgreementRepository;
import kr.swyp.backend.term.domain.Term;
import kr.swyp.backend.term.dto.TermDto.TermAgreementRequest;
import kr.swyp.backend.term.dto.TermDto.TermAgreementResponse;
import kr.swyp.backend.term.dto.TermDto.TermResponse;
import kr.swyp.backend.term.dto.TermDto.TermsAgreementRequest;
import kr.swyp.backend.term.dto.TermDto.TermsAgreementResponse;
import kr.swyp.backend.term.repository.TermRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class TermServiceImplTest {

    @Autowired
    private TermService termService;

    @Autowired
    private TermRepository termRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberTermsAgreementRepository memberTermsAgreementRepository;

    private Member testMember;
    private Term serviceTerms;
    private Term privacyTerms;
    private Term privacyPolicyTerms;

    @BeforeEach
    void setUp() {
        // 테스트용 회원 생성
        testMember = Member.builder()
                .username("test@test.com")
                .nickname("테스트유저")
                .password(" ")
                .isActive(true)
                .notificationAgreedAt(LocalDateTime.now())
                .build();
        testMember.addRole(RoleType.USER);
        testMember = memberRepository.save(testMember);

        // 테스트용 약관 생성
        serviceTerms = Term.builder()
                .title("서비스 이용 약관")
                .version("1.0")
                .isRequired(true)
                .build();
        serviceTerms = termRepository.save(serviceTerms);

        privacyTerms = Term.builder()
                .title("개인정보 수집 및 이용 동의서")
                .version("1.0")
                .isRequired(true)
                .build();
        privacyTerms = termRepository.save(privacyTerms);

        privacyPolicyTerms = Term.builder()
                .title("개인정보 처리방침")
                .version("1.0")
                .isRequired(false)
                .build();
        privacyPolicyTerms = termRepository.save(privacyPolicyTerms);
    }

    @Test
    @DisplayName("모든 약관 목록을 조회할 수 있어야 한다")
    void 모든_약관_목록을_조회할_수_있어야_한다() {
        // when
        List<TermResponse> terms = termService.getAllTerms();

        // then
        assertThat(terms).hasSize(3);
        assertThat(terms).extracting("title")
                .contains("서비스 이용 약관", "개인정보 수집 및 이용 동의서", "개인정보 처리방침");
    }

    @Test
    @DisplayName("회원의 약관 동의 상태를 조회할 수 있어야 한다")
    void 회원의_약관_동의_상태를_조회할_수_있어야_한다() {
        // given
        MemberTermsAgreement agreement = MemberTermsAgreement.builder()
                .memberId(testMember.getMemberId())
                .termsId(serviceTerms.getId())
                .isAgreed(true)
                .build();
        memberTermsAgreementRepository.save(agreement);

        // when
        TermsAgreementResponse response = termService.getMemberTermsAgreements(
                testMember.getMemberId());

        // then
        assertThat(response.getAgreements()).hasSize(3);

        TermAgreementResponse serviceTermAgreement = response.getAgreements().stream()
                .filter(a -> a.getTermId().equals(serviceTerms.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(serviceTermAgreement.getIsAgreed()).isTrue();
        assertThat(serviceTermAgreement.getAgreedAt()).isNotNull();

        TermAgreementResponse privacyTermAgreement = response.getAgreements().stream()
                .filter(a -> a.getTermId().equals(privacyTerms.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(privacyTermAgreement.getIsAgreed()).isFalse();
        assertThat(privacyTermAgreement.getAgreedAt()).isNull();
    }

    @Test
    @DisplayName("회원의 약관 동의를 저장할 수 있어야 한다")
    void 회원의_약관_동의를_저장할_수_있어야_한다() {
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
        termService.saveMemberTermsAgreements(testMember.getMemberId(), request);

        // then
        List<MemberTermsAgreement> agreements = memberTermsAgreementRepository.findAllByMemberId(
                testMember.getMemberId());

        assertThat(agreements).hasSize(3);
        assertThat(agreements).allMatch(MemberTermsAgreement::getIsAgreed);
    }

    @Test
    @DisplayName("필수 약관에 동의하지 않으면 예외가 발생해야 한다")
    void 필수_약관에_동의하지_않으면_예외가_발생해야_한다() {
        // given
        TermsAgreementRequest request = TermsAgreementRequest.builder()
                .agreements(List.of(
                        TermAgreementRequest.builder()
                                .termId(serviceTerms.getId())
                                .isAgreed(false) // 필수 약관인데 동의 안 함
                                .build()
                ))
                .build();

        // when & then
        assertThatThrownBy(() ->
                termService.saveMemberTermsAgreements(testMember.getMemberId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("필수 약관에 동의해야 합니다");
    }

    @Test
    @DisplayName("약관 동의를 업데이트할 수 있어야 한다")
    void 약관_동의를_업데이트할_수_있어야_한다() {
        // given - 초기 동의
        TermsAgreementRequest initialRequest = TermsAgreementRequest.builder()
                .agreements(List.of(
                        TermAgreementRequest.builder()
                                .termId(serviceTerms.getId())
                                .isAgreed(true)
                                .build()
                ))
                .build();
        termService.saveMemberTermsAgreements(testMember.getMemberId(), initialRequest);

        // when - 동의 상태 변경 (필수 약관이므로 실제로는 동의 철회 불가능하지만 테스트용)
        TermsAgreementRequest updateRequest = TermsAgreementRequest.builder()
                .agreements(List.of(
                        TermAgreementRequest.builder()
                                .termId(serviceTerms.getId())
                                .isAgreed(true) // 여전히 true
                                .build(),
                        TermAgreementRequest.builder()
                                .termId(privacyPolicyTerms.getId())
                                .isAgreed(true) // 새로 추가
                                .build()
                ))
                .build();
        termService.saveMemberTermsAgreements(testMember.getMemberId(), updateRequest);

        // then
        List<MemberTermsAgreement> agreements = memberTermsAgreementRepository.findAllByMemberId(
                testMember.getMemberId());

        assertThat(agreements).hasSize(2);
        assertThat(agreements).extracting(MemberTermsAgreement::getTermsId)
                .contains(serviceTerms.getId(), privacyPolicyTerms.getId());
    }

    @Test
    @DisplayName("존재하지 않는 약관에 동의하려고 하면 예외가 발생해야 한다")
    void 존재하지_않는_약관에_동의하려고_하면_예외가_발생해야_한다() {
        // given
        TermsAgreementRequest request = TermsAgreementRequest.builder()
                .agreements(List.of(
                        TermAgreementRequest.builder()
                                .termId(999L) // 존재하지 않는 약관 ID
                                .isAgreed(true)
                                .build()
                ))
                .build();

        // when & then
        assertThatThrownBy(() ->
                termService.saveMemberTermsAgreements(testMember.getMemberId(), request))
                .isInstanceOf(java.util.NoSuchElementException.class)
                .hasMessageContaining("약관을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("새로운 약관이 추가되면 조회 시 포함되어야 한다")
    void 새로운_약관이_추가되면_조회_시_포함되어야_한다() {
        // given - 새로운 약관 추가
        Term newTerm = Term.builder()
                .title("마케팅 정보 수신 동의")
                .version("1.0")
                .isRequired(false)
                .build();
        termRepository.save(newTerm);

        // when
        TermsAgreementResponse response = termService.getMemberTermsAgreements(
                testMember.getMemberId());

        // then
        assertThat(response.getAgreements()).hasSize(4);
        assertThat(response.getAgreements()).extracting("title")
                .contains("마케팅 정보 수신 동의");

        TermAgreementResponse marketingAgreement = response.getAgreements().stream()
                .filter(a -> a.getTitle().equals("마케팅 정보 수신 동의"))
                .findFirst()
                .orElseThrow();

        assertThat(marketingAgreement.getIsAgreed()).isFalse();
        assertThat(marketingAgreement.getIsRequired()).isFalse();
    }
}
