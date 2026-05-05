package com.example.onlineshop.controller;

import com.example.onlineshop.entity.ChatMessage;
import com.example.onlineshop.entity.ChatSession;
import com.example.onlineshop.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/sessions")
    public ResponseEntity<?> createSession(
            @RequestParam String userId,
            @RequestParam(required = false) String name) {
        ChatSession session = chatService.createSession(userId, name);
        return ResponseEntity.ok(Map.of("code", 200, "data", session));
    }

    @GetMapping("/sessions")
    public ResponseEntity<?> getUserSessions(@RequestParam String userId) {
        List<ChatSession> sessions = chatService.getUserSessions(userId);
        return ResponseEntity.ok(Map.of("code", 200, "data", sessions));
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<?> getSession(@PathVariable Integer sessionId) {
        ChatSession session = chatService.getSession(sessionId);
        if (session == null) {
            return ResponseEntity.badRequest().body(Map.of("code", 404, "message", "会话不存在"));
        }
        return ResponseEntity.ok(Map.of("code", 200, "data", session));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<?> getSessionMessages(@PathVariable Integer sessionId) {
        List<ChatMessage> messages = chatService.getSessionMessages(sessionId);
        return ResponseEntity.ok(Map.of("code", 200, "data", messages));
    }

    @PutMapping("/sessions/{sessionId}")
    public ResponseEntity<?> renameSession(
            @PathVariable Integer sessionId,
            @RequestParam String userId,
            @RequestParam String name) {
        ChatSession session = chatService.getSession(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            return ResponseEntity.badRequest().body(Map.of("code", 403, "message", "无权限"));
        }
        chatService.renameSession(sessionId, name);
        return ResponseEntity.ok(Map.of("code", 200, "message", "修改成功"));
    }

    @PostMapping("/sessions/{sessionId}/rename-with-ai")
    public ResponseEntity<?> renameSessionWithAi(
            @PathVariable Integer sessionId,
            @RequestParam String userId) {
        String newName = chatService.renameSessionWithAi(sessionId, userId);
        if (newName == null) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", "重命名失败"));
        }
        return ResponseEntity.ok(Map.of("code", 200, "data", Map.of("name", newName)));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<?> deleteSession(
            @PathVariable Integer sessionId,
            @RequestParam String userId) {
        ChatSession session = chatService.getSession(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            return ResponseEntity.badRequest().body(Map.of("code", 403, "message", "无权限"));
        }
        chatService.deleteSession(sessionId);
        return ResponseEntity.ok(Map.of("code", 200, "message", "删除成功"));
    }
}