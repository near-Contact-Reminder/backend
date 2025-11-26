package kr.swyp.backend.friend.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import kr.swyp.backend.friend.domain.FriendAnniversary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendAnniversaryRepository extends JpaRepository<FriendAnniversary, Long> {

    List<FriendAnniversary> findByFriendId(UUID friendId);

    List<FriendAnniversary> findByIdIsIn(List<Long> idList);

    List<FriendAnniversary> findAllByFriendIdIsInAndDateBetween(List<UUID> friendIds,
            LocalDate dateAfter, LocalDate dateBefore);

    void deleteByFriendId(UUID friendId);

    @Query("""
            SELECT fa
            FROM FriendAnniversary fa
            WHERE fa.friendId IN
                (SELECT DISTINCT fcl.friend.friendId
                  FROM FriendCheckingLog fcl
                  WHERE fcl.id IN :checkingLogIdList)
            """)
    List<FriendAnniversary> findAllAnniversaryByCheckingLogIdList(
            @Param("checkingLogIdList") List<Long> checkingLogIdList);

    /**
     * 오늘 날짜(월-일)의 기념일을 가진 항목들 조회 (알림 대상).
     * 연도는 무시하고 월-일만 비교.
     */
    @Query("""
            SELECT fa
            FROM FriendAnniversary fa
            WHERE MONTH(fa.date) = :month AND DAY(fa.date) = :day
            """)
    List<FriendAnniversary> findAllByMonthAndDay(@Param("month") int month, @Param("day") int day);
}
