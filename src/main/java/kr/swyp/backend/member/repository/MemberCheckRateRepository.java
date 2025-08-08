package kr.swyp.backend.member.repository;

import java.util.Optional;
import kr.swyp.backend.member.domain.Member;
import kr.swyp.backend.member.domain.MemberCheckRate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberCheckRateRepository extends JpaRepository<MemberCheckRate, Long> {

    Optional<MemberCheckRate> findByMember(Member member);

    void deleteByMember(Member member);
}
