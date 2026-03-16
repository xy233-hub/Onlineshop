package com.example.onlineshop.mapper;

import com.example.onlineshop.entity.LogisticsProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface LogisticsProviderMapper {
    
    List<LogisticsProvider> findAll();
    
    List<LogisticsProvider> findByType(@Param("type") String type);
    
    LogisticsProvider findById(@Param("providerId") Integer providerId);
    
    LogisticsProvider findByCode(@Param("providerCode") String providerCode);
}