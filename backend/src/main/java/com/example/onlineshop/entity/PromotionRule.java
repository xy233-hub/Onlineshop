package com.example.onlineshop.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionRule {
    private Integer ruleId;
    private Integer promotionId;
    private String ruleType;
    private String ruleConfig;
    private LocalDateTime createdAt;
}

