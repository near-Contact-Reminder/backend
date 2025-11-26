package kr.swyp.backend.term.service;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;
import kr.swyp.backend.member.domain.MemberTermsAgreement;
import kr.swyp.backend.member.repository.MemberTermsAgreementRepository;
import kr.swyp.backend.term.domain.Term;
import kr.swyp.backend.term.dto.TermDto.TermAgreementRequest;
import kr.swyp.backend.term.dto.TermDto.TermAgreementResponse;
import kr.swyp.backend.term.dto.TermDto.TermResponse;
import kr.swyp.backend.term.dto.TermDto.TermsAgreementRequest;
import kr.swyp.backend.term.dto.TermDto.TermsAgreementResponse;
import kr.swyp.backend.term.repository.TermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TermServiceImpl implements TermService {

    private final TermRepository termRepository;
    private final MemberTermsAgreementRepository memberTermsAgreementRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TermResponse> getAllTerms() {
        return termRepository.findAll().stream()
                .map(TermResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TermsAgreementResponse getMemberTermsAgreements(UUID memberId) {
        // 모든 약관 조회
        List<Term> allTerms = termRepository.findAll();

        // 회원의 약관 동의 정보 조회
        List<MemberTermsAgreement> memberAgreements =
                memberTermsAgreementRepository.findAllByMemberId(memberId);

        // termId를 키로 하는 Map 생성
        Map<Long, MemberTermsAgreement> agreementMap = memberAgreements.stream()
                .collect(Collectors.toMap(
                        MemberTermsAgreement::getTermsId,
                        agreement -> agreement
                ));

        // 모든 약관에 대해 동의 상태를 포함한 응답 생성
        List<TermAgreementResponse> responses = allTerms.stream()
                .map(term -> TermAgreementResponse.fromEntity(
                        term,
                        agreementMap.get(term.getId())
                ))
                .collect(Collectors.toList());

        return TermsAgreementResponse.of(responses);
    }

    @Override
    @Transactional
    public void saveMemberTermsAgreements(UUID memberId, TermsAgreementRequest request) {
        for (TermAgreementRequest agreementRequest : request.getAgreements()) {
            // 약관 존재 여부 확인
            Term term = termRepository.findById(agreementRequest.getTermId())
                    .orElseThrow(() -> new NoSuchElementException(
                            "약관을 찾을 수 없습니다. ID: " + agreementRequest.getTermId()));

            // 필수 약관인데 동의하지 않은 경우 예외 발생
            if (term.getIsRequired() && !agreementRequest.getIsAgreed()) {
                throw new IllegalArgumentException(
                        "필수 약관에 동의해야 합니다: " + term.getTitle());
            }

            // 기존 동의 정보 조회
            MemberTermsAgreement existingAgreement =
                    memberTermsAgreementRepository.findByMemberIdAndTermsId(
                            memberId, agreementRequest.getTermId()
                    ).orElse(null);

            if (existingAgreement != null) {
                // 기존 레코드가 있으면 삭제 후 새로 생성 (업데이트)
                memberTermsAgreementRepository.delete(existingAgreement);
            }

            // 새로운 동의 정보 저장
            MemberTermsAgreement newAgreement = MemberTermsAgreement.builder()
                    .memberId(memberId)
                    .termsId(agreementRequest.getTermId())
                    .isAgreed(agreementRequest.getIsAgreed())
                    .build();

            memberTermsAgreementRepository.save(newAgreement);
        }
    }
}
