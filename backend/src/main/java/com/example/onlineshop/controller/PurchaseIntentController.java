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
 * 卖家端的采购意向控制器 提供查询所有采购意向和更新采购意向状态的 HTTP 接口 主要职责： - 从请求头解析 token 并校验卖家身份 - 调用
 * PurchaseIntentService 完成具体业务 - 在异常情况下记录完整日志并返回规范化的 ApiResponse
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
     * GET / 获取所有采购意向列表（供卖家查看）
     * 请求头： - Authorization: 包含 JWT，用于解析并验证卖家身份
     * 查询参数： - seller_id: 可选，指定查询哪个卖家的购买意向（支持买家调用）
     * 返回： - 401 未授权（当 token 无效或无法解析 sellerId 时） - 200 成功，body 为 ApiResponse 包含数据列表 -
     * 500 出错，记录完整异常并返回带有 fallback 消息的错误响应
     */
    @GetMapping("")
    public ResponseEntity<ApiResponse> getAllPurchaseIntents(
            @RequestHeader("Authorization") String token,
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10") Integer size,
            @RequestParam(value = "seller_id", required = false) Integer sellerId
    ) {
        // 从 token 中解析 sellerId
        Integer tokenSellerId = JwtUtil.getSellerIdFromToken(token);
        
        // 从 token 中解析 customerId
        Integer customerId = JwtUtil.getCustomerIdFromToken(token);
        
        // 卖家调用
        if (tokenSellerId != null) {
            // 卖家调用时，如果没有传 seller_id，则使用 token 中的 sellerId
            if (sellerId == null) {
                sellerId = tokenSellerId;
            }
            
            try {
                // 分页参数处理
                if (page == null || page < 1) page = 1;
                if (size == null || size < 1) size = 10;
                
                // 调用服务层根据 sellerId 查询购买意向
                List<PurchaseIntent> list = purchaseIntentService.getPurchaseIntentsBySellerId(sellerId, page, size);
                int total = purchaseIntentService.countPurchaseIntentsBySellerId(sellerId);
                
                Map<String, Object> result = new HashMap<>();
                result.put("page", page);
                result.put("size", size);
                result.put("total", total);
                result.put("items", list);
                
                return ResponseEntity.ok(new ApiResponse(200, "查询成功", result));
            } catch (Exception e) {
                // 捕获所有异常并记录完整堆栈，返回通用错误信息并附带异常简短描述（避免 null）
                logger.error("getAllPurchaseIntents failed, sellerId={}", sellerId, e);
                String msg = e.getMessage() != null ? e.getMessage() : e.toString();
                return ResponseEntity.status(500)
                        .body(new ApiResponse(500, "查询失败：" + msg, null));
            }
        } 
        // 买家调用
        else if (customerId != null) {
            try {
                // 如果传递了 seller_id 参数，说明是买家查看自己作为卖家的购买意向
                if (sellerId != null) {
                    // 分页参数处理
                    if (page == null || page < 1) page = 1;
                    if (size == null || size < 1) size = 10;
                    
                    // 调用服务层根据 sellerId 查询购买意向
                    List<PurchaseIntent> list = purchaseIntentService.getPurchaseIntentsBySellerId(sellerId, page, size);
                    int total = purchaseIntentService.countPurchaseIntentsBySellerId(sellerId);
                    
                    Map<String, Object> result = new HashMap<>();
                    result.put("page", page);
                    result.put("size", size);
                    result.put("total", total);
                    result.put("items", list);
                    
                    return ResponseEntity.ok(new ApiResponse(200, "查询成功", result));
                } else {
                    // 调用服务层根据 customerId 查询购买意向
                    List<PurchaseIntent> list = purchaseIntentService.getPurchaseIntentsByCustomerId(customerId);
                    
                    Map<String, Object> result = new HashMap<>();
                    result.put("page", 1);
                    result.put("size", list.size());
                    result.put("total", list.size());
                    result.put("items", list);
                    
                    return ResponseEntity.ok(new ApiResponse(200, "查询成功", result));
                }
            } catch (Exception e) {
                // 捕获所有异常并记录完整堆栈，返回通用错误信息并附带异常简短描述（避免 null）
                logger.error("getAllPurchaseIntents failed, customerId={}, sellerId={}", customerId, sellerId, e);
                String msg = e.getMessage() != null ? e.getMessage() : e.toString();
                return ResponseEntity.status(500)
                        .body(new ApiResponse(500, "查询失败：" + msg, null));
            }
        }
        // 未授权
        else {
            return ResponseEntity.status(401)
                    .body(new ApiResponse(401, "未授权", null));
        }
    }

    /**
     * PUT /{purchase_id}/status 更新指定采购意向的处理状态（例如：接受、拒绝、已完成等）
     * 路径参数： - purchase_id: 采购意向的唯一标识 
     * 请求体： - PurchaseIntentStatusRequest 包含更新所需字段（如新状态、备注等） 
     * 请求头： - Authorization: 用于校验身份（卖家或买家）
     * 返回： - 401 未授权（token 无效） - 根据服务层返回的 ApiResponse 使用对应的 HTTP 状态码和内容 
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
        
        // 校验 token 并获取 customerId
        Integer customerId = JwtUtil.getCustomerIdFromToken(token);
        
        // 现在允许买家（customer）也能处理购买意向状态
        // 当买家发布商品后，别人购买该商品，买家需要能处理订单
        // 此时买家的 customerId 就充当 sellerId 的角色
        Integer effectiveSellerId = null;
        if (sellerId != null) {
            // 传统卖家模式
            effectiveSellerId = sellerId;
        } else if (customerId != null) {
            // 买家作为个人卖家模式：用 customerId 作为 sellerId
            effectiveSellerId = customerId;
        } else {
            return ResponseEntity.status(401).body(new ApiResponse(401, "未授权", null));
        }
        
        try {
            // 将请求转发给服务层处理，并直接使用服务层返回的 ApiResponse
            ApiResponse resp = purchaseIntentService.handlePurchaseIntentStatus(purchaseId, req, effectiveSellerId);
            return ResponseEntity.status(resp.getCode()).body(resp);
        } catch (Exception e) {
            // 记录详细异常以便排查问题，返回通用错误响应并附带异常简短描述
            logger.error("updateIntentStatus failed, purchaseId={}, effectiveSellerId={}", purchaseId, effectiveSellerId, e);
            String msg = e.getMessage() != null ? e.getMessage() : e.toString();
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "处理失败：" + msg, null));
        }
    }

}