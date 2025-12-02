// service/PurchaseIntentService.java
package com.example.onlineshop.service;

import com.example.onlineshop.dto.request.PurchaseIntentRequest;
import com.example.onlineshop.dto.request.PurchaseIntentStatusRequest;
import com.example.onlineshop.dto.response.ApiResponse;
import com.example.onlineshop.entity.Customer;
import com.example.onlineshop.entity.Product;
import com.example.onlineshop.entity.PurchaseIntent;
import com.example.onlineshop.entity.PurchaseIntentItem;
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
        return purchaseIntentMapper.findByCustomerId(customerId);
    }

    public PurchaseIntent getPurchaseIntentById(Integer purchaseId) {
        return purchaseIntentMapper.findById(purchaseId);
    }

    /**
     * 更新购买意向状态：仅允许从 pending 更新为 success 或 failed
     * 若更新为 success，则尝试原子扣减商品库存，库存不足则返回错误
     * 同时更新卖家备注与更新时间
     */
    @Transactional
    public ApiResponse handlePurchaseIntentStatus(Integer purchaseId, PurchaseIntentStatusRequest request, Integer sellerId) {

        PurchaseIntent intent = purchaseIntentMapper.findById(purchaseId);

        if (intent == null) {
            return new ApiResponse(404, "购买意向不存在", null);
        }

        String newStatus = request.getNewStatus();
        if (!"success".equals(newStatus) && !"failed".equals(newStatus)) {
            return new ApiResponse(400, "new_status 必须为 'success' 或 'failed'", null);
        }

        if (!"pending".equals(intent.getPurchaseStatus())) {
            return new ApiResponse(400, "该购买意向已处理", null);
        }

        // 检查卖家权限：该意向对应商品必须属于当前 seller
        Product product = productMapper.findById(intent.getProductId());
        if (product == null) {
            return new ApiResponse(404, "关联商品不存在", null);
        }
        if (!product.getSellerId().equals(sellerId)) {
            return new ApiResponse(403, "无权处理该购买意向", null);
        }

        int quantityToDeduct = request.getSoldQuantity() == null ? intent.getQuantity() : request.getSoldQuantity();
        if (quantityToDeduct <= 0) {
            return new ApiResponse(400, "sold_quantity 必须大于 0", null);
        }

        LocalDateTime now = LocalDateTime.now();

        if ("success".equals(newStatus)) {
            // 原子扣减库存：在 mapper 中用 SQL 做 WHERE stock_quantity >= #{deduct} 并更新 stock & status
            int rows = productMapper.deductStockIfEnough(product.getProductId(), quantityToDeduct, now);
            if (rows == 0) {
                return new ApiResponse(400, "库存不足", null);
            }

        }

        // 更新本条购买意向状态与备注、更新时间
        purchaseIntentMapper.updateStatusAndNotes(purchaseId, newStatus, request.getSellerNotes(), now);

        PurchaseIntent updated = purchaseIntentMapper.findById(purchaseId);
        return new ApiResponse(200, "处理成功", updated);
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
