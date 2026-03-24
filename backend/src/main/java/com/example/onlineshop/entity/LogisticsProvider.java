package com.example.onlineshop.entity;

import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 物流服务商配置
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogisticsProvider implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer providerId;
    private String providerName;
    private String providerCode;
    private Boolean enabled;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}