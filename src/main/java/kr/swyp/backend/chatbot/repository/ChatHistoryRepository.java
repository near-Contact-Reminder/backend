package kr.swyp.backend.chatbot.repository;

import java.util.List;
import java.util.UUID;
import kr.swyp.backend.chatbot.domain.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {

    List<ChatHistory> findTop5ByMemberIdOrderByIdDesc(UUID memberId);
    
    List<ChatHistory> findBySessionIdOrderByCreatedAtAsc(String sessionId);
    
    List<ChatHistory> findBySessionIdAndMessageTypeOrderByCreatedAtAsc(String sessionId, String messageType);
}
