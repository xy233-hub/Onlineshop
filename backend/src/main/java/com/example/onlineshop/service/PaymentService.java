package com.example.onlineshop.service;

import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayApiException;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.internal.util.AlipaySignature;
import com.example.onlineshop.config.AlipayProperties;
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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private AlipayProperties alipayProperties;

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

            if ("PAID".equals(payment.getPaymentStatus())) {
                return new ApiResponse(200, "支付已处理", payment);
            }

            if (!"PENDING".equals(payment.getPaymentStatus())) {
                return new ApiResponse(400, "该支付记录状态不是待支付", null);
            }

            if (payment.getPaymentExpiry() != null && LocalDateTime.now().isAfter(payment.getPaymentExpiry())) {
                return new ApiResponse(400, "支付已过期", null);
            }

            // 更新支付状态（包括支付方式）
            LocalDateTime now = LocalDateTime.now();
            paymentMapper.updateStatusWithMethod(paymentId, "PAID", transactionId, paymentMethod, now, now);

            // 获取所有关联的购买意向 ID
            List<Integer> allPurchaseIds = getAllPurchaseIdsByPayment(payment);
            
            // 更新所有关联订单的支付状态为 PAID
            for (Integer purchaseId : allPurchaseIds) {
                Map<String, Object> params = new HashMap<>();
                params.put("purchaseId", purchaseId);
                params.put("paymentStatus", "PAID");
                params.put("updatedAt", now);
                purchaseIntentMapper.updatePaymentInfo(params);
                
                // 同时将订单状态从 CUSTOMER_ORDERED 更新为 SELLER_CONFIRMED
                PurchaseIntent intent = purchaseIntentMapper.findById(purchaseId);
                if (intent != null && "CUSTOMER_ORDERED".equals(intent.getPurchaseStatus())) {
                    purchaseIntentMapper.updateStatus(purchaseId, "SELLER_CONFIRMED", now);
                }
            }

            // 重新查询并返回
            Payment updated = paymentMapper.findById(paymentId);
            return new ApiResponse(200, "支付成功，共更新 " + allPurchaseIds.size() + " 个订单", updated);
        } catch (Exception e) {
            log.error("处理支付成功回调失败，paymentId={}", paymentId, e);
            return new ApiResponse(500, "处理支付失败：" + e.getMessage(), null);
        }
    }

    /**
     * 根据支付记录获取所有关联的购买意向 ID
     */
    private List<Integer> getAllPurchaseIdsByPayment(Payment payment) {
        List<Integer> result = new ArrayList<>();
        
        // 首先添加主关联的 purchaseId
        if (payment.getPurchaseId() != null) {
            result.add(payment.getPurchaseId());
        }
        
        // 从 paymentNotes 中解析批次信息
        // 格式："批量订单合并支付，共 X 个订单"
        if (payment.getPaymentNotes() != null && payment.getPaymentNotes().contains("批量订单合并支付")) {
            // 查询该客户的所有 UNPAID 状态的订单（同一批次创建）
            // 通过时间范围来限定：支付记录创建时间的前后 1 分钟内
            LocalDateTime startTime = payment.getCreatedAt().minusMinutes(1);
            LocalDateTime endTime = payment.getCreatedAt().plusMinutes(1);
            
            List<PurchaseIntent> intents = purchaseIntentMapper.findByCustomerIdAndTimeRange(
                payment.getCustomerId(), 
                startTime,
                endTime
            );
            
            if (intents != null) {
                for (PurchaseIntent intent : intents) {
                    // 只添加尚未支付的订单，并且不重复添加主订单
                    if (!result.contains(intent.getPurchaseId()) && 
                        ("UNPAID".equals(intent.getPaymentStatus()) || "PENDING".equals(intent.getPaymentStatus()))) {
                        result.add(intent.getPurchaseId());
                    }
                }
            }
        }
        
        return result;
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

            if (payment.getPaymentExpiry() != null && LocalDateTime.now().isAfter(payment.getPaymentExpiry())) {
                return new ApiResponse(400, "支付已过期", null);
            }

            LocalDateTime now = LocalDateTime.now();
            paymentMapper.updatePaymentMethod(paymentId, paymentMethod, now);

            Map<String, Object> result = new HashMap<>();
            result.put("payment_id", payment.getPaymentId());
            result.put("purchase_id", payment.getPurchaseId());
            result.put("payment_method", paymentMethod);
            result.put("payment_status", "PENDING");
            result.put("transaction_id", null);

            if ("ALIPAY".equals(paymentMethod)) {
                String outTradeNo = buildOutTradeNo(payment.getPaymentId());
                String payForm = buildAlipayPageForm(payment, outTradeNo);
                result.put("out_trade_no", outTradeNo);
                result.put("pay_type", "FORM");
                result.put("pay_form", payForm);
            } else if ("WECHAT_PAY".equals(paymentMethod)) {
                result.put("pay_url", "https://api.mch.weixin.qq.com/v3/pay/transactions/jsapi");
                result.put("qr_code", "weixin://wxpay/bizpayurl?pr=" + System.currentTimeMillis());
            } else if ("BANK_CARD".equals(paymentMethod) || "CREDIT_CARD".equals(paymentMethod)) {
                result.put("pay_url", "https://payment.unionpay.com/tfront/main");
            }

            return new ApiResponse(200, "支付请求已受理", result);
        } catch (AlipayApiException e) {
            log.error("生成支付宝支付表单失败，paymentId={}", paymentId, e);
            return new ApiResponse(500, "支付宝下单失败：" + e.getErrMsg(), null);
        } catch (Exception e) {
            log.error("更新支付方式失败，paymentId={}", paymentId, e);
            return new ApiResponse(500, "处理失败：" + e.getMessage(), null);
        }
    }

    @Transactional
    public boolean handleAlipayNotify(Map<String, String> params) {
        try {
            if (!verifyAlipaySign(params)) {
                log.warn("支付宝回调验签失败，params={}", params);
                return false;
            }

            String appId = params.get("app_id");
            if (appId == null || !appId.equals(alipayProperties.getAppId())) {
                log.warn("支付宝回调 app_id 不匹配，app_id={}", appId);
                return false;
            }

            String tradeStatus = params.get("trade_status");
            if (!("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus))) {
                return true;
            }

            String outTradeNo = params.get("out_trade_no");
            Integer paymentId = parsePaymentId(outTradeNo);
            if (paymentId == null) {
                log.warn("无法解析 paymentId，out_trade_no={}", outTradeNo);
                return false;
            }

            Payment payment = paymentMapper.findById(paymentId);
            if (payment == null) {
                log.warn("支付宝回调对应支付记录不存在，paymentId={}", paymentId);
                return false;
            }

            String totalAmount = params.get("total_amount");
            if (totalAmount != null && payment.getPaymentAmount() != null) {
                BigDecimal callbackAmount = new BigDecimal(totalAmount);
                if (callbackAmount.compareTo(payment.getPaymentAmount()) != 0) {
                    log.warn("支付宝回调金额不匹配，paymentId={}, db={}, callback={}", paymentId, payment.getPaymentAmount(), callbackAmount);
                    return false;
                }
            }

            if ("PAID".equals(payment.getPaymentStatus())) {
                return true;
            }

            String tradeNo = params.get("trade_no");
            ApiResponse resp = processPaymentSuccess(paymentId, tradeNo, "ALIPAY");
            return resp.getCode() == 200;
        } catch (Exception e) {
            log.error("处理支付宝回调异常", e);
            return false;
        }
    }

    public ApiResponse handleAlipayReturn(Map<String, String> params) {
        try {
            if (!verifyAlipaySign(params)) {
                return new ApiResponse(400, "支付宝回跳验签失败", null);
            }

            String outTradeNo = params.get("out_trade_no");
            Integer paymentId = parsePaymentId(outTradeNo);
            if (paymentId == null) {
                return new ApiResponse(400, "无效的 out_trade_no", null);
            }

            Payment payment = paymentMapper.findById(paymentId);
            if (payment == null) {
                return new ApiResponse(404, "支付记录不存在", null);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("payment_id", payment.getPaymentId());
            data.put("out_trade_no", outTradeNo);
            data.put("trade_no", params.get("trade_no"));
            data.put("trade_status", params.get("trade_status"));
            data.put("payment_status", payment.getPaymentStatus());
            return new ApiResponse(200, "回跳验签成功", data);
        } catch (Exception e) {
            log.error("处理支付宝回跳失败", e);
            return new ApiResponse(500, "处理失败：" + e.getMessage(), null);
        }
    }

    private String buildAlipayPageForm(Payment payment, String outTradeNo) throws AlipayApiException {
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(alipayProperties.getNotifyUrl());
        request.setReturnUrl(alipayProperties.getReturnUrl());

        String totalAmount = payment.getPaymentAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();
        String subject = "在线商城订单支付-" + payment.getPurchaseId();
        String body = "paymentId=" + payment.getPaymentId() + ",purchaseId=" + payment.getPurchaseId();

        String bizContent = "{" +
                "\"out_trade_no\":\"" + outTradeNo + "\"," +
                "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"," +
                "\"total_amount\":\"" + totalAmount + "\"," +
                "\"subject\":\"" + subject + "\"," +
                "\"body\":\"" + body + "\"" +
                "}";

        request.setBizContent(bizContent);
        AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
        if (!response.isSuccess()) {
            throw new AlipayApiException("alipay pageExecute failed: " + response.getSubMsg());
        }
        return response.getBody();
    }

    private boolean verifyAlipaySign(Map<String, String> params) throws AlipayApiException {
        Map<String, String> normalized = new HashMap<>();
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                String value = entry.getValue();
                // Frontend/URL decode often turns '+' in sign into spaces; convert back before verify.
                if ("sign".equals(entry.getKey())) {
                    value = value.replace(" ", "+");
                }
                normalized.put(entry.getKey(), value);
            }
        }

        return AlipaySignature.rsaCheckV1(
                normalized,
                alipayProperties.getAlipayPublicKey(),
                alipayProperties.getCharset(),
                alipayProperties.getSignType()
        );
    }

    private String buildOutTradeNo(Integer paymentId) {
        return "PAY_" + paymentId;
    }

    private Integer parsePaymentId(String outTradeNo) {
        if (outTradeNo == null || outTradeNo.isBlank()) return null;
        String normalized = outTradeNo.startsWith("PAY_") ? outTradeNo.substring(4) : outTradeNo;
        try {
            return Integer.valueOf(normalized);
        } catch (NumberFormatException e) {
            return null;
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

    /**
     * 创建批量支付记录（多个订单合并支付）
     */
    @Transactional
    public Payment createBatchPayment(List<Integer> purchaseIds, Integer customerId, BigDecimal totalAmount) {
        if (purchaseIds == null || purchaseIds.isEmpty()) {
            throw new IllegalArgumentException("购买意向 ID 列表不能为空");
        }

        // 验证所有购买意向都属于该客户
        for (Integer purchaseId : purchaseIds) {
            PurchaseIntent intent = purchaseIntentMapper.findById(purchaseId);
            if (intent == null) {
                throw new IllegalArgumentException("购买意向不存在：" + purchaseId);
            }
            if (!intent.getCustomerId().equals(customerId)) {
                throw new IllegalArgumentException("无权为该订单创建支付记录：" + purchaseId);
            }
        }

        // 生成支付过期时间（30 分钟后）
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(30);

        // 创建支付记录（第一个 purchase_id作为主关联）
        Payment payment = Payment.builder()
                .purchaseId(purchaseIds.get(0))
                .customerId(customerId)
                .paymentAmount(totalAmount)
                .paymentStatus("PENDING")
                .paymentMethod(null)
                .paymentExpiry(expiryTime)
                .paymentNotes("批量订单合并支付，共 " + purchaseIds.size() + " 个订单")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        int rows = paymentMapper.insert(payment);
        if (rows <= 0 || payment.getPaymentId() == null) {
            throw new IllegalStateException("创建批量支付记录失败");
        }

        // 更新所有购买意向的支付状态
        Map<String, Object> params = new HashMap<>();
        params.put("paymentStatus", "UNPAID");
        params.put("paymentVerifyToken", UUID.randomUUID().toString().replace("-", ""));
        params.put("updatedAt", LocalDateTime.now());

        for (Integer purchaseId : purchaseIds) {
            params.put("purchaseId", purchaseId);
            purchaseIntentMapper.updatePaymentInfo(params);
        }

        return payment;
    }
}

