package kr.swyp.backend.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import kr.swyp.backend.friend.domain.ContactReminder;
import kr.swyp.backend.friend.enums.ReminderType;
import kr.swyp.backend.friend.repository.ContactReminderRepository;
import kr.swyp.backend.member.domain.Member;
import kr.swyp.backend.member.dto.MemberDto.MemberWithdrawRequest;
import kr.swyp.backend.member.dto.MemberDto.MigrationStatusResponse;
import kr.swyp.backend.member.enums.RoleType;
import kr.swyp.backend.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class MemberServiceImplTest {

    @Autowired
    private MemberServiceImpl memberService;

    @Autowired
    private MemberRepository memberRepository;

    @MockitoBean
    private ContactReminderRepository contactReminderRepository;

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
    @DisplayName("활성 알림이 있는 경우 마이그레이션 상태가 true를 반환해야 한다.")
    void 활성_알림이_있는_경우_마이그레이션_상태가_true를_반환해야_한다() {
        // given
        Member member = createMember("test@test.com", "test");
        UUID memberId = member.getMemberId();

        ContactReminder activeReminder = ContactReminder.builder()
                .memberId(memberId)
                .reminderType(ReminderType.CONTACT)
                .reminderDate(LocalDate.now().plusDays(7))
                .isActive(true)
                .build();

        when(contactReminderRepository.findByMemberIdAndIsActiveTrue(memberId))
                .thenReturn(List.of(activeReminder));

        // when
        MigrationStatusResponse response = memberService.checkReminderMigrationStatus(memberId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getIsMigrated()).isTrue();
    }

    @Test
    @DisplayName("활성 알림이 없는 경우 마이그레이션 상태가 false를 반환해야 한다.")
    void 활성_알림이_없는_경우_마이그레이션_상태가_false를_반환해야_한다() {
        // given
        Member member = createMember("test2@test.com", "test2");
        UUID memberId = member.getMemberId();

        when(contactReminderRepository.findByMemberIdAndIsActiveTrue(memberId))
                .thenReturn(Collections.emptyList());

        // when
        MigrationStatusResponse response = memberService.checkReminderMigrationStatus(memberId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getIsMigrated()).isFalse();
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