package com.example.onlineshop.service;

import com.example.onlineshop.entity.ChatMessage;
import com.example.onlineshop.entity.ChatSession;
import com.example.onlineshop.mapper.ChatMessageMapper;
import com.example.onlineshop.mapper.ChatSessionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatService {

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final ExternalAiClient externalAiClient;

    public ChatService(ChatSessionMapper sessionMapper, ChatMessageMapper messageMapper, ExternalAiClient externalAiClient) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.externalAiClient = externalAiClient;
    }

    @Transactional
    public ChatSession createSession(String userId, String name) {
        System.out.println("[ChatService] createSession called, userId=" + userId + ", name=" + name);
        ChatSession session = ChatSession.builder()
                .userId(userId)
                .sessionName(name != null && !name.isBlank() ? name : "新对话")
                .build();
        int rows = sessionMapper.insert(session);
        System.out.println("[ChatService] insert result: " + rows + " rows, sessionId=" + session.getId());
        return session;
    }

    @Transactional
    public ChatSession createSessionIfNotExists(String userId) {
        ChatSession latest = sessionMapper.findLatestByUserId(userId);
        if (latest != null) {
            return latest;
        }
        return createSession(userId, "新对话");
    }

    public List<ChatSession> getUserSessions(String userId) {
        return sessionMapper.findByUserId(userId);
    }

    public ChatSession getSession(Integer sessionId) {
        return sessionMapper.findById(sessionId);
    }

    @Transactional
    public void renameSession(Integer sessionId, String newName) {
        sessionMapper.updateName(sessionId, newName);
    }

    @Transactional
    public String renameSessionWithAi(Integer sessionId, String userId) {
        ChatSession session = sessionMapper.findById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            return null;
        }

        List<ChatMessage> messages = messageMapper.findRecentBySessionId(sessionId, 5);
        if (messages == null || messages.isEmpty()) {
            return null;
        }

        StringBuilder context = new StringBuilder();
        for (ChatMessage msg : messages) {
            context.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        }

        String newName = externalAiClient.generateSessionName(context.toString());
        if (newName != null && !newName.isBlank()) {
            sessionMapper.updateName(sessionId, newName.trim());
            return newName.trim();
        }
        return null;
    }

    @Transactional
    public void deleteSession(Integer sessionId) {
        messageMapper.deleteBySessionId(sessionId);
        sessionMapper.deleteById(sessionId);
    }

    @Transactional
    public ChatMessage addMessage(Integer sessionId, String role, String content) {
        System.out.println("[ChatService] addMessage called, sessionId=" + sessionId + ", role=" + role);
        ChatMessage message = ChatMessage.builder()
                .sessionId(sessionId)
                .role(role)
                .content(content)
                .build();
        int rows = messageMapper.insert(message);
        System.out.println("[ChatService] insert result: " + rows + " rows, messageId=" + message.getId());

        ChatSession session = sessionMapper.findById(sessionId);
        if (session != null) {
            session.setUpdatedAt(LocalDateTime.now());
        }

        return message;
    }

    public List<ChatMessage> getSessionMessages(Integer sessionId) {
        return messageMapper.findBySessionId(sessionId);
    }

    public List<ChatMessage> getRecentMessages(Integer sessionId, int limit) {
        return messageMapper.findRecentBySessionId(sessionId, limit);
    }

    public String buildHistoryContext(Integer sessionId, int maxTurns) {
        List<ChatMessage> messages = messageMapper.findRecentBySessionId(sessionId, maxTurns * 2);
        if (messages == null || messages.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (ChatMessage msg : messages) {
            if ("user".equals(msg.getRole())) {
                sb.append("用户：").append(msg.getContent()).append("\n");
            } else if ("assistant".equals(msg.getRole())) {
                sb.append("助手：").append(msg.getContent()).append("\n");
            }
        }
        return sb.toString();
    }
}