package com.example.onlineshop.service.impl;

import com.example.onlineshop.entity.LogisticsTrack;
import com.example.onlineshop.mapper.LogisticsTrackMapper;
import com.example.onlineshop.service.LogisticsTrackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LogisticsTrackServiceImpl implements LogisticsTrackService {

    @Autowired
    private LogisticsTrackMapper logisticsTrackMapper;

    @Override
    @Transactional
    public void addTrack(LogisticsTrack track) {
        track.setTrackTime(LocalDateTime.now());
        track.setCreatedAt(LocalDateTime.now());
        logisticsTrackMapper.insert(track);
    }

    @Override
    public List<LogisticsTrack> getTracksByPurchaseId(Integer purchaseId) {
        return logisticsTrackMapper.findByPurchaseId(purchaseId);
    }

    @Override
    @Transactional
    public void addManualTrack(Integer purchaseId, String content, String location, String status) {
        LogisticsTrack track = LogisticsTrack.builder()
                .purchaseId(purchaseId)
                .trackTime(LocalDateTime.now())
                .trackContent(content)
                .trackLocation(location)
                .trackStatus(status)
                .createdAt(LocalDateTime.now())
                .build();
        logisticsTrackMapper.insert(track);
    }
}