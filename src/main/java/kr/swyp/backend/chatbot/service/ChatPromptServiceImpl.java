package kr.swyp.backend.chatbot.service;

import kr.swyp.backend.chatbot.domain.ChatPrompt;
import kr.swyp.backend.chatbot.repository.ChatPromptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatPromptServiceImpl implements ChatPromptService {

    private final ChatPromptRepository chatPromptRepository;

    @Override
    public String createMessageExtractionPrompt(String userMessage) {
        log.info("[프롬프트] MESSAGE_EXTRACTION 프롬프트 조회 시작");

        // DB에서 프롬프트 조회
        ChatPrompt prompt = chatPromptRepository
                .findByPromptKeyAndIsActiveTrue("MESSAGE_EXTRACTION")
                .orElseThrow(() -> {
                    log.error("[프롬프트] MESSAGE_EXTRACTION 프롬프트를 찾을 수 없습니다");
                    return new IllegalStateException("메시지 추출 프롬프트를 찾을 수 없습니다");
                });

        log.info("[프롬프트] DB에서 조회된 프롬프트 ID: {}, Key: {}", prompt.getId(), prompt.getPromptKey());
        log.debug("[프롬프트] 원본 프롬프트 내용:\n{}", prompt.getPromptContent());

        // %s를 실제 메시지로 치환
        String formattedPrompt = prompt.getPromptContent().formatted(userMessage);

        log.info("[프롬프트] 사용자 메시지로 치환 완료");
        log.debug("[프롬프트] 최종 프롬프트:\n{}", formattedPrompt);

        return formattedPrompt;
    }

    @Override
    public String createConversationPrompt(String context) {
        log.info("[프롬프트] CONVERSATION 프롬프트 조회 시작");

        ChatPrompt prompt = chatPromptRepository
                .findByPromptKeyAndIsActiveTrue("CONVERSATION")
                .orElseThrow(() -> {
                    log.error("[프롬프트] CONVERSATION 프롬프트를 찾을 수 없습니다");
                    return new IllegalStateException("대화 프롬프트를 찾을 수 없습니다");
                });

        log.info("[프롬프트] DB에서 조회된 프롬프트 ID: {}, Key: {}", prompt.getId(), prompt.getPromptKey());
        log.debug("[프롬프트] 원본 프롬프트 내용:\n{}", prompt.getPromptContent());

        String formattedPrompt = prompt.getPromptContent().formatted(context);

        log.info("[프롬프트] 컨텍스트로 치환 완료");
        log.debug("[프롬프트] 최종 프롬프트:\n{}", formattedPrompt);

        return formattedPrompt;
    }

}