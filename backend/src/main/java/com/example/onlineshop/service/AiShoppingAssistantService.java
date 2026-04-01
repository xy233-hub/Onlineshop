// src/main/java/com/example/onlineshop/service/AiShoppingAssistantService.java
package com.example.onlineshop.service;

import com.example.onlineshop.dto.response.AiAssistantProductResponse;
import com.example.onlineshop.dto.ai.AiProductQuery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiShoppingAssistantService {

    private static final int MAX_TURNS = 3;

    private final ExternalAiClient externalAiClient;
    private final Map<String, ChatSession> chatMemory = new ConcurrentHashMap<>();

    @Value("${ai.session.ttl-ms:1800000}")
    private long sessionTtlMs;

    public AiShoppingAssistantService(ExternalAiClient externalAiClient) {
        this.externalAiClient = externalAiClient;
    }

    public AiAssistantProductResponse recommend(String userText, Integer page, Integer size, String userId) {
        if (userText == null || userText.trim().isEmpty()) {
            throw new IllegalArgumentException("text 必填");
        }

        int safePage = (page != null && page > 0) ? page : 1;
        int safeSize = (size != null && size > 0) ? size : 10;
        String memoryKey = buildMemoryKey(userId);

        evictExpiredSessions();
        String historyContext = buildHistoryContext(memoryKey);
        String aiReply = externalAiClient.generateChatReply(userText, historyContext);
        if (aiReply == null || aiReply.isBlank()) {
            aiReply = "我明白你的需求了，可以再补充下预算、用途或偏好，我会给你更准确的建议。";
        }

        appendTurn(memoryKey, userText, aiReply);

        AiProductQuery query = new AiProductQuery();
        query.setQ(userText);
        query.setPage(safePage);
        query.setSize(safeSize);

        return new AiAssistantProductResponse(query, aiReply, safePage, safeSize, 0, Collections.emptyList());
    }

    private String buildMemoryKey(String userId) {
        if (userId == null || userId.isBlank()) return "anonymous";
        return userId.trim();
    }

    private String buildHistoryContext(String memoryKey) {
        ChatSession session = chatMemory.get(memoryKey);
        if (session == null || session.turns().isEmpty()) return "";

        session.touch(System.currentTimeMillis());
        List<String> lines = new ArrayList<>();
        for (ChatTurn turn : session.turns()) {
            lines.add("用户：" + turn.userText());
            lines.add("助手：" + turn.assistantText());
        }
        return String.join("\n", lines);
    }

    private void appendTurn(String memoryKey, String userText, String assistantText) {
        long now = System.currentTimeMillis();
        chatMemory.compute(memoryKey, (k, oldSession) -> {
            ChatSession session = oldSession == null ? new ChatSession(new ArrayDeque<>(), now) : oldSession;
            Deque<ChatTurn> turns = session.turns();
            turns.addLast(new ChatTurn(userText.trim(), assistantText.trim()));
            while (turns.size() > MAX_TURNS) {
                turns.removeFirst();
            }
            session.touch(now);
            return session;
        });
    }

    private void evictExpiredSessions() {
        long now = System.currentTimeMillis();
        long ttl = Math.max(sessionTtlMs, 60_000L);
        chatMemory.entrySet().removeIf(e -> (now - e.getValue().lastAccessAt()) > ttl);
    }

    private record ChatTurn(String userText, String assistantText) {}

    private static final class ChatSession {
        private final Deque<ChatTurn> turns;
        private volatile long lastAccessAt;

        private ChatSession(Deque<ChatTurn> turns, long lastAccessAt) {
            this.turns = turns;
            this.lastAccessAt = lastAccessAt;
        }

        private Deque<ChatTurn> turns() {
            return turns;
        }

        private long lastAccessAt() {
            return lastAccessAt;
        }

        private void touch(long ts) {
            this.lastAccessAt = ts;
        }
    }
}
