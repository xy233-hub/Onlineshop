package com.example.onlineshop.service;

import com.example.onlineshop.dto.request.PriceChangeRequest;
import com.example.onlineshop.dto.response.PriceHistoryResponse;
import com.example.onlineshop.dto.response.PriceSummaryResponse;

import java.util.Map;

public interface PriceService {
    
    PriceHistoryResponse getPriceHistory(Integer productId, Integer days, 
                                         String startDate, String endDate,
                                         Integer page, Integer size);
    
    PriceSummaryResponse getPriceSummary(Integer productId);
    
    Map<String, Object> updateProductPrice(Integer productId, PriceChangeRequest request, Integer sellerId);
}