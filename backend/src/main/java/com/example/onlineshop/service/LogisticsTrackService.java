package com.example.onlineshop.service;

import com.example.onlineshop.entity.LogisticsTrack;
import java.util.List;

public interface LogisticsTrackService {
    
    void addTrack(LogisticsTrack track);
    
    List<LogisticsTrack> getTracksByPurchaseId(Integer purchaseId);
    
    void addManualTrack(Integer purchaseId, String content, String location, String status);
}