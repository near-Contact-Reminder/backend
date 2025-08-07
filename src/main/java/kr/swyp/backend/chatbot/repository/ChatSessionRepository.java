package kr.swyp.backend.chatbot.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.swyp.backend.chatbot.domain.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    Optional<ChatSession> findBySessionIdAndIsActiveTrue(String sessionId);
    
    List<ChatSession> findByMemberIdAndIsActiveTrueOrderByCreatedAtDesc(UUID memberId);
    
    Optional<ChatSession> findBySessionId(String sessionId);
}