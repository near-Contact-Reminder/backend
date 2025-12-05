package kr.swyp.backend.friend.repository;

import feign.Param;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.swyp.backend.friend.domain.Friend;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FriendRepository extends JpaRepository<Friend, UUID> {

    @Query("""
            SELECT AVG(f.checkRate)
            FROM Friend f
            WHERE f.memberId = :memberId
            """)
    Double findAverageCheckRateByMemberId(@Param("memberId") UUID memberId);

    @EntityGraph(attributePaths = {"friendDetail"})
    Optional<Friend> findByFriendIdAndMemberId(UUID friendId, UUID memberId);

    @Query("""
            SELECT f FROM Friend f
            JOIN FETCH f.friendDetail fd
            WHERE f.memberId = :memberId
            ORDER BY f.position ASC
            """)
    List<Friend> findAllByMemberIdWithDetail(@Param("memberId") UUID memberId);

    List<Friend> findAllByMemberId(UUID memberId);

    List<Friend> findAllByMemberIdAndNextContactAtBetween(UUID memberId, LocalDate startDate,
            LocalDate endDate);

    @Query("""
            SELECT DISTINCT f
            FROM Friend f
            JOIN FriendCheckingLog fcl
            ON fcl.friend = f
            WHERE f.memberId = :memberId AND fcl.isChecked = true
            AND fcl.createdAt BETWEEN :startDateTime AND :endDateTime
            """)
    List<Friend> findAllByMemberIdWithCheckedLogsInPeriod(
            @Param("memberId") UUID memberId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);

    void deleteAllByMemberId(UUID memberId);

    /**
     * 특정 날짜에 연락해야 할 친구 목록 조회 (알림 대상).
     */
    List<Friend> findAllByNextContactAt(LocalDate targetDate);

}

