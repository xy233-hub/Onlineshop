// service/PurchaseIntentService.java
package com.example.onlineshop.service;

import com.example.onlineshop.dto.request.PurchaseIntentRequest;
import com.example.onlineshop.dto.request.PurchaseIntentStatusRequest;
import com.example.onlineshop.dto.response.ApiResponse;
import com.example.onlineshop.entity.Product;
import com.example.onlineshop.entity.PurchaseIntent;
import com.example.onlineshop.mapper.ProductMapper;
import com.example.onlineshop.mapper.PurchaseIntentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class PurchaseIntentService {

    @Autowired
    private PurchaseIntentMapper purchaseIntentMapper;

    @Autowired
    private ProductMapper productMapper;

    /**
     * 创建购买意向：校验 product 存在且为 online，计算 unit_price/total_amount，插入并返回创建的意向
     */
    @Transactional
    public PurchaseIntent createPurchaseIntent(PurchaseIntentRequest req) {
        Product product = productMapper.findById(req.getProductId());
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        if (!"online".equals(product.getProductStatus())) {
            throw new IllegalArgumentException("商品当前不可购买");
        }

        int quantity = req.getQuantity() == null ? 1 : req.getQuantity();
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity 必须大于 0");
        }

        // 可选：不在提交意向时扣库存，只在卖家确认时扣减，这里仅记录意向和计算金额
        BigDecimal unitPrice = product.getPrice();
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));

        PurchaseIntent intent = new PurchaseIntent();
        intent.setProductId(product.getProductId());
        intent.setCustomerId(req.getCustomerId());
        intent.setCustomerName(req.getCustomerName());
        intent.setCustomerPhone(req.getCustomerPhone());
        intent.setCustomerAddress(req.getCustomerAddress());
        intent.setQuantity(quantity);
        intent.setTotalAmount(totalAmount);
        intent.setPurchaseStatus("CUSTOMER_ORDERED");
        intent.setSellerNotes(null);
        intent.setCreatedAt(LocalDateTime.now());
        intent.setUpdatedAt(LocalDateTime.now());

        int rows = purchaseIntentMapper.insert(intent);
        if (rows <= 0) {
            return null;
        }

        // 回查以返回完整记录（含 purchase_id）
        PurchaseIntent created = purchaseIntentMapper.findById(intent.getPurchaseId());
        return created;
    }

    public List<PurchaseIntent> getPurchaseIntentsByProductId(Integer productId) {
        return purchaseIntentMapper.findByProductId(productId);
    }

    public List<PurchaseIntent> getPurchaseIntentsByCustomerId(Integer customerId) {
        return purchaseIntentMapper.findByCustomerId(customerId);
    }

    public PurchaseIntent getPurchaseIntentById(Integer purchaseId) {
        return purchaseIntentMapper.findById(purchaseId);
    }

    /**
     * 更新购买意向状态：支持新的订单状态流程
     */
    @Transactional
    public ApiResponse handlePurchaseIntentStatus(Integer purchaseId, PurchaseIntentStatusRequest request, Integer sellerId) {

        PurchaseIntent intent = purchaseIntentMapper.findById(purchaseId);

        if (intent == null) {
            return new ApiResponse(404, "购买意向不存在", null);
        }

        String newStatus = request.getNewStatus();

        // 验证状态转换是否合法
        if (!isValidStatusTransition(intent.getPurchaseStatus(), newStatus)) {
            return new ApiResponse(400, "非法的状态转换: 从 " + intent.getPurchaseStatus() + " 到 " + newStatus, null);
        }

        // 检查卖家权限：该意向对应商品必须属于当前 seller
        Product product = productMapper.findById(intent.getProductId());
        if (product == null) {
            return new ApiResponse(404, "关联商品不存在", null);
        }
        if (!product.getSellerId().equals(sellerId)) {
            return new ApiResponse(403, "无权处理该购买意向", null);
        }

        LocalDateTime now = LocalDateTime.now();

        // 处理商家操作的状态更新
        try {
            switch (newStatus) {
                case "SELLER_CONFIRMED":
                case "STOCK_PREPARED":
                case "SHIPPING_STARTED":
                    purchaseIntentMapper.updateStatusAndNotes(purchaseId, newStatus, request.getSellerNotes(), now);
                    break;

                case "COMPLETED":
                    // 客户确认收货完成订单
                    purchaseIntentMapper.updateStatus(purchaseId, newStatus, now);
                    break;

                case "SELLER_CANCELLED":
                    // 商家取消订单
                    Map<String, Object> cancelParams = Map.of(
                            "purchaseId", purchaseId,
                            "status", newStatus,
                            "cancelReason", request.getCancelReason() != null ? request.getCancelReason() : "",
                            "cancelNotes", request.getCancelNotes() != null ? request.getCancelNotes() : "",
                            "updatedAt", now
                    );
                    purchaseIntentMapper.updateStatusWithCancelInfo(cancelParams);
                    break;

                default:
                    return new ApiResponse(400, "不支持的状态更新", null);
            }

            PurchaseIntent updated = purchaseIntentMapper.findById(purchaseId);
            return new ApiResponse(200, "处理成功", updated);
        } catch (Exception e) {
            // 记录异常并返回错误信息
            return new ApiResponse(500, "处理失败: " + e.getMessage(), null);
        }
    }

    /**
     * 客户取消订单
     */
    @Transactional
    public ApiResponse customerCancelOrder(Integer purchaseId, String cancelReason, String cancelNotes, Integer customerId) {
        try {
            PurchaseIntent intent = purchaseIntentMapper.findById(purchaseId);

            if (intent == null) {
                return new ApiResponse(404, "订单不存在", null);
            }

            // 验证是否为客户本人操作
            if (!intent.getCustomerId().equals(customerId)) {
                return new ApiResponse(403, "无权操作该订单", null);
            }

            // 验证状态是否允许客户取消
            String currentStatus = intent.getPurchaseStatus();
            if (!"CUSTOMER_ORDERED".equals(currentStatus)
                    && !"SELLER_CONFIRMED".equals(currentStatus)
                    && !"STOCK_PREPARED".equals(currentStatus)) {
                return new ApiResponse(400, "当前订单状态不允许客户取消", null);
            }

            // 执行取消操作
            Map<String, Object> cancelParams = Map.of(
                    "purchaseId", purchaseId,
                    "status", "CUSTOMER_CANCELLED",
                    "cancelReason", cancelReason != null ? cancelReason : "",
                    "cancelNotes", cancelNotes != null ? cancelNotes : "",
                    "updatedAt", LocalDateTime.now()
            );

            int updatedRows = purchaseIntentMapper.updateStatusWithCancelInfo(cancelParams);
            if (updatedRows <= 0) {
                return new ApiResponse(500, "更新订单状态失败", null);
            }

            PurchaseIntent updated = purchaseIntentMapper.findById(purchaseId);
            return new ApiResponse(200, "订单已取消", updated);
        } catch (Exception e) {
            // 记录详细的异常信息
            e.printStackTrace();
            return new ApiResponse(500, "取消订单失败: " + e.getMessage(), null);
        }
    }

    /**
     * 客户确认收货
     */
    @Transactional
    public ApiResponse customerConfirmReceived(Integer purchaseId, Integer customerId) {
        PurchaseIntent intent = purchaseIntentMapper.findById(purchaseId);

        if (intent == null) {
            return new ApiResponse(404, "订单不存在", null);
        }

        // 验证是否为客户本人操作
        if (!intent.getCustomerId().equals(customerId)) {
            return new ApiResponse(403, "无权操作该订单", null);
        }

        // 验证状态是否允许客户确认收货
        String currentStatus = intent.getPurchaseStatus();
        if (!"SHIPPING_STARTED".equals(currentStatus)) {
            return new ApiResponse(400, "当前订单状态不允许客户确认收货", null);
        }

        // 执行确认收货操作
        purchaseIntentMapper.updateStatus(purchaseId, "COMPLETED", LocalDateTime.now());

        // 减少商品库存
        try {
            int productId = intent.getProductId();
            int quantity = intent.getQuantity();

            // 使用原子操作扣减库存
            int updatedRows = productMapper.deductStockIfEnough(productId, quantity, LocalDateTime.now());

            if (updatedRows <= 0) {
                // 库存不足，回滚状态更新（由于使用了@Transactional，会自动回滚）
                throw new RuntimeException("商品库存不足，无法完成订单");
            }
        } catch (Exception e) {
            // 如果扣减库存失败，回滚确认收货操作
            purchaseIntentMapper.updateStatus(purchaseId, "SHIPPING_STARTED", LocalDateTime.now());
            return new ApiResponse(500, "确认收货失败: " + e.getMessage(), null);
        }

        PurchaseIntent updated = purchaseIntentMapper.findById(purchaseId);
        return new ApiResponse(200, "订单已完成，库存已更新", updated);
    }

    /**
     * 验证状态转换是否合法
     */
    private boolean isValidStatusTransition(String currentStatus, String newStatus) {
        // 定义允许的状态转换
        switch (currentStatus) {
            case "CUSTOMER_ORDERED":
                return "SELLER_CONFIRMED".equals(newStatus)
                        || "STOCK_PREPARED".equals(newStatus)
                        || "SHIPPING_STARTED".equals(newStatus)
                        || "COMPLETED".equals(newStatus)
                        || "CUSTOMER_CANCELLED".equals(newStatus)
                        || "SELLER_CANCELLED".equals(newStatus);

            case "SELLER_CONFIRMED":
                return "STOCK_PREPARED".equals(newStatus)
                        || "SHIPPING_STARTED".equals(newStatus)
                        || "COMPLETED".equals(newStatus)
                        || "CUSTOMER_CANCELLED".equals(newStatus)
                        || "SELLER_CANCELLED".equals(newStatus);

            case "STOCK_PREPARED":
                return "SHIPPING_STARTED".equals(newStatus)
                        || "COMPLETED".equals(newStatus)
                        || "CUSTOMER_CANCELLED".equals(newStatus)
                        || "SELLER_CANCELLED".equals(newStatus);

            case "SHIPPING_STARTED":
                return "COMPLETED".equals(newStatus)
                        || "SELLER_CANCELLED".equals(newStatus);

            case "COMPLETED":
            case "CUSTOMER_CANCELLED":
            case "SELLER_CANCELLED":
                // 最终状态不能再改变
                return false;

            default:
                return false;
        }
    }

    public List<PurchaseIntent> getAllPurchaseIntents() {
        return purchaseIntentMapper.findAll();
    }

    public void markOtherIntentsFailed(Integer productId, Integer excludePurchaseId) {
        purchaseIntentMapper.markOtherIntentsFailed(productId, excludePurchaseId);
    }

    public List<PurchaseIntent> getPurchaseIntentsByCondition(Map<String, Object> params) {
        return purchaseIntentMapper.findByCondition(params);
    }

    public int countPurchaseIntentsByCondition(Map<String, Object> params) {
        return purchaseIntentMapper.countByCondition(params);
    }
}
