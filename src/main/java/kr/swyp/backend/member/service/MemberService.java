package kr.swyp.backend.member.service;

import java.util.UUID;
import kr.swyp.backend.member.dto.MemberDto.CheckRateResponse;
import kr.swyp.backend.member.dto.MemberDto.MemberInfoResponse;
import kr.swyp.backend.member.dto.MemberDto.MemberWithdrawRequest;
import kr.swyp.backend.member.dto.MemberDto.MigrationStatusResponse;

public interface MemberService {

    MemberInfoResponse getMemberInfo(UUID memberId);

    void withdrawMember(UUID memberId, MemberWithdrawRequest request);

    CheckRateResponse saveMemberCheckRate(UUID memberId);

    MigrationStatusResponse checkReminderMigrationStatus(UUID memberId);
}
