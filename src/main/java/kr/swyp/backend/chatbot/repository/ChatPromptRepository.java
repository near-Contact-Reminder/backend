package kr.swyp.backend.chatbot.repository;

import java.util.Optional;
import kr.swyp.backend.chatbot.domain.ChatPrompt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatPromptRepository extends JpaRepository<ChatPrompt, Long> {

    // 활성화된 프롬프트만 key로 조회
    Optional<ChatPrompt> findByPromptKeyAndIsActiveTrue(String promptKey);
}
