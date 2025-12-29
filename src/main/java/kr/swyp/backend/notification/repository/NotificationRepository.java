package kr.swyp.backend.notification.repository;

import java.util.List;
import java.util.UUID;
import kr.swyp.backend.notification.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByMemberIdOrderByCreatedAtDesc(UUID memberId);

    List<Notification> findByMemberIdAndIsReadFalseOrderByCreatedAtDesc(UUID memberId);
}
