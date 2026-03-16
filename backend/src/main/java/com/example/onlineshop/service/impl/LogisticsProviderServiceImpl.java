package com.example.onlineshop.service.impl;

import com.example.onlineshop.entity.LogisticsProvider;
import com.example.onlineshop.mapper.LogisticsProviderMapper;
import com.example.onlineshop.service.LogisticsProviderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LogisticsProviderServiceImpl implements LogisticsProviderService {

    @Autowired
    private LogisticsProviderMapper logisticsProviderMapper;

    @Override
    public List<LogisticsProvider> getAllProviders() {
        return logisticsProviderMapper.findAll();
    }

    @Override
    public List<LogisticsProvider> getProvidersByType(String type) {
        return logisticsProviderMapper.findByType(type);
    }

    @Override
    public LogisticsProvider getProviderById(Integer providerId) {
        return logisticsProviderMapper.findById(providerId);
    }

    @Override
    public LogisticsProvider getProviderByCode(String code) {
        return logisticsProviderMapper.findByCode(code);
    }
}