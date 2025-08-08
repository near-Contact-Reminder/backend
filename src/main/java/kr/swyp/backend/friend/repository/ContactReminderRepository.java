package kr.swyp.backend.friend.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.swyp.backend.friend.domain.ContactReminder;
import kr.swyp.backend.friend.enums.ReminderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactReminderRepository extends JpaRepository<ContactReminder, Long> {

    List<ContactReminder> findByMemberIdAndIsActiveTrue(UUID memberId);

    List<ContactReminder> findByFriendFriendIdAndIsActiveTrue(UUID friendId);

    @Query("""
            SELECT cr
            FROM ContactReminder cr
            WHERE cr.reminderDate = :date
            AND cr.isActive = true
            """)
    List<ContactReminder> findActiveRemindersByDate(@Param("date") LocalDate date);

    @Query("""
            SELECT cr
            FROM ContactReminder cr
            WHERE cr.reminderDate <= :date
            AND cr.isActive = true
            """)
    List<ContactReminder> findActiveRemindersBeforeDate(@Param("date") LocalDate date);

    Optional<ContactReminder> findByFriendFriendIdAndReminderTypeAndIsActiveTrue(UUID friendId,
            ReminderType reminderType);

    Optional<ContactReminder> findByAnniversaryIdAndIsActiveTrue(Long anniversaryId);

    @Query("""
            SELECT cr FROM ContactReminder cr
            WHERE cr.memberId = :memberId
            AND cr.reminderDate BETWEEN :startDate AND :endDate
            AND cr.isActive = true
            ORDER BY cr.reminderDate
            """)
    List<ContactReminder> findActiveRemindersByMemberIdAndDateRange(
            @Param("memberId") UUID memberId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}