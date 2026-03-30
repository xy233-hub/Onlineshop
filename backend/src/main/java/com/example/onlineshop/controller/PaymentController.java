package com.example.onlineshop.controller;

import com.example.onlineshop.dto.response.ApiResponse;
import com.example.onlineshop.entity.Payment;
import com.example.onlineshop.service.PaymentService;
import com.example.onlineshop.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    /**
     * POST /api/payments/create
     * 创建支付记录
     */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse> createPayment(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request
    ) {
        try {
            Integer customerId = JwtUtil.getCustomerIdFromToken(token);
            if (customerId == null) {
                return ResponseEntity.status(401)
                        .body(new ApiResponse(401, "未授权", null));
            }

            Integer purchaseId = (Integer) request.get("purchaseId");
            BigDecimal amount = new BigDecimal(request.get("amount").toString());

            if (purchaseId == null || amount == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(400, "参数错误", null));
            }

            Payment payment = paymentService.createPayment(purchaseId, customerId, amount);
            return ResponseEntity.ok(new ApiResponse(200, "支付记录创建成功", payment));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(400, e.getMessage(), null));
        } catch (Exception e) {
            log.error("创建支付记录失败", e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "创建失败：" + e.getMessage(), null));
        }
    }


    /**
     * POST /api/payments/{paymentId}/success
     * 支付成功回调
     */
    @PostMapping("/{paymentId}/success")
    public ResponseEntity<ApiResponse> paymentSuccess(
            @PathVariable("paymentId") Integer paymentId,
            @RequestParam("transactionId") String transactionId,
            @RequestParam("paymentMethod") String paymentMethod
    ) {
        try {
            log.info("收到支付成功回调：paymentId={}, transactionId={}, paymentMethod={}", 
                     paymentId, transactionId, paymentMethod);
            
            ApiResponse response = paymentService.processPaymentSuccess(paymentId, transactionId, paymentMethod);
            log.info("支付成功回调处理完成：code={}, message={}", response.getCode(), response.getMessage());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("支付成功回调失败，paymentId={}", paymentId, e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "处理失败：" + e.getMessage(), null));
        }
    }


    /**
     * POST /api/payments/{paymentId}/pay
     * 选择支付方式并发起支付
     */
    @PostMapping("/{paymentId}/pay")
    public ResponseEntity<ApiResponse> pay(
            @RequestHeader("Authorization") String token,
            @PathVariable("paymentId") Integer paymentId,
            @RequestBody Map<String, String> request
    ) {
        try {
            Integer customerId = JwtUtil.getCustomerIdFromToken(token);
            if (customerId == null) {
                return ResponseEntity.status(401)
                        .body(new ApiResponse(401, "未授权", null));
            }

            String paymentMethod = request.get("paymentMethod");
            if (paymentMethod == null || paymentMethod.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(400, "支付方式不能为空", null));
            }

            // 验证支付方式
            List<String> validMethods = List.of("BANK_CARD", "CREDIT_CARD", "ALIPAY", "WECHAT_PAY");
            if (!validMethods.contains(paymentMethod.toUpperCase())) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(400, "无效的支付方式", null));
            }

            // 查询支付记录，验证权限
            Payment payment = paymentService.getPaymentById(paymentId);
            if (payment == null) {
                return ResponseEntity.status(404)
                        .body(new ApiResponse(404, "支付记录不存在", null));
            }

            if (!payment.getCustomerId().equals(customerId)) {
                return ResponseEntity.status(403)
                        .body(new ApiResponse(403, "无权操作该支付记录", null));
            }

            // 更新支付方式
            ApiResponse response = paymentService.updatePaymentMethod(paymentId, paymentMethod.toUpperCase());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(400, e.getMessage(), null));
        } catch (Exception e) {
            log.error("选择支付方式失败，paymentId={}", paymentId, e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "处理失败：" + e.getMessage(), null));
        }
    }

    /**
     * POST /api/payments/{paymentId}/failure
     * 支付失败回调
     */
    @PostMapping("/{paymentId}/failure")
    public ResponseEntity<ApiResponse> paymentFailure(
            @PathVariable("paymentId") Integer paymentId,
            @RequestBody Map<String, String> request
    ) {
        try {
            String reason = request.get("reason");
            ApiResponse response = paymentService.processPaymentFailure(paymentId, reason);
            return ResponseEntity.status(response.getCode()).body(response);
        } catch (Exception e) {
            log.error("支付失败回调失败，paymentId={}", paymentId, e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "处理失败：" + e.getMessage(), null));
        }
    }

    /**
     * POST /api/payments/verify
     * 手动触发支付结果校验
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse> verifyPayment(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Integer> request
    ) {
        try {
            Integer customerId = JwtUtil.getCustomerIdFromToken(token);
            if (customerId == null) {
                return ResponseEntity.status(401)
                        .body(new ApiResponse(401, "未授权", null));
            }

            Integer purchaseId = request.get("purchaseId");
            if (purchaseId == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(400, "参数错误", null));
            }

            ApiResponse response = paymentService.verifyPaymentResult(purchaseId, customerId);
            return ResponseEntity.status(response.getCode()).body(response);
        } catch (Exception e) {
            log.error("支付结果校验失败", e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "校验失败：" + e.getMessage(), null));
        }
    }

    /**
     * POST /api/payments/{paymentId}/refund
     * 处理退款
     */
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<ApiResponse> refund(
            @RequestHeader("Authorization") String token,
            @PathVariable("paymentId") Integer paymentId,
            @RequestBody Map<String, Object> request
    ) {
        try {
            Integer sellerId = JwtUtil.getSellerIdFromToken(token);
            if (sellerId == null) {
                return ResponseEntity.status(401)
                        .body(new ApiResponse(401, "未授权", null));
            }

            BigDecimal refundAmount = new BigDecimal(request.get("refundAmount").toString());
            String refundReason = (String) request.get("refundReason");

            if (refundAmount == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(400, "退款金额不能为空", null));
            }

            ApiResponse response = paymentService.processRefund(paymentId, refundAmount, refundReason, sellerId);
            return ResponseEntity.status(response.getCode()).body(response);
        } catch (Exception e) {
            log.error("处理退款失败，paymentId={}", paymentId, e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "退款失败：" + e.getMessage(), null));
        }
    }

    /**
     * GET /api/payments/customer
     * 获取客户的支付记录
     */
    @GetMapping("/customer")
    public ResponseEntity<ApiResponse> getCustomerPayments(
            @RequestHeader("Authorization") String token
    ) {
        try {
            Integer customerId = JwtUtil.getCustomerIdFromToken(token);
            if (customerId == null) {
                return ResponseEntity.status(401)
                        .body(new ApiResponse(401, "未授权", null));
            }

            List<Payment> payments = paymentService.getPaymentsByCustomerId(customerId);
            return ResponseEntity.ok(new ApiResponse(200, "查询成功", payments));
        } catch (Exception e) {
            log.error("查询客户支付记录失败", e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "查询失败：" + e.getMessage(), null));
        }
    }

    /**
     * GET /api/payments/purchase/{purchaseId}
     * 根据购买意向 ID 获取支付记录
     */
    @GetMapping("/purchase/{purchaseId}")
    public ResponseEntity<ApiResponse> getPaymentByPurchaseId(
            @RequestHeader("Authorization") String token,
            @PathVariable("purchaseId") Integer purchaseId
    ) {
        try {
            Integer customerId = JwtUtil.getCustomerIdFromToken(token);
            if (customerId == null) {
                return ResponseEntity.status(401)
                        .body(new ApiResponse(401, "未授权", null));
            }

            Payment payment = paymentService.getPaymentByPurchaseId(purchaseId);
            if (payment == null) {
                return ResponseEntity.ok(new ApiResponse(404, "支付记录不存在", null));
            }

            return ResponseEntity.ok(new ApiResponse(200, "查询成功", payment));
        } catch (Exception e) {
            log.error("查询支付记录失败，purchaseId={}", purchaseId, e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "查询失败：" + e.getMessage(), null));
        }
    }

    /**
     * POST /api/payments/alipay/notify
     * 支付宝支付通知
     */
    @PostMapping(value = "/alipay/notify", produces = "text/plain;charset=UTF-8")
    public String alipayNotify(HttpServletRequest request) {
        Map<String, String> params = extractRequestParams(request);
        boolean success = paymentService.handleAlipayNotify(params);
        return success ? "success" : "failure";
    }

    /**
     * GET /api/payments/alipay/return
     * 支付宝支付返回
     */
    @GetMapping("/alipay/return")
    public ResponseEntity<ApiResponse> alipayReturn(@RequestParam Map<String, String> params) {
        ApiResponse response = paymentService.handleAlipayReturn(params);
        return ResponseEntity.status(response.getCode() == 200 ? 200 : 400).body(response);
    }

    private Map<String, String> extractRequestParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            String[] values = request.getParameterValues(name);
            if (values == null || values.length == 0) {
                continue;
            }
            params.put(name, String.join(",", values));
        }
        return params;
    }
}

