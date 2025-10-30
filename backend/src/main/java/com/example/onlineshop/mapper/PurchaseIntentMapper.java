// mapper/PurchaseIntentMapper.java
package com.example.onlineshop.mapper;

import com.example.onlineshop.entity.PurchaseIntent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PurchaseIntentMapper {

    int insert(PurchaseIntent purchaseIntent);

    List<PurchaseIntent> findByProductId(@Param("productId") Integer productId);

    PurchaseIntent findById(@Param("purchaseId") Integer purchaseId);

    int updateStatus(@Param("purchaseId") Integer purchaseId, @Param("status") String status);

    List<PurchaseIntent> findAll();
    // 批量将同一商品下除指定意向外的pending意向设为failed
    int markOtherIntentsFailed(Integer productId, Integer excludePurchaseId);

    int updateStatusAndNotes(@Param("purchaseId") Integer purchaseId,
                             @Param("status") String status,
                             @Param("sellerNotes") String sellerNotes,
                             @Param("updatedAt") LocalDateTime updatedAt);

}
