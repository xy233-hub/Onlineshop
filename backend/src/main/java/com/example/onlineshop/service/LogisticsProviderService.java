package com.example.onlineshop.service;

import com.example.onlineshop.entity.LogisticsProvider;
import java.util.List;

public interface LogisticsProviderService {
    
    List<LogisticsProvider> getAllProviders();
    
    List<LogisticsProvider> getProvidersByType(String type);
    
    LogisticsProvider getProviderById(Integer providerId);
    
    LogisticsProvider getProviderByCode(String code);
}