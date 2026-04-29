package com.example.onlineshop.mapper;

import com.example.onlineshop.entity.ProductPriceHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductPriceHistoryMapper {
    
    int insert(ProductPriceHistory history);
    
    List<ProductPriceHistory> selectByProductId(@Param("productId") Integer productId,
                                                 @Param("startDate") String startDate,
                                                 @Param("endDate") String endDate,
                                                 @Param("offset") Integer offset,
                                                 @Param("limit") Integer limit);
    
    int countByProductId(@Param("productId") Integer productId,
                         @Param("startDate") String startDate,
                         @Param("endDate") String endDate);
    
    ProductPriceHistory selectLatestByProductId(@Param("productId") Integer productId);
}