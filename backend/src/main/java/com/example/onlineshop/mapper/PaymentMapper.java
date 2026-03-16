package com.example.onlineshop.mapper;

import com.example.onlineshop.entity.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface PaymentMapper {

    /**
     * 根据 ID 查找支付记录
     */
    Payment findById(@Param("paymentId") Integer paymentId);

    /**
     * 根据购买意向 ID 查找支付记录
     */
    Payment findByPurchaseId(@Param("purchaseId") Integer purchaseId);

    /**
     * 根据客户 ID 查找支付记录列表
     */
    List<Payment> findByCustomerId(@Param("customerId") Integer customerId);

    /**
     * 插入支付记录
     */
    int insert(Payment payment);

    /**
     * 更新支付状态
     */
    int updateStatus(@Param("paymentId") Integer paymentId,
                     @Param("paymentStatus") String paymentStatus,
                     @Param("transactionId") String transactionId,
                     @Param("paymentTime") LocalDateTime paymentTime,
                     @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 更新支付信息（包括过期时间等）
     */
    int updatePaymentInfo(Payment payment);

    /**
     * 标记过期支付为失败
     */
    int markExpiredPaymentsFailed(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 处理退款
     */
    int processRefund(@Param("paymentId") Integer paymentId,
                      @Param("refundAmount") BigDecimal refundAmount,
                      @Param("refundReason") String refundReason,
                      @Param("refundTime") LocalDateTime refundTime,
                      @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 根据条件查询支付记录
     */
    List<Payment> findByCondition(Map<String, Object> params);

    /**
     * 统计符合条件的支付记录数
     */
    int countByCondition(Map<String, Object> params);

    /**
     * 查找即将过期的支付记录
     */
    List<Payment> findExpiringPayments(@Param("minutes") Integer minutes,
                                       @Param("currentTime") LocalDateTime currentTime);
    

   
    /**
     * 更新支付方式
     */
    int updatePaymentMethod(@Param("paymentId") Integer paymentId,
                            @Param("paymentMethod") String paymentMethod,
                            @Param("updatedAt") LocalDateTime updatedAt);

                          
     /**
     * 更新支付状态（包括支付方式）
     */
    int updateStatusWithMethod(@Param("paymentId") Integer paymentId,
                               @Param("paymentStatus") String paymentStatus,
                               @Param("transactionId") String transactionId,
                               @Param("paymentMethod") String paymentMethod,
                               @Param("paymentTime") LocalDateTime paymentTime,
                               @Param("updatedAt") LocalDateTime updatedAt);
                  
}