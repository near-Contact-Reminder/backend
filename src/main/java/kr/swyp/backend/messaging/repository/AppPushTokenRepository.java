package kr.swyp.backend.messaging.repository;

import java.util.Optional;
import java.util.UUID;
import kr.swyp.backend.messaging.domain.AppPushToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppPushTokenRepository extends JpaRepository<AppPushToken, Long> {

    Optional<AppPushToken> findByMemberId(UUID memberId);

    void deleteByMemberIdAndToken(UUID memberId, String token);
}
