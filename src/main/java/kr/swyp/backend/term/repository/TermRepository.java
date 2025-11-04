package kr.swyp.backend.term.repository;

import java.util.Optional;
import kr.swyp.backend.term.domain.Term;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermRepository extends JpaRepository<Term, Long> {

    Optional<Term> findByTitle(String title);
}
