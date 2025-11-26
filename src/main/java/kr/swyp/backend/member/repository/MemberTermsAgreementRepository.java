package kr.swyp.backend.member.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.swyp.backend.member.domain.MemberTermsAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberTermsAgreementRepository extends JpaRepository<MemberTermsAgreement, Long> {

    List<MemberTermsAgreement> findAllByMemberId(UUID memberId);

    Optional<MemberTermsAgreement> findByMemberIdAndTermsId(UUID memberId, Long termsId);

    void deleteAllByMemberId(UUID memberId);
}
