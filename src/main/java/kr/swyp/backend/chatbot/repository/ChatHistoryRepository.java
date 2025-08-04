package kr.swyp.backend.chatbot.repository;

import kr.swyp.backend.chatbot.domain.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {
}
