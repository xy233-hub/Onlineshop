package com.example.onlineshop.service;

import com.example.onlineshop.dto.response.ApiResponse;
import com.example.onlineshop.entity.Payment;
import com.example.onlineshop.entity.PurchaseIntent;
import com.example.onlineshop.mapper.PaymentMapper;
import com.example.onlineshop.mapper.PurchaseIntentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class PaymentService {

    @Autowired
    private PaymentMapper paymentMapper;

    @Autowired
    private PurchaseIntentMapper purchaseIntentMapper;

    /**
     * 创建支付记录
     */
    @Transactional
    public Payment createPayment(Integer purchaseId, Integer customerId, BigDecimal amount) {
        // 检查是否已存在支付记录
        Payment existing = paymentMapper.findByPurchaseId(purchaseId);
        if (existing != null) {
            throw new IllegalArgumentException("该订单已存在支付记录");
        }

        // 获取购买意向信息
        PurchaseIntent intent = purchaseIntentMapper.findById(purchaseId);
        if (intent == null) {
            throw new IllegalArgumentException("购买意向不存在");
        }

        // 生成支付过期时间（30 分钟后）
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(30);

        // 创建支付记录
        Payment payment = Payment.builder()
                .purchaseId(purchaseId)
                .customerId(customerId)
                .paymentAmount(amount)
                .paymentStatus("PENDING")
                .paymentMethod(null)  // 创建时不设置支付方式，允许为 NULL
                .paymentExpiry(expiryTime)
                .paymentNotes("订单创建后自动生成，待支付")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        int rows = paymentMapper.insert(payment);
        if (rows <= 0 || payment.getPaymentId() == null) {
            throw new IllegalStateException("创建支付记录失败");
        }

        // 更新购买意向的支付状态
        Map<String, Object> params = new HashMap<>();
        params.put("purchaseId", purchaseId);
        params.put("paymentStatus", "UNPAID");
        params.put("paymentVerifyToken", UUID.randomUUID().toString().replace("-", ""));
          params.put("updatedAt", LocalDateTime.now());
        purchaseIntentMapper.updatePaymentInfo(params);

        return payment;
    }



    /**
     * 处理支付成功回调
     */
    @Transactional
    public ApiResponse processPaymentSuccess(Integer paymentId, String transactionId, String paymentMethod) {
        try {
            Payment payment = paymentMapper.findById(paymentId);
            if (payment == null) {
                return new ApiResponse(404, "支付记录不存在", null);
            }

            if (!"PENDING".equals(payment.getPaymentStatus())) {
                return new ApiResponse(400, "该支付记录状态不是待支付", null);
            }

            // 检查是否已过期
            if (LocalDateTime.now().isAfter(payment.getPaymentExpiry())) {
                return new ApiResponse(400, "支付已过期", null);
            }

            // 更新支付状态（包括支付方式）
            LocalDateTime now = LocalDateTime.now();
            paymentMapper.updateStatusWithMethod(paymentId, "PAID", transactionId, paymentMethod, now, now);

            // 更新购买意向的支付状态为 PAID
            Map<String, Object> params = new HashMap<>();
            params.put("purchaseId", payment.getPurchaseId());
            params.put("paymentStatus", "PAID");
            purchaseIntentMapper.updatePaymentInfo(params);
            
            // 同时将订单状态从 CUSTOMER_ORDERED 更新为 SELLER_CONFIRMED（表示已支付，等待商家处理）
            PurchaseIntent intent = purchaseIntentMapper.findById(payment.getPurchaseId());
            if (intent != null && "CUSTOMER_ORDERED".equals(intent.getPurchaseStatus())) {
                purchaseIntentMapper.updateStatus(payment.getPurchaseId(), "SELLER_CONFIRMED", now);
            }

            // 重新查询并返回
            Payment updated = paymentMapper.findById(paymentId);
            return new ApiResponse(200, "支付成功", updated);
        } catch (Exception e) {
            log.error("处理支付成功回调失败，paymentId={}", paymentId, e);
            return new ApiResponse(500, "处理支付失败：" + e.getMessage(), null);
        }
    }



     /**
     * 更新支付方式
     */
    @Transactional
    public ApiResponse updatePaymentMethod(Integer paymentId, String paymentMethod) {
        try {
            Payment payment = paymentMapper.findById(paymentId);
            if (payment == null) {
                return new ApiResponse(404, "支付记录不存在", null);
            }

            if (!"PENDING".equals(payment.getPaymentStatus())) {
                return new ApiResponse(400, "只有待支付的订单才能选择支付方式", null);
            }

            // 检查是否已过期
            if (LocalDateTime.now().isAfter(payment.getPaymentExpiry())) {
                return new ApiResponse(400, "支付已过期", null);
            }

            // 更新支付方式
            LocalDateTime now = LocalDateTime.now();
            paymentMapper.updatePaymentMethod(paymentId, paymentMethod, now);

            // 生成模拟的支付跳转信息（实际项目中需要对接真实支付接口）
            Map<String, Object> result = new HashMap<>();
            result.put("payment_id", payment.getPaymentId());
            result.put("purchase_id", payment.getPurchaseId());
            result.put("payment_method", paymentMethod);
            result.put("payment_status", "PENDING");
            
            // 模拟第三方支付 URL（实际项目中替换为真实接口）
            if ("ALIPAY".equals(paymentMethod)) {
                result.put("pay_url", "https://openapi.alipay.com/gateway.do?trade_no=" + System.currentTimeMillis());
                result.put("qr_code", "https://qr.alipay.com/" + System.currentTimeMillis());
            } else if ("WECHAT_PAY".equals(paymentMethod)) {
                result.put("pay_url", "https://api.mch.weixin.qq.com/v3/pay/transactions/jsapi");
                result.put("qr_code", "weixin://wxpay/bizpayurl?pr=" + System.currentTimeMillis());
            } else if ("BANK_CARD".equals(paymentMethod)) {
                result.put("pay_url", "https://payment.unionpay.com/tfront/main");
            } else if ("CREDIT_CARD".equals(paymentMethod)) {
                result.put("pay_url", "https://payment.unionpay.com/tfront/main");
            }
            
            result.put("transaction_id", null);

            return new ApiResponse(200, "支付请求已受理", result);
        } catch (Exception e) {
            log.error("更新支付方式失败，paymentId={}", paymentId, e);
            return new ApiResponse(500, "处理失败：" + e.getMessage(), null);
        }
    }

    /**
     * 处理支付失败
     */
    @Transactional
    public ApiResponse processPaymentFailure(Integer paymentId, String reason) {
        try {
            Payment payment = paymentMapper.findById(paymentId);
            if (payment == null) {
                return new ApiResponse(404, "支付记录不存在", null);
            }

            LocalDateTime now = LocalDateTime.now();
            Payment updatePayment = Payment.builder()
                    .paymentStatus("FAILED")
                    .paymentNotes(reason)
                    .updatedAt(now)
                    .build();
            updatePayment.setPaymentId(paymentId);
            
            paymentMapper.updatePaymentInfo(updatePayment);

            // 更新购买意向的支付状态
            Map<String, Object> params = new HashMap<>();
            params.put("purchaseId", payment.getPurchaseId());
            params.put("paymentStatus", "UNPAID");
            purchaseIntentMapper.updatePaymentInfo(params);

            Payment updated = paymentMapper.findById(paymentId);
            return new ApiResponse(200, "支付失败已记录", updated);
        } catch (Exception e) {
            log.error("处理支付失败失败，paymentId={}", paymentId, e);
            return new ApiResponse(500, "处理支付失败：" + e.getMessage(), null);
        }
    }

    /**
     * 手动触发支付结果校验
     */
    public ApiResponse verifyPaymentResult(Integer purchaseId, Integer customerId) {
        try {
            PurchaseIntent intent = purchaseIntentMapper.findById(purchaseId);
            if (intent == null) {
                return new ApiResponse(404, "订单不存在", null);
            }

            if (!intent.getCustomerId().equals(customerId)) {
                return new ApiResponse(403, "无权操作该订单", null);
            }

            Payment payment = paymentMapper.findByPurchaseId(purchaseId);
            if (payment == null) {
                return new ApiResponse(404, "支付记录不存在", null);
            }

            Map<String, Object> result = new HashMap<>();
            
            if ("CANCELLED".equals(intent.getPurchaseStatus()) || "ORDER_CANCELLED".equals(intent.getPurchaseStatus())) {
                result.put("paymentStatus", "FAILED");
                result.put("paymentMethod", payment.getPaymentMethod());
                result.put("transactionId", payment.getTransactionId());
                result.put("paymentTime", payment.getPaymentTime());
                result.put("message", "订单已取消，支付失败");
                return new ApiResponse(200, "订单已取消", result);
            }
            
            if ("FAILED".equals(payment.getPaymentStatus())) {
                result.put("paymentStatus", "FAILED");
                result.put("paymentMethod", payment.getPaymentMethod());
                result.put("transactionId", payment.getTransactionId());
                result.put("paymentTime", payment.getPaymentTime());
                result.put("message", "支付失败");
                return new ApiResponse(200, "支付失败", result);
            }

            result.put("paymentStatus", payment.getPaymentStatus());
            result.put("paymentMethod", payment.getPaymentMethod());
            result.put("transactionId", payment.getTransactionId());
            result.put("paymentTime", payment.getPaymentTime());

            return new ApiResponse(200, "支付状态查询成功", result);
        } catch (Exception e) {
            log.error("支付结果校验失败，purchaseId={}", purchaseId, e);
            return new ApiResponse(500, "校验失败：" + e.getMessage(), null);
        }
    }

    /**
     * 处理退款
     */
    @Transactional
    public ApiResponse processRefund(Integer paymentId, BigDecimal refundAmount, String refundReason, Integer sellerId) {
        try {
            Payment payment = paymentMapper.findById(paymentId);
            if (payment == null) {
                return new ApiResponse(404, "支付记录不存在", null);
            }

            // 验证权限：需要验证卖家是否有权退款该订单
            PurchaseIntent intent = purchaseIntentMapper.findById(payment.getPurchaseId());
            if (intent == null) {
                return new ApiResponse(404, "关联订单不存在", null);
            }

            // TODO: 需要根据商品项查询卖家 ID 进行权限验证

            if (!"PAID".equals(payment.getPaymentStatus())) {
                return new ApiResponse(400, "只有已支付的订单才能退款", null);
            }

            if (refundAmount.compareTo(payment.getPaymentAmount()) > 0) {
                return new ApiResponse(400, "退款金额不能大于支付金额", null);
            }

            LocalDateTime now = LocalDateTime.now();
            paymentMapper.processRefund(paymentId, refundAmount, refundReason, now, now);

            // 更新购买意向的支付状态
            Map<String, Object> params = new HashMap<>();
            params.put("purchaseId", payment.getPurchaseId());
            params.put("paymentStatus", "REFUNDED");
            purchaseIntentMapper.updatePaymentInfo(params);

            Payment updated = paymentMapper.findById(paymentId);
            return new ApiResponse(200, "退款成功", updated);
        } catch (Exception e) {
            log.error("处理退款失败，paymentId={}", paymentId, e);
            return new ApiResponse(500, "退款失败：" + e.getMessage(), null);
        }
    }

    /**
     * 定时任务：每 5 分钟检查一次过期支付
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void scheduleCheckExpiredPayments() {
        try {
            LocalDateTime now = LocalDateTime.now();
            int count = paymentMapper.markExpiredPaymentsFailed(now);
            if (count > 0) {
                log.info("定时任务：标记了 {} 个过期支付为失败", count);
            }
        } catch (Exception e) {
            log.error("定时任务检查过期支付失败", e);
        }
    }

    /**
     * 获取客户的支付记录
     */
    public List<Payment> getPaymentsByCustomerId(Integer customerId) {
        return paymentMapper.findByCustomerId(customerId);
    }

    /**
     * 根据购买意向 ID 获取支付记录
     */
    public Payment getPaymentByPurchaseId(Integer purchaseId) {
        return paymentMapper.findByPurchaseId(purchaseId);
    }
     /**
     * 根据 ID 获取支付记录
     */
    public Payment getPaymentById(Integer paymentId) {
        return paymentMapper.findById(paymentId);
    }

    /**
     * 根据条件查询支付记录
     */
    public List<Payment> getPaymentsByCondition(Map<String, Object> params) {
        return paymentMapper.findByCondition(params);
    }
}