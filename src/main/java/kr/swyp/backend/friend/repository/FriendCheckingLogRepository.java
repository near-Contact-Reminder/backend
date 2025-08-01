package kr.swyp.backend.friend.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.swyp.backend.friend.domain.FriendCheckingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@SuppressWarnings("checkstyle:LineLength")
public interface FriendCheckingLogRepository extends JpaRepository<FriendCheckingLog, Long> {

    @Query("""
            SELECT COUNT(f)
            FROM FriendCheckingLog f
            WHERE f.friend.friendId = :friendId
            AND f.isChecked = true
            """)
    Integer countCheckedLogsByFriendId(@Param("friendId") UUID friendId);

    List<FriendCheckingLog> findByFriend_FriendId(UUID friendId);

    Optional<FriendCheckingLog> findFirstByFriend_FriendIdAndIsCheckedTrueOrderByCreatedAtDesc(
            UUID friendId);

    List<FriendCheckingLog> findAllByFriend_FriendIdIsInAndIsCheckedTrueAndCreatedAtBetweenOrderByCreatedAtDesc(
            List<UUID> friendIdList, LocalDateTime startDateTime, LocalDateTime endDateTime);

    @Query("""
            SELECT AVG(CASE WHEN f.isChecked THEN 1 ELSE 0 END)
            FROM FriendCheckingLog f
            WHERE f.friend.friendId = :friendId
            """)
    BigDecimal averageCheckedLogsByFriendId(UUID friendId);
}