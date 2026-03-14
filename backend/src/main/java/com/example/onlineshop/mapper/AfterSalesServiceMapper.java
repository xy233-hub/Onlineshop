package com.example.onlineshop.mapper;

import com.example.onlineshop.entity.AfterSalesService;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AfterSalesServiceMapper {
    int insert(AfterSalesService afterSalesService);
    
    int update(AfterSalesService afterSalesService);
    
    AfterSalesService selectByServiceId(@Param("serviceId") Integer serviceId);
    
    List<AfterSalesService> selectByCustomerId(@Param("customerId") Integer customerId,
                                               @Param("serviceStatus") String serviceStatus,
                                               @Param("offset") Integer offset,
                                               @Param("size") Integer size);
    
    List<AfterSalesService> selectBySellerId(@Param("sellerId") Integer sellerId,
                                             @Param("serviceStatus") String serviceStatus,
                                             @Param("serviceType") String serviceType,
                                             @Param("offset") Integer offset,
                                             @Param("limit") Integer limit);
    
    int countByCustomerId(@Param("customerId") Integer customerId,
                          @Param("serviceStatus") String serviceStatus);
    
    int countBySellerId(@Param("sellerId") Integer sellerId,
                        @Param("serviceStatus") String serviceStatus,
                        @Param("serviceType") String serviceType);
    AfterSalesService selectByPurchaseIdAndCustomerId(@Param("purchaseId") Integer purchaseId,
                                                       @Param("customerId") Integer customerId);                    
}