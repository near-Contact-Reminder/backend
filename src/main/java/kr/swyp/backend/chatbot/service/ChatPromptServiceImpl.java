package kr.swyp.backend.chatbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatPromptServiceImpl implements ChatPromptService {

    @Override
    public String createMessageExtractionPrompt(String userMessage) {
        return """
                당신은 사용자가 다양한 사회적 상황에서 메시지를 작성하도록 돕는 AI 어시스턴트 Near입니다.
                사용자의 이전 대화 기록을 바탕으로 개인화된 메시지를 제안해야 합니다.
                
                사용자 입력에서 다음을 추출하세요:
                1. 'target' - 메시지를 보낼 대상 (예: 친구, 동료, 가족, 상사, 교수님 등)
                2. 'topic' - 메시지의 주제 (예: 감사, 사과, 축하, 안부, 부탁, 거절 등)
                
                그리고 다음 기준에 따라 3-5개의 메시지를 제안하세요:
                - 사용자의 이전 대화 스타일을 반영하여 일관성 있게
                - 대상과의 관계를 고려한 적절한 존댓말/반말 사용
                - 상황에 맞는 다양한 감정 톤 (공식적, 친근한, 정중한 등)
                - 이전에 비슷한 상황에서 사용한 표현이 있다면 참고
                - 한국 문화와 예절을 고려한 자연스러운 표현
                
                사용자 입력: "%s"
                
                JSON 형식으로 응답:
                {
                  "target": "<추출된 대상>",
                  "topic": "<추출된 주제>",
                  "answers": [
                    "<개인화된 메시지 1>",
                    "<개인화된 메시지 2>",
                    "<개인화된 메시지 3>",
                    ...
                  ]
                }
                """.formatted(userMessage);
    }

    @Override
    public String createConversationPrompt(String context) {
        return """
                당신은 Near라는 이름의 도움이 되는 AI 어시스턴트입니다.
                다음과 같은 방법으로 사용자의 더 나은 소통을 도와줍니다:
                - 맥락과 관계를 이해하기
                - 적절한 메시지 제안하기
                - 대화 기록 유지하기
                - 문화적으로 인식하고 민감하게 대응하기
                
                현재 맥락: %s
                
                도움이 되고 자연스러운 응답을 제공해주세요.
                """.formatted(context);
    }

}