package kr.swyp.backend.chatbot.service;

public interface ChatPromptService {

    String createMessageExtractionPrompt(String userMessage);

    String createConversationPrompt(String context);
}