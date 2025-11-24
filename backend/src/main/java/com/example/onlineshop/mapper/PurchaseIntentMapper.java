// mapper/PurchaseIntentMapper.java
package com.example.onlineshop.mapper;

import com.example.onlineshop.entity.PurchaseIntent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface PurchaseIntentMapper {

    int insert(PurchaseIntent purchaseIntent);

    List<PurchaseIntent> findByProductId(@Param("productId") Integer productId);

    List<PurchaseIntent> findByCustomerId(@Param("customerId") Integer customerId);

    PurchaseIntent findById(@Param("purchaseId") Integer purchaseId);

    int updateStatus(@Param("purchaseId") Integer purchaseId, @Param("status") String status);

    int updateStatus(@Param("purchaseId") Integer purchaseId,
            @Param("status") String status,
            @Param("updatedAt") LocalDateTime updatedAt);

    List<PurchaseIntent> findAll();

    // 批量将同一商品下除指定意向外的pending意向设为failed
    int markOtherIntentsFailed(Integer productId, Integer excludePurchaseId);

    int updateStatusAndNotes(@Param("purchaseId") Integer purchaseId,
            @Param("status") String status,
            @Param("sellerNotes") String sellerNotes,
            @Param("updatedAt") LocalDateTime updatedAt);

    // 按条件查询购买意向
    List<PurchaseIntent> findByCondition(Map<String, Object> params);

    // 按条件统计购买意向数量
    int countByCondition(Map<String, Object> params);

    // 更新订单状态和取消信息
    int updateStatusWithCancelInfo(Map<String, Object> params);
}
