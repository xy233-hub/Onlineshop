// java
package com.example.onlineshop.controller;

import com.example.onlineshop.dto.request.PurchaseIntentStatusRequest;
import com.example.onlineshop.dto.response.ApiResponse;
import com.example.onlineshop.entity.PurchaseIntent;
import com.example.onlineshop.service.PurchaseIntentService;
import com.example.onlineshop.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 添加Product类的导入
import com.example.onlineshop.entity.Product;
import com.example.onlineshop.service.ProductService;
import com.example.onlineshop.dto.request.PurchaseRequest;
import com.example.onlineshop.util.ResponseUtil;

/**
 * 卖家端的采购意向控制器
 * 提供查询所有采购意向和更新采购意向状态的 HTTP 接口
 * 主要职责：
 * - 从请求头解析 token 并校验卖家身份
 * - 调用 PurchaseIntentService 完成具体业务
 * - 在异常情况下记录完整日志并返回规范化的 ApiResponse
 */
@RestController
@RequestMapping("/api/seller/purchase-intents")
public class PurchaseIntentController {

    /**
     * 日志记录器：用于在发生异常或关键操作时输出详细信息
     */
    private static final Logger logger = LoggerFactory.getLogger(PurchaseIntentController.class);

    /**
     * 业务服务：处理采购意向的查询与状态变更逻辑
     */
    @Autowired
    private PurchaseIntentService purchaseIntentService;

    /**
     * GET /
     * 获取所有采购意向列表（供卖家查看）
     * 请求头：
     * - Authorization: 包含 JWT，用于解析并验证卖家身份
     * 返回：
     * - 401 未授权（当 token 无效或无法解析 sellerId 时）
     * - 200 成功，body 为 ApiResponse 包含数据列表
     * - 500 出错，记录完整异常并返回带有 fallback 消息的错误响应
     */
    @GetMapping("")
    public ResponseEntity<ApiResponse> getAllPurchaseIntents(
            @RequestHeader("Authorization") String token
    ) {
        // 从 token 中解析 sellerId（若解析失败则认为未授权）
        Integer sellerId = JwtUtil.getSellerIdFromToken(token);
        if (sellerId == null) {
            return ResponseEntity.status(401)
                    .body(new ApiResponse(401, "未授权", null));
        }
        try {
            // 调用服务层获取所有采购意向
            List<PurchaseIntent> list = purchaseIntentService.getAllPurchaseIntents();
            return ResponseEntity.ok(new ApiResponse(200, "查询成功", list));
        } catch (Exception e) {
            // 捕获所有异常并记录完整堆栈，返回通用错误信息并附带异常简短描述（避免 null）
            logger.error("getAllPurchaseIntents failed, sellerId={}", sellerId, e);
            String msg = e.getMessage() != null ? e.getMessage() : e.toString();
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "查询失败: " + msg, null));
        }
    }

    /**
     * PUT /{purchase_id}/status
     * 更新指定采购意向的处理状态（例如：接受、拒绝、已完成等）
     * 路径参数：
     * - purchase_id: 采购意向的唯一标识
     * 请求体：
     * - PurchaseIntentStatusRequest 包含更新所需字段（如新状态、备注等）
     * 请求头：
     * - Authorization: 用于校验卖家身份
     * 返回：
     * - 401 未授权（token 无效）
     * - 根据服务层返回的 ApiResponse 使用对应的 HTTP 状态码和内容
     * - 500 出错时记录异常并返回带描述的错误响应
     */
    @PutMapping("/{purchase_id}/status")
    public ResponseEntity<ApiResponse> updateIntentStatus(
            @RequestHeader("Authorization") String token,
            @PathVariable("purchase_id") Integer purchaseId,
            @RequestBody PurchaseIntentStatusRequest req
    ) {
        // 校验 token 并获取 sellerId
        Integer sellerId = JwtUtil.getSellerIdFromToken(token);
        if (sellerId == null) {
            return ResponseEntity.status(401).body(new ApiResponse(401, "未授权", null));
        }
        try {
            // 将请求转发给服务层处理，并直接使用服务层返回的 ApiResponse
            ApiResponse resp = purchaseIntentService.handlePurchaseIntentStatus(purchaseId, req, sellerId);
            return ResponseEntity.status(resp.getCode()).body(resp);
        } catch (Exception e) {
            // 记录详细异常以便排查问题，返回通用错误响应并附带异常简短描述
            logger.error("updateIntentStatus failed, purchaseId={}, sellerId={}", purchaseId, sellerId, e);
            String msg = e.getMessage() != null ? e.getMessage() : e.toString();
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "处理失败: " + msg, null));
        }
    }


}
