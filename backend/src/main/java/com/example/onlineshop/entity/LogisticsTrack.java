package com.example.onlineshop.entity;

import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogisticsTrack implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer trackId;
    private Integer purchaseId;
    private LocalDateTime trackTime;
    private String trackContent;
    private String trackLocation;
    private String trackStatus;
    private LocalDateTime createdAt;
}