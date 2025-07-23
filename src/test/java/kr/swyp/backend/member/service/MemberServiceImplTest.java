package kr.swyp.backend.member.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import kr.swyp.backend.member.domain.Member;
import kr.swyp.backend.member.dto.MemberDto.MemberWithdrawRequest;
import kr.swyp.backend.member.enums.RoleType;
import kr.swyp.backend.member.repository.MemberRepository;
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
}