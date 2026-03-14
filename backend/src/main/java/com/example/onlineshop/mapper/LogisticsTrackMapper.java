package com.example.onlineshop.mapper;

import com.example.onlineshop.entity.LogisticsTrack;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface LogisticsTrackMapper {
    
    int insert(LogisticsTrack track);
    
    int insertInitialTrack(@Param("purchaseId") Integer purchaseId, @Param("content") String content);
    
    List<LogisticsTrack> findByPurchaseId(@Param("purchaseId") Integer purchaseId);
}