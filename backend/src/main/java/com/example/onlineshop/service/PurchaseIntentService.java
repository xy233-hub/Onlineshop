// service/PurchaseIntentService.java
package com.example.onlineshop.service;

import com.example.onlineshop.dto.request.PurchaseIntentRequest;
import com.example.onlineshop.dto.request.PurchaseIntentStatusRequest;
import com.example.onlineshop.dto.response.ApiResponse;
import com.example.onlineshop.entity.Customer;
import com.example.onlineshop.entity.Payment;
import com.example.onlineshop.entity.Product;
import com.example.onlineshop.entity.PurchaseIntent;
import com.example.onlineshop.entity.PurchaseIntentItem;
import com.example.onlineshop.mapper.PaymentMapper;
import com.example.onlineshop.mapper.ProductMapper;
import com.example.onlineshop.mapper.PurchaseIntentItemMapper;
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
    private PurchaseIntentItemMapper purchaseIntentItemMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private PaymentMapper paymentMapper;


    /**
     * 根据 purchaseId 回查购买意向并回填商品项
     */
    public PurchaseIntent getPurchaseIntentById(Integer purchaseId) {
        PurchaseIntent intent = purchaseIntentMapper.findById(purchaseId);
        if (intent == null) {
            return null;
        }
        List<PurchaseIntentItem> items = purchaseIntentItemMapper.findByPurchaseId(purchaseId);
        intent.setItems(items);
        return intent;
    }

    /**
     * 直接根据 purchaseId 获取商品项列表
     */
    public List<PurchaseIntentItem> getItemsByPurchaseIntentId(Integer purchaseId) {
        return purchaseIntentItemMapper.findByPurchaseId(purchaseId);
    }

    /**
     * 创建购买意向：不再把 productId 写入 purchase_intents，
     * 而是插入一条 purchase_intents（订单层），然后为每个商品插入 purchase_intent_items（商品项层）。
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

        Customer customer = null;
        if (req.getCustomerId() != null) {
            customer = customerService.findById(req.getCustomerId());
        }

        String customerName = req.getCustomerName();
        if ((customerName == null || customerName.isBlank()) && customer != null) {
            customerName = customer.getUsername();
        }

        String customerPhone = req.getCustomerPhone();
        if ((customerPhone == null || customerPhone.isBlank()) && customer != null) {
            customerPhone = customer.getPhone();
        }

        String customerAddress = req.getCustomerAddress();
        if ((customerAddress == null || customerAddress.isBlank()) && customer != null) {
            customerAddress = customer.getDefaultAddress();
        }

        BigDecimal unitPrice = product.getPrice();
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));

        // 1) 插入 purchase_intents（不设置 productId 字段）
        PurchaseIntent intent = new PurchaseIntent();
        intent.setCustomerId(req.getCustomerId());
        intent.setCustomerName(customerName);
        intent.setCustomerPhone(customerPhone);
        intent.setCustomerAddress(customerAddress);
        intent.setQuantity(quantity); // 可视为订单总量（单商品场景即该商品数量）
        intent.setTotalAmount(totalAmount);
        intent.setPurchaseStatus("CUSTOMER_ORDERED");
        intent.setSellerNotes(req.getNote());
        intent.setCreatedAt(LocalDateTime.now());
        intent.setUpdatedAt(LocalDateTime.now());

        int rows = purchaseIntentMapper.insert(intent);
        if (rows <= 0 || intent.getPurchaseId() == null) {
            throw new IllegalStateException("创建购买意向失败");
        }

        // 2) 为该商品创建 purchase_intent_items（商品快照）
        PurchaseIntentItem item = new PurchaseIntentItem();
        item.setPurchaseId(intent.getPurchaseId());
        item.setProductId(product.getProductId());
        item.setProductName(product.getProductName());
        item.setProductPrice(product.getPrice());
        item.setQuantity(quantity);
        item.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(quantity)));

        int ir = purchaseIntentItemMapper.insert(item);
        if (ir <= 0) {
            throw new IllegalStateException("创建购买意向商品项失败");
        }

        // 3) 回查并返回完整的 purchase_intent（productId 字段应为空或忽略）
        PurchaseIntent created = purchaseIntentMapper.findById(intent.getPurchaseId());
        return created;
    }

    public List<PurchaseIntent> getPurchaseIntentsByProductId(Integer productId) {
        return purchaseIntentMapper.findByProductId(productId);
    }


    public List<PurchaseIntent> getPurchaseIntentsByCustomerId(Integer customerId) {
        List<PurchaseIntent> intents = purchaseIntentMapper.findByCustomerId(customerId);
        if (intents == null || intents.isEmpty()) {
            return intents;
        }
        for (PurchaseIntent intent : intents) {
            if (intent != null && intent.getPurchaseId() != null) {
                // 获取商品项列表
                List<PurchaseIntentItem> items = purchaseIntentItemMapper.findByPurchaseId(intent.getPurchaseId());
                intent.setItems(items);
                
                // 获取支付状态
                Payment payment = paymentMapper.findByPurchaseId(intent.getPurchaseId());
                if (payment != null) {
                    intent.setPaymentStatus(payment.getPaymentStatus());
                    intent.setPaymentVerifyToken(payment.getTransactionId());
                } else {
                    // 如果没有支付记录，默认为 UNPAID
                    intent.setPaymentStatus("UNPAID");
                }
            }
        }
        return intents;
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
        List<PurchaseIntentItem> items = purchaseIntentItemMapper.findByPurchaseId(purchaseId);
        if (items == null || items.isEmpty()) {
            return new ApiResponse(404, "关联商品不存在", null);
        }
        for (PurchaseIntentItem piItem : items) {
            Product product = productMapper.findById(piItem.getProductId());
            if (product == null) {
                return new ApiResponse(404, "关联商品不存在", null);
            }
            if (product.getSellerId() == null || !product.getSellerId().equals(sellerId)) {
                return new ApiResponse(403, "无权处理该购买意向", null);
            }
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

        // 查询该购买意向的所有商品项
        List<PurchaseIntentItem> items = purchaseIntentItemMapper.findByPurchaseId(purchaseId);
        if (items == null || items.isEmpty()) {
            return new ApiResponse(404, "关联商品不存在", null);
        }

        try {
            LocalDateTime now = LocalDateTime.now();

            // 逐个商品扣减库存（在事务中，任一失败抛出异常则回滚）
            for (PurchaseIntentItem item : items) {
                Integer productId = item.getProductId();
                Integer quantity = item.getQuantity() == null ? 0 : item.getQuantity();

                if (productId == null || quantity <= 0) {
                    throw new RuntimeException("关联商品信息不完整");
                }

                int updatedRows = productMapper.deductStockIfEnough(productId, quantity, now);
                if (updatedRows <= 0) {
                    throw new RuntimeException("商品库存不足，productId=" + productId);
                }
            }

            // 全部扣减成功后更新订单状态为 COMPLETED
            purchaseIntentMapper.updateStatus(purchaseId, "COMPLETED", LocalDateTime.now());

            PurchaseIntent updated = purchaseIntentMapper.findById(purchaseId);
            // 回填 items 以保证返回结果包含商品项
            List<PurchaseIntentItem> updatedItems = purchaseIntentItemMapper.findByPurchaseId(purchaseId);
            updated.setItems(updatedItems);

            return new ApiResponse(200, "订单已完成，库存已更新", updated);
        } catch (Exception e) {
            // 事务会回滚，无需手动恢复状态
            return new ApiResponse(500, "确认收货失败: " + e.getMessage(), null);
        }
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
        List<PurchaseIntent> intents = purchaseIntentMapper.findAll();
        if (intents == null || intents.isEmpty()) {
            return intents;
        }
        for (PurchaseIntent intent : intents) {
            if (intent != null && intent.getPurchaseId() != null) {
                List<PurchaseIntentItem> items = purchaseIntentItemMapper.findByPurchaseId(intent.getPurchaseId());
                intent.setItems(items);
            }
        }
        return intents;
    }

    public void markOtherIntentsFailed(Integer productId, Integer excludePurchaseId) {
        purchaseIntentMapper.markOtherIntentsFailed(productId, excludePurchaseId);
    }

     public List<PurchaseIntent> getPurchaseIntentsByCondition(Map<String, Object> params) {
        List<PurchaseIntent> intents = purchaseIntentMapper.findByCondition(params);
        if (intents == null || intents.isEmpty()) {
            return intents;
        }
        for (PurchaseIntent intent : intents) {
            if (intent != null && intent.getPurchaseId() != null) {
                List<PurchaseIntentItem> items = purchaseIntentItemMapper.findByPurchaseId(intent.getPurchaseId());
                intent.setItems(items);
            }
        }
        return intents;
    }

    public int countPurchaseIntentsByCondition(Map<String, Object> params) {
        return purchaseIntentMapper.countByCondition(params);
    }
}
