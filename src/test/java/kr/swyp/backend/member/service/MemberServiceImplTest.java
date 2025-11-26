package kr.swyp.backend.member.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.swyp.backend.friend.domain.Friend;
import kr.swyp.backend.friend.domain.FriendContactFrequency;
import kr.swyp.backend.friend.domain.FriendDetail;
import kr.swyp.backend.friend.enums.FriendContactWeek;
import kr.swyp.backend.friend.enums.FriendRelation;
import kr.swyp.backend.friend.enums.FriendSource;
import kr.swyp.backend.friend.repository.FriendRepository;
import kr.swyp.backend.member.domain.Member;
import kr.swyp.backend.member.domain.MemberCheckRate;
import kr.swyp.backend.member.domain.MemberTermsAgreement;
import kr.swyp.backend.member.dto.MemberDto.MemberWithdrawRequest;
import kr.swyp.backend.member.enums.RoleType;
import kr.swyp.backend.member.repository.MemberCheckRateRepository;
import kr.swyp.backend.member.repository.MemberRepository;
import kr.swyp.backend.member.repository.MemberTermsAgreementRepository;
import kr.swyp.backend.term.domain.Term;
import kr.swyp.backend.term.repository.TermRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class MemberServiceImplTest {

    @Autowired
    private MemberServiceImpl memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private FriendRepository friendRepository;

    @Autowired
    private MemberCheckRateRepository memberCheckRateRepository;

    @Autowired
    private MemberTermsAgreementRepository memberTermsAgreementRepository;

    @Autowired
    private TermRepository termRepository;

    @Test
    @DisplayName("회원탈퇴를 할 수 있어야 한다.")
    void 회원탈퇴를_할_수_있어야_한다() {
        // given
        String username = "test@test.com";
        String nickname = "test";

        Member member = createMember(username, nickname);

        // when
        memberService.withdrawMember(member.getMemberId(),
                MemberWithdrawRequest.builder()
                        .reasonType("test")
                        .customReason("test")
                        .build());

        // then
        assertThat(
                memberRepository.findById(member.getMemberId()).get().getWithdrawnAt()).isNotNull();
    }

    @Test
    @DisplayName("회원 탈퇴 시 Friend 데이터가 삭제되어야 한다.")
    void 회원_탈퇴_시_Friend_데이터가_삭제되어야_한다() {
        // given
        String username = "test@test.com";
        String nickname = "test";
        Member member = createMember(username, nickname);

        // Friend 생성
        Friend friend1 = createFriend(member, "친구1", 1);
        Friend friend2 = createFriend(member, "친구2", 2);
        friendRepository.saveAll(List.of(friend1, friend2));

        // when
        memberService.withdrawMember(member.getMemberId(),
                MemberWithdrawRequest.builder()
                        .reasonType("test")
                        .customReason("test")
                        .build());

        // then
        List<Friend> friends = friendRepository.findAllByMemberId(member.getMemberId());
        assertThat(friends).isEmpty();
    }

    @Test
    @DisplayName("회원 탈퇴 시 MemberCheckRate가 삭제되어야 한다.")
    void 회원_탈퇴_시_MemberCheckRate가_삭제되어야_한다() {
        // given
        String username = "test@test.com";
        String nickname = "test";
        Member member = createMember(username, nickname);

        // MemberCheckRate 생성
        MemberCheckRate checkRate = MemberCheckRate.builder()
                .member(member)
                .checkRate(80)
                .build();
        memberCheckRateRepository.save(checkRate);

        // when
        memberService.withdrawMember(member.getMemberId(),
                MemberWithdrawRequest.builder()
                        .reasonType("test")
                        .customReason("test")
                        .build());

        // then
        assertThat(memberCheckRateRepository.findByMember(member)).isEmpty();
    }

    @Test
    @DisplayName("회원 탈퇴 시 Friend와 연관된 FriendDetail도 함께 삭제되어야 한다.")
    void 회원_탈퇴_시_Friend와_연관된_FriendDetail도_함께_삭제되어야_한다() {
        // given
        String username = "test@test.com";
        String nickname = "test";
        Member member = createMember(username, nickname);

        // Friend와 FriendDetail 생성
        Friend friend = createFriend(member, "친구1", 1);
        FriendDetail friendDetail = FriendDetail.builder()
                .friend(friend)
                .relation(FriendRelation.FRIEND)
                .phone("01012345678")
                .birthday(LocalDate.of(1990, 1, 1))
                .memo("테스트 메모")
                .build();
        friend.addFriendDetail(friendDetail);
        friendRepository.save(friend);

        // when
        memberService.withdrawMember(member.getMemberId(),
                MemberWithdrawRequest.builder()
                        .reasonType("test")
                        .customReason("test")
                        .build());

        // then
        List<Friend> friends = friendRepository.findAllByMemberId(member.getMemberId());
        assertThat(friends).isEmpty();
    }

    @Test
    @DisplayName("회원 탈퇴 시 약관 동의 정보가 삭제되어야 한다.")
    void 회원_탈퇴_시_약관_동의_정보가_삭제되어야_한다() {
        // given
        String username = "test@test.com";
        String nickname = "test";
        Member member = createMember(username, nickname);

        // 약관 생성
        Term term1 = Term.builder()
                .title("서비스 이용 약관")
                .version("1.0")
                .isRequired(true)
                .build();
        Term term2 = Term.builder()
                .title("개인정보 수집 및 이용 동의서")
                .version("1.0")
                .isRequired(true)
                .build();
        termRepository.saveAll(List.of(term1, term2));

        // 약관 동의 생성
        MemberTermsAgreement agreement1 = MemberTermsAgreement.builder()
                .memberId(member.getMemberId())
                .termsId(term1.getId())
                .isAgreed(true)
                .build();
        MemberTermsAgreement agreement2 = MemberTermsAgreement.builder()
                .memberId(member.getMemberId())
                .termsId(term2.getId())
                .isAgreed(true)
                .build();
        memberTermsAgreementRepository.saveAll(List.of(agreement1, agreement2));

        // when
        memberService.withdrawMember(member.getMemberId(),
                MemberWithdrawRequest.builder()
                        .reasonType("test")
                        .customReason("test")
                        .build());

        // then
        List<MemberTermsAgreement> agreements =
                memberTermsAgreementRepository.findAllByMemberId(member.getMemberId());
        assertThat(agreements).isEmpty();
    }

    private Member createMember(String username, String nickname) {

        Member member = Member.builder()
                .username(username)
                .nickname(nickname)
                .password(" ")
                .isActive(true)
                .notificationAgreedAt(LocalDateTime.now())
                .build();

        member.addRole(RoleType.USER);

        return memberRepository.save(member);
    }

    private Friend createFriend(Member member, String name, int position) {
        return Friend.builder()
                .memberId(member.getMemberId())
                .name(name)
                .friendSource(FriendSource.KAKAO)
                .contactFrequency(FriendContactFrequency.builder()
                        .contactWeek(FriendContactWeek.EVERY_WEEK)
                        .build())
                .position(position)
                .nextContactAt(LocalDate.now().plusWeeks(1))
                .checkRate(0)
                .alarmTriggerCount(0)
                .build();
    }
}