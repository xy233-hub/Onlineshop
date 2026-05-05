// src/main/java/com/example/onlineshop/service/AiShoppingAssistantService.java
package com.example.onlineshop.service;

import com.example.onlineshop.dto.ai.AiProductQuery;
import com.example.onlineshop.dto.response.AiAssistantProductResponse;
import com.example.onlineshop.dto.response.ProductInfoResponse;
import com.example.onlineshop.entity.ChatMessage;
import com.example.onlineshop.entity.ChatSession;
import com.example.onlineshop.entity.Product;
import com.example.onlineshop.mapper.ProductMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiShoppingAssistantService {

    private static final int MAX_TURNS = 3;

    private final ExternalAiClient externalAiClient;
    private final AiVectorRetrieverService aiVectorRetrieverService;
    private final ChatService chatService;
    private final ProductMapper productMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, MemoryChatSession> chatMemory = new ConcurrentHashMap<>();

    @Value("${ai.session.ttl-ms:1800000}")
    private long sessionTtlMs;

    public AiShoppingAssistantService(ExternalAiClient externalAiClient, AiVectorRetrieverService aiVectorRetrieverService, ChatService chatService, ProductMapper productMapper) {
        this.externalAiClient = externalAiClient;
        this.aiVectorRetrieverService = aiVectorRetrieverService;
        this.chatService = chatService;
        this.productMapper = productMapper;
    }

    public AiAssistantProductResponse recommend(String userText, Integer page, Integer size, String userId, String scene, String action, Integer sessionId, Integer productId) {
        System.out.println("[AI] recommend called, sessionId=" + sessionId + ", userId=" + userId + ", productId=" + productId);
        if (userText == null || userText.trim().isEmpty()) {
            throw new IllegalArgumentException("text 必填");
        }

        int safePage = (page != null && page > 0) ? page : 1;
        int safeSize = (size != null && size > 0) ? size : 10;
        
        ChatSession session = null;
        if (sessionId != null) {
            session = chatService.getSession(sessionId);
            System.out.println("[AI] 查询到的 session: " + (session != null ? session.getId() : "null"));
        }

        String historyContext = "";
        if (session != null) {
            historyContext = chatService.buildHistoryContext(sessionId, 3);
        }

        String productContext = buildProductContext(productId);
        System.out.println("[AI] productContext: " + (productContext.isEmpty() ? "空" : productContext.substring(0, Math.min(50, productContext.length())) + "..."));
        if (!productContext.isEmpty()) {
            historyContext = productContext + "\n" + historyContext;
        }

        String mode = resolveMode(scene, action);
        if ("auto".equals(mode)) {
            mode = externalAiClient.analyzeIntent(userText, historyContext);
            System.out.println("[AI Agent] 意图识别结果: " + mode);
        }

        AiAssistantProductResponse response = switch (mode) {
            case "chat" -> handleChat(userText, safePage, safeSize, userId, sessionId, productContext);
            case "search", "recommend" -> handleRecommend(userText, safePage, safeSize, userId, sessionId, productContext);
            case "extract", "extract_query" -> handleExtractQuery(userText, safePage, safeSize);
            default -> throw new IllegalArgumentException("scene/action 仅支持: chat, recommend, extract_query, auto");
        };

        if (sessionId != null) {
            System.out.println("[AI] 保存消息到数据库, sessionId=" + sessionId);
            chatService.addMessage(sessionId, "user", userText);
            String replyText = response.getAiDescription();
            System.out.println("[AI] AI回复长度: " + (replyText != null ? replyText.length() : 0));
            if (replyText != null && !replyText.isBlank()) {
                chatService.addMessage(sessionId, "assistant", replyText);
                System.out.println("[AI] 消息已保存到数据库");
            }
        } else {
            System.out.println("[AI] sessionId 为空，不保存消息");
        }

        return response;
    }

    private AiAssistantProductResponse handleChat(String userText, int safePage, int safeSize, String userId, Integer sessionId, String productContext) {
        String memoryKey = buildMemoryKey(userId);

        evictExpiredSessions();
        String historyContext = buildHistoryContext(memoryKey);
        
        if (productContext != null && !productContext.isEmpty()) {
            historyContext = productContext + "\n" + historyContext;
        }
        
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

    private AiAssistantProductResponse handleRecommend(String userText, int safePage, int safeSize, String userId, Integer sessionId, String productContext) {
        String memoryKey = buildMemoryKey(userId);
        evictExpiredSessions();
        String historyContext = buildRecentUserContext(memoryKey, 3);

        if (productContext != null && !productContext.isEmpty()) {
            historyContext = productContext + "\n" + historyContext;
        }

        AiProductQuery query = externalAiClient.extractQuery(userText, historyContext);
        if (query == null) {
            query = new AiProductQuery();
            query.setQ(userText);
        }

        if (query.getQ() == null || query.getQ().isBlank()) query.setQ(userText);
        if (query.getStatus() == null || query.getStatus().isBlank()) query.setStatus("online");
        query.setPage(safePage);
        query.setSize(safeSize);

        AiVectorRetrieverService.RetrievalResult retrieval = aiVectorRetrieverService.retrieve(query, userText, historyContext, safePage, safeSize);
        List<ProductInfoResponse> items = retrieval.items().stream().map(sp -> {
            ProductInfoResponse item = new ProductInfoResponse(sp.product());
            item.score = sp.score();
            return item;
        }).toList();

        List<Product> retrievedProducts = retrieval.items().stream().map(AiVectorRetrieverService.ScoredProduct::product).toList();
        String summaryJson = buildProductsSummaryJson(retrievedProducts);
        String aiDescription = externalAiClient.generateDescription(userText, summaryJson);
        if (aiDescription == null || aiDescription.isBlank()) {
            aiDescription = retrieval.total() > 0 ? "已为你筛选到更匹配的商品，可以按价格、成色和发布时间进一步对比。" :
                    "暂时没有找到特别匹配的商品，建议补充预算、品牌或用途后再试。";
        }

        appendTurn(memoryKey, userText, aiDescription);
        return new AiAssistantProductResponse(query, aiDescription, safePage, safeSize, retrieval.total(), items);
    }

    private AiAssistantProductResponse handleExtractQuery(String userText, int safePage, int safeSize) {
        AiProductQuery query = externalAiClient.extractQuery(userText);
        if (query == null) {
            query = new AiProductQuery();
            query.setQ(userText);
        }
        query.setPage(safePage);
        query.setSize(safeSize);

        return new AiAssistantProductResponse(query, "已完成查询条件提取", safePage, safeSize, 0, Collections.emptyList());
    }

    private String resolveMode(String scene, String action) {
        String raw = (scene != null && !scene.isBlank()) ? scene : action;
        if (raw == null || raw.isBlank()) return "auto";

        String mode = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        if ("conversation".equals(mode)) return "chat";
        if ("rag".equals(mode) || "search".equals(mode)) return "recommend";
        return mode;
    }

    private String buildProductsSummaryJson(List<Product> products) {
        if (products == null || products.isEmpty()) return "[]";
        List<Map<String, Object>> summary = new ArrayList<>();
        for (Product p : products) {
            Map<String, Object> row = new HashMap<>();
            row.put("product_id", p.getProductId());
            row.put("product_name", p.getProductName());
            row.put("price", p.getPrice());
            row.put("short_desc", p.getShortDesc());
            summary.add(row);
        }

        try {
            return objectMapper.writeValueAsString(summary);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String buildMemoryKey(String userId) {
        if (userId == null || userId.isBlank()) return "anonymous";
        return userId.trim();
    }

    private String buildHistoryContext(String memoryKey) {
        MemoryChatSession session = chatMemory.get(memoryKey);
        if (session == null || session.turns().isEmpty()) return "";

        session.touch(System.currentTimeMillis());
        List<String> lines = new ArrayList<>();
        for (ChatTurn turn : session.turns()) {
            lines.add("用户：" + turn.userText());
            lines.add("助手：" + turn.assistantText());
        }
        return String.join("\n", lines);
    }

    private String buildRecentUserContext(String memoryKey, int maxUserTurns) {
        MemoryChatSession session = chatMemory.get(memoryKey);
        if (session == null || session.turns().isEmpty()) return "";

        session.touch(System.currentTimeMillis());
        List<String> recent = new ArrayList<>();
        for (ChatTurn turn : session.turns()) {
            String text = turn.userText();
            if (text != null && !text.isBlank()) recent.add(text);
        }

        int from = Math.max(0, recent.size() - Math.max(maxUserTurns, 1));
        return String.join("\n", recent.subList(from, recent.size()));
    }

    private void appendTurn(String memoryKey, String userText, String assistantText) {
        long now = System.currentTimeMillis();
        chatMemory.compute(memoryKey, (k, oldSession) -> {
            MemoryChatSession session = oldSession == null ? new MemoryChatSession(new ArrayDeque<>(), now) : oldSession;
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

    private String buildProductContext(Integer productId) {
        if (productId == null) {
            return "";
        }
        Product product = productMapper.findById(productId);
        if (product == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("【当前商品信息】\n");
        sb.append("商品ID：").append(product.getProductId()).append("\n");
        sb.append("商品名称：").append(product.getProductName()).append("\n");
        if (product.getProductDesc() != null && !product.getProductDesc().isEmpty()) {
            sb.append("商品描述：").append(product.getProductDesc()).append("\n");
        }
        sb.append("价格：¥").append(product.getPrice()).append("\n");
        if (product.getOriginalPrice() != null && product.getOriginalPrice().compareTo(product.getPrice()) > 0) {
            sb.append("原价：¥").append(product.getOriginalPrice()).append("\n");
        }
        if (product.getStockQuantity() != null) {
            sb.append("库存：").append(product.getStockQuantity()).append("\n");
        }
        if (product.getProductStatus() != null) {
            sb.append("状态：").append(product.getProductStatus()).append("\n");
        }
        return sb.toString();
    }

    private record ChatTurn(String userText, String assistantText) {}

    private static final class MemoryChatSession {
        private final Deque<ChatTurn> turns;
        private volatile long lastAccessAt;

        private MemoryChatSession(Deque<ChatTurn> turns, long lastAccessAt) {
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
