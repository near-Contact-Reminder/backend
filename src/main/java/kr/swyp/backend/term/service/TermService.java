package kr.swyp.backend.term.service;

import java.util.List;
import java.util.UUID;
import kr.swyp.backend.term.dto.TermDto.TermAgreementResponse;
import kr.swyp.backend.term.dto.TermDto.TermResponse;
import kr.swyp.backend.term.dto.TermDto.TermsAgreementRequest;
import kr.swyp.backend.term.dto.TermDto.TermsAgreementResponse;

public interface TermService {

    /**
     * 모든 약관 목록 조회.
     */
    List<TermResponse> getAllTerms();

    /**
     * 특정 회원의 약관 동의 상태 조회.
     */
    TermsAgreementResponse getMemberTermsAgreements(UUID memberId);

    /**
     * 회원의 약관 동의 저장/업데이트.
     */
    void saveMemberTermsAgreements(UUID memberId, TermsAgreementRequest request);
}
