package com.example.onlineshop.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {
    private Integer id;
    private String userId;
    private String sessionName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}