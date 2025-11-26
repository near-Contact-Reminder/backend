package kr.swyp.backend.member.integration;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.swyp.backend.authentication.dto.SocialLoginDto;
import kr.swyp.backend.authentication.service.SocialLoginService;
import kr.swyp.backend.friend.domain.Friend;
import kr.swyp.backend.friend.domain.FriendContactFrequency;
import kr.swyp.backend.friend.enums.FriendContactWeek;
import kr.swyp.backend.friend.enums.FriendSource;
import kr.swyp.backend.friend.repository.FriendRepository;
import kr.swyp.backend.member.domain.Member;
import kr.swyp.backend.member.domain.MemberCheckRate;
import kr.swyp.backend.member.domain.MemberSocialLoginInfo;
import kr.swyp.backend.member.dto.MemberDto.MemberWithdrawRequest;
import kr.swyp.backend.member.enums.RoleType;
import kr.swyp.backend.member.enums.SocialLoginProviderType;
import kr.swyp.backend.member.repository.MemberCheckRateRepository;
import kr.swyp.backend.member.repository.MemberRepository;
import kr.swyp.backend.member.repository.MemberSocialLoginInfoRepository;
import kr.swyp.backend.member.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class MemberWithdrawAndReJoinIntegrationTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private SocialLoginService socialLoginService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberSocialLoginInfoRepository socialLoginInfoRepository;

    @Autowired
    private FriendRepository friendRepository;

    @Autowired
    private MemberCheckRateRepository memberCheckRateRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("회원 탈퇴 후 재가입 시 Friend 데이터가 초기화되어야 한다.")
    void 회원_탈퇴_후_재가입_시_Friend_데이터가_초기화되어야_한다() {
        // given - 회원 생성
        String email = "test@kakao.com";
        String providerId = "kakao_12345";
        Member member = createMemberWithSocialLogin(email, providerId);

        // Friend 데이터 생성
        Friend friend1 = createFriend(member, "친구1", 1);
        Friend friend2 = createFriend(member, "친구2", 2);
        friendRepository.saveAll(List.of(friend1, friend2));

        // MemberCheckRate 생성
        MemberCheckRate checkRate = MemberCheckRate.builder()
                .member(member)
                .checkRate(80)
                .build();
        memberCheckRateRepository.save(checkRate);

        // 탈퇴 전 데이터 확인
        assertThat(friendRepository.findAllByMemberId(member.getMemberId())).hasSize(2);
        assertThat(memberCheckRateRepository.findByMember(member)).isPresent();

        // when - 회원 탈퇴
        memberService.withdrawMember(member.getMemberId(),
                MemberWithdrawRequest.builder()
                        .reasonType("테스트 탈퇴")
                        .customReason("테스트")
                        .build());

        // then - 탈퇴 후 데이터 확인
        Member withdrawnMember = memberRepository.findById(member.getMemberId()).get();
        assertThat(withdrawnMember.getWithdrawnAt()).isNotNull();
        assertThat(friendRepository.findAllByMemberId(member.getMemberId())).isEmpty();
        assertThat(memberCheckRateRepository.findByMember(withdrawnMember)).isEmpty();

        // given - 재가입 준비 (소셜 로그인 정보 재생성)
        MemberSocialLoginInfo newSocialLoginInfo = MemberSocialLoginInfo.builder()
                .member(withdrawnMember)
                .providerId(providerId)
                .providerType(SocialLoginProviderType.KAKAO)
                .build();
        socialLoginInfoRepository.save(newSocialLoginInfo);

        // when - 재가입 (reactivate)
        withdrawnMember.reactivate();
        withdrawnMember.addRole(RoleType.USER);
        memberRepository.save(withdrawnMember);

        // then - 재가입 후 데이터 확인
        Member reactivatedMember = memberRepository.findById(member.getMemberId()).get();
        assertThat(reactivatedMember.getWithdrawnAt()).isNull();
        assertThat(reactivatedMember.getIsActive()).isTrue();
        assertThat(friendRepository.findAllByMemberId(reactivatedMember.getMemberId())).isEmpty();
        assertThat(memberCheckRateRepository.findByMember(reactivatedMember)).isEmpty();
    }

    @Test
    @DisplayName("회원 탈퇴 후 재가입 시 새로운 Friend를 추가할 수 있어야 한다.")
    void 회원_탈퇴_후_재가입_시_새로운_Friend를_추가할_수_있어야_한다() {
        // given - 회원 생성 및 Friend 추가
        String email = "test@kakao.com";
        String providerId = "kakao_12345";
        Member member = createMemberWithSocialLogin(email, providerId);

        Friend oldFriend = createFriend(member, "이전 친구", 1);
        friendRepository.save(oldFriend);

        // 영속성 컨텍스트를 플러시하고 클리어
        entityManager.flush();
        entityManager.clear();

        // 회원 탈퇴
        memberService.withdrawMember(member.getMemberId(),
                MemberWithdrawRequest.builder()
                        .reasonType("테스트 탈퇴")
                        .customReason("테스트")
                        .build());

        // 영속성 컨텍스트를 플러시하고 클리어
        entityManager.flush();
        entityManager.clear();

        // 재가입
        Member withdrawnMember = memberRepository.findById(member.getMemberId()).get();
        MemberSocialLoginInfo newSocialLoginInfo = MemberSocialLoginInfo.builder()
                .member(withdrawnMember)
                .providerId(providerId)
                .providerType(SocialLoginProviderType.KAKAO)
                .build();
        socialLoginInfoRepository.save(newSocialLoginInfo);

        withdrawnMember.reactivate();
        withdrawnMember.addRole(RoleType.USER);
        memberRepository.save(withdrawnMember);

        // 영속성 컨텍스트를 플러시하고 클리어
        entityManager.flush();
        entityManager.clear();

        // when - 재가입 후 새로운 Friend 추가
        Member reactivatedMember = memberRepository.findById(member.getMemberId()).get();
        Friend newFriend = createFriend(reactivatedMember, "새로운 친구", 1);
        friendRepository.save(newFriend);

        entityManager.flush();
        entityManager.clear();

        // then
        List<Friend> friends = friendRepository.findAllByMemberId(reactivatedMember.getMemberId());
        assertThat(friends).hasSize(1);
        assertThat(friends.get(0).getName()).isEqualTo("새로운 친구");
        assertThat(friends.get(0).getFriendId()).isNotEqualTo(oldFriend.getFriendId());
    }

    @Test
    @DisplayName("여러 Friend가 있는 회원이 탈퇴 시 모든 Friend와 연관 데이터가 삭제되어야 한다.")
    void 여러_Friend가_있는_회원이_탈퇴_시_모든_Friend와_연관_데이터가_삭제되어야_한다() {
        // given
        String email = "test@kakao.com";
        String providerId = "kakao_12345";
        Member member = createMemberWithSocialLogin(email, providerId);

        // 10명의 Friend 생성
        for (int i = 1; i <= 10; i++) {
            Friend friend = createFriend(member, "친구" + i, i);
            friendRepository.save(friend);
        }

        MemberCheckRate checkRate = MemberCheckRate.builder()
                .member(member)
                .checkRate(75)
                .build();
        memberCheckRateRepository.save(checkRate);

        assertThat(friendRepository.findAllByMemberId(member.getMemberId())).hasSize(10);

        // when
        memberService.withdrawMember(member.getMemberId(),
                MemberWithdrawRequest.builder()
                        .reasonType("테스트 탈퇴")
                        .customReason("테스트")
                        .build());

        // then
        assertThat(friendRepository.findAllByMemberId(member.getMemberId())).isEmpty();
        assertThat(memberCheckRateRepository.findByMember(member)).isEmpty();
    }

    private Member createMemberWithSocialLogin(String email, String providerId) {
        Member member = Member.builder()
                .username(email)
                .nickname("테스트유저")
                .password(" ")
                .isActive(true)
                .notificationAgreedAt(LocalDateTime.now())
                .build();

        member.addRole(RoleType.USER);
        Member savedMember = memberRepository.save(member);

        MemberSocialLoginInfo socialLoginInfo = MemberSocialLoginInfo.builder()
                .member(savedMember)
                .providerId(providerId)
                .providerType(SocialLoginProviderType.KAKAO)
                .build();
        socialLoginInfoRepository.save(socialLoginInfo);

        return savedMember;
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
