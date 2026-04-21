package com.example.onlineshop.service;

import com.example.onlineshop.dto.response.CartItemResponse;
import com.example.onlineshop.entity.ShoppingCartItem;
import com.example.onlineshop.mapper.ShoppingCartMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.onlineshop.entity.Customer;
import com.example.onlineshop.entity.Payment;
import com.example.onlineshop.entity.Product;
import com.example.onlineshop.entity.PurchaseIntent;
import com.example.onlineshop.entity.PurchaseIntentItem;
import com.example.onlineshop.mapper.CustomerMapper;
import com.example.onlineshop.mapper.PurchaseIntentMapper;
import com.example.onlineshop.mapper.PurchaseIntentItemMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ShoppingCartService {

    @Autowired
    private ShoppingCartMapper cartMapper;

    @Autowired
    private ProductService productService;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private PurchaseIntentMapper purchaseIntentMapper;

    @Autowired
    private PurchaseIntentItemMapper purchaseIntentItemMapper;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PromotionService promotionService;

    private static final Logger log = LoggerFactory.getLogger(ShoppingCartService.class);

    /**
     * 添加商品到购物车（存在则数量累加，累加后不超过库存）
     */
    @Transactional
    public Map<String, Object> addItem(Integer customerId, Integer productId, Integer quantity) {
        if (quantity == null || quantity <= 0) quantity = 1;

        // 校验商品是否存在且为 online
        if (!productService.isProductOnline(productId)) {
            return Map.of("code", 400, "message", "商品不存在或已下架", "data", Collections.emptyMap());
        }

        Integer stock = productService.getProductStock(productId);
        if (stock == null || stock <= 0) {
            return Map.of("code", 400, "message", "商品库存不足", "data", Collections.emptyMap());
        }

        ShoppingCartItem existing = cartMapper.findByCustomerAndProduct(customerId, productId);
        if (existing != null) {
            int newQty = Math.min(stock, existing.getQuantity() + quantity);
            cartMapper.updateQuantity(existing.getCartItemId(), newQty);
            return Map.of("code", 200, "message", "已更新购物车数量", "data", Map.of("cart_item_id", existing.getCartItemId(), "quantity", newQty));
        } else {
            int toInsertQty = Math.min(stock, quantity);
            ShoppingCartItem item = new ShoppingCartItem();
            item.setCustomerId(customerId);
            item.setProductId(productId);
            item.setQuantity(toInsertQty);
            cartMapper.insert(item);
            return Map.of("code", 200, "message", "添加购物车成功", "data", Map.of("cart_item_id", item.getCartItemId(), "quantity", toInsertQty));
        }
    }

    /**
     * 查询购物车（分页）
     */
    public Map<String, Object> listItems(Integer customerId, int page, int size, Boolean filterStock, String sortBy, String order) {
        if (page <= 0) page = 1;
        if (size <= 0) size = 10;
        if (sortBy == null || sortBy.isBlank()) sortBy = "created_at";
        if (order == null || order.isBlank()) order = "desc";

        int offset = (page - 1) * size;
        List<CartItemResponse> items = cartMapper.listByCustomer(customerId, offset, size, filterStock, sortBy, order);
        int total = cartMapper.countByCustomer(customerId);

        return Map.of(
                "code", 200,
                "message", "成功",
                "data", Map.of(
                        "page", page,
                        "size", size,
                        "total", total,
                        "items", items
                )
        );
    }

    /**
     * 删除购物车项（支持单个/批量），仅允许删除属于该客户的项
     */
    @Transactional
    public Map<String, Object> deleteItems(Integer customerId, List<Integer> cartItemIds) {
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            return Map.of("code", 400, "message", "没有要删除的项", "data", Collections.emptyMap());
        }

        int deleted = cartMapper.deleteByIds(customerId, cartItemIds);
        int failed = cartItemIds.size() - deleted;

        return Map.of(
                "code", 200,
                "message", "删除完成",
                "data", Map.of(
                        "deleted_count", deleted,
                        "failed_count", failed,
                        "deleted_cart_item_ids", deleted > 0 ? cartItemIds.subList(0, deleted) : List.of()
                )
        );
    }

    /**
     * 根据 customerId 和 cart_item_id 列表查询购物车项并返回通用 Map 结构（供 controller 使用）
     * 返回列表按输入 cartItemIds 顺序排列（不存在或不属于该客户的 id 会被忽略）
     */
    public List<Map<String, Object>> getItemsByIds(Integer customerId, List<Integer> cartItemIds) {
        if (customerId == null || cartItemIds == null || cartItemIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 从 mapper 批量查询（仅返回属于该 customer 的项）
        List<CartItemResponse> items = cartMapper.listByCustomerAndIds(customerId, cartItemIds);
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        // 建立 id -> map 映射
        Map<Integer, Map<String, Object>> idToMap = new HashMap<>();
        for (CartItemResponse it : items) {
            Map<String, Object> m = new HashMap<>();
            m.put("cart_item_id", it.getCartItemId());
            m.put("product_id", it.getProductId());
            m.put("product_name", it.getProductName());
            m.put("quantity", it.getQuantity());
            m.put("unit_price", it.getUnitPrice());
            m.put("stock_quantity", it.getStockQuantity());
            m.put("product_status", it.getProductStatus());
            idToMap.put(it.getCartItemId(), m);
        }

        // 按输入顺序组织返回结果，忽略不存在的 id
        List<Map<String, Object>> result = new ArrayList<>();
        for (Integer id : cartItemIds) {
            Map<String, Object> entry = idToMap.get(id);
            if (entry != null) result.add(entry);
        }
        return result;
    }
    /**
     * 批量下单预结算：返回促销结算后的金额与明细，不创建订单。
     */
    public Map<String, Object> batchPurchasePreview(Integer customerId, List<Integer> cartItemIds) {
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            return Map.of("code", 400, "message", "请选择要结算的商品", "data", Collections.emptyMap());
        }

        List<CartItemResponse> cartItems = cartMapper.listByCustomerAndIds(customerId, cartItemIds);
        if (cartItems.isEmpty()) {
            return Map.of("code", 400, "message", "选中的商品不存在或不属于该用户", "data", Collections.emptyMap());
        }

        List<SettledCartItem> settledItems = buildSettledItems(cartItems);
        if (settledItems.isEmpty()) {
            return Map.of("code", 400, "message", "没有有效的商品可以结算", "data", Collections.emptyMap());
        }

        Map<Integer, List<SettledCartItem>> itemsBySeller = groupItemsBySeller(settledItems);
        List<Map<String, Object>> orderLevelPromotions = applyOrderLevelPromotions(itemsBySeller);

        BigDecimal originalAmount = settledItems.stream()
                .map(SettledCartItem::originalSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalAmount = settledItems.stream()
                .map(item -> item.subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountAmount = originalAmount.subtract(finalAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        List<Map<String, Object>> settlementDetails = new ArrayList<>();
        for (SettledCartItem item : settledItems) {
            settlementDetails.add(toSettlementDetail(item));
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("selected_count", settledItems.size());
        data.put("original_amount", originalAmount);
        data.put("total_amount", finalAmount);
        data.put("discount_amount", discountAmount);
        data.put("promotion_settlement", settlementDetails);
        data.put("order_level_promotions", orderLevelPromotions);

        return Map.of("code", 200, "message", "预结算成功", "data", data);
    }

    /**
     * 批量下单：将选中的购物车项创建为多个订单（按卖家分组），然后合并为一个支付请求
     */
    @Transactional
    public Map<String, Object> batchPurchase(Integer customerId, List<Integer> cartItemIds, 
                                             String contactName, String contactPhone, 
                                             String deliveryAddress, String note) {
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            return Map.of("code", 400, "message", "请选择要结算的商品", "data", Collections.emptyMap());
        }

        // 查询选中的购物车项
        List<CartItemResponse> cartItems = cartMapper.listByCustomerAndIds(customerId, cartItemIds);
        if (cartItems.isEmpty()) {
            return Map.of("code", 400, "message", "选中的商品不存在或不属于该用户", "data", Collections.emptyMap());
        }

        // 获取客户信息
        Customer customer = customerMapper.findById(customerId);
        if (customer == null) {
            return Map.of("code", 404, "message", "客户不存在", "data", Collections.emptyMap());
        }

        List<SettledCartItem> settledItems = buildSettledItems(cartItems);
        if (settledItems.isEmpty()) {
            return Map.of("code", 400, "message", "没有有效的商品可以下单", "data", Collections.emptyMap());
        }

        // 按 seller_id 分组，同一个卖家的商品合并为一个订单
        Map<Integer, List<SettledCartItem>> itemsBySeller = groupItemsBySeller(settledItems);
        List<Map<String, Object>> orderLevelPromotions = applyOrderLevelPromotions(itemsBySeller);

        BigDecimal originalAmount = settledItems.stream()
                .map(SettledCartItem::originalSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        List<Integer> purchaseIds = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        // 为每个卖家的商品创建一个订单
        for (Map.Entry<Integer, List<SettledCartItem>> entry : itemsBySeller.entrySet()) {
            Integer sellerId = entry.getKey();
            List<SettledCartItem> sellerItems = entry.getValue();

            try {
                // 计算该订单的总金额
                BigDecimal orderTotal = sellerItems.stream()
                        .map(item -> item.subtotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .setScale(2, RoundingMode.HALF_UP);

                // 创建购买意向
                PurchaseIntent intent = new PurchaseIntent();
                intent.setCustomerId(customerId);
                intent.setSellerId(sellerId);
                intent.setCustomerName(contactName != null ? contactName : customer.getUsername());
                intent.setCustomerPhone(contactPhone != null ? contactPhone : customer.getPhone());
                intent.setCustomerAddress(deliveryAddress != null ? deliveryAddress : customer.getDefaultAddress());
                intent.setQuantity(sellerItems.stream().mapToInt(item -> item.cartItem.getQuantity()).sum());
                intent.setTotalAmount(orderTotal);
                intent.setPurchaseStatus("CUSTOMER_ORDERED");
                intent.setSellerNotes(note);
                intent.setCreatedAt(LocalDateTime.now());
                intent.setUpdatedAt(LocalDateTime.now());

                int rows = purchaseIntentMapper.insert(intent);
                if (rows <= 0 || intent.getPurchaseId() == null) {
                    throw new IllegalStateException("创建购买意向失败");
                }

                purchaseIds.add(intent.getPurchaseId());
                totalAmount = totalAmount.add(orderTotal).setScale(2, RoundingMode.HALF_UP);

                // 为每个商品项创建记录
                for (SettledCartItem settled : sellerItems) {
                    CartItemResponse cartItem = settled.cartItem;
                    PurchaseIntentItem item = new PurchaseIntentItem();
                    item.setPurchaseId(intent.getPurchaseId());
                    item.setProductId(cartItem.getProductId());
                    item.setProductName(cartItem.getProductName());
                    item.setProductPrice(settled.finalUnitPrice);
                    item.setQuantity(cartItem.getQuantity());
                    item.setSubtotal(settled.subtotal);

                    int ir = purchaseIntentItemMapper.insert(item);
                    if (ir <= 0) {
                        throw new IllegalStateException("创建购买意向商品项失败");
                    }
                }

            } catch (Exception e) {
                log.error("创建订单失败，sellerId={}", sellerId, e);
                throw new RuntimeException("创建订单失败：" + e.getMessage(), e);
            }
        }

        // 创建合并支付记录
        try {
            Payment payment = paymentService.createBatchPayment(purchaseIds, customerId, totalAmount);
            BigDecimal discountAmount = originalAmount.subtract(totalAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

            Map<String, Object> data = new HashMap<>();
            data.put("payment_id", payment.getPaymentId());
            data.put("purchase_ids", purchaseIds);
            data.put("original_amount", originalAmount);
            data.put("total_amount", totalAmount);
            data.put("discount_amount", discountAmount);
            data.put("payment_status", payment.getPaymentStatus());

            List<Map<String, Object>> promotionSettleDetails = new ArrayList<>();
            List<Integer> purchasedCartItemIds = new ArrayList<>();
            for (SettledCartItem settled : settledItems) {
                purchasedCartItemIds.add(settled.cartItem.getCartItemId());
                promotionSettleDetails.add(toSettlementDetail(settled));
            }
            data.put("promotion_settlement", promotionSettleDetails);
            data.put("order_level_promotions", orderLevelPromotions);

            // 从购物车删除已下单的商品
            cartMapper.deleteByIds(customerId, purchasedCartItemIds);

            return Map.of(
                "code", 200,
                "message", "下单成功，请完成支付",
                "data", data
            );
        } catch (Exception e) {
            log.error("创建支付记录失败", e);
            throw new RuntimeException("创建支付记录失败：" + e.getMessage(), e);
        }
    }

    private List<SettledCartItem> buildSettledItems(List<CartItemResponse> cartItems) {
        List<SettledCartItem> settledItems = new ArrayList<>();
        for (CartItemResponse item : cartItems) {
            Product product = productService.getProductById(item.getProductId());
            if (product == null || product.getSellerId() == null) {
                log.warn("商品 ID {} 没有有效的卖家信息", item.getProductId());
                continue;
            }

            BigDecimal listedUnitPrice = toMoney(item.getUnitPrice());
            if (listedUnitPrice == null) {
                log.warn("商品 ID {} 价格无效，跳过结算", item.getProductId());
                continue;
            }

            Map<String, Object> promotionSettlement = settlePromotionForCheckout(item.getProductId(), listedUnitPrice);
            BigDecimal settledUnitPrice = toMoney(promotionSettlement.get("final_unit_price"));
            if (settledUnitPrice == null) settledUnitPrice = listedUnitPrice;

            Set<Integer> blockedPromotionIds = toPromotionIdSet(promotionSettlement.get("active_promotion_ids"));
            Integer settledPromotionId = toInteger(promotionSettlement.get("promotion_id"));
            if (settledPromotionId != null) {
                blockedPromotionIds.add(settledPromotionId);
            }

            BigDecimal subtotal = settledUnitPrice
                    .multiply(BigDecimal.valueOf(item.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            settledItems.add(new SettledCartItem(item, product.getSellerId(), listedUnitPrice, settledUnitPrice, subtotal, promotionSettlement, blockedPromotionIds));
        }
        return settledItems;
    }

    private Map<Integer, List<SettledCartItem>> groupItemsBySeller(List<SettledCartItem> settledItems) {
        Map<Integer, List<SettledCartItem>> itemsBySeller = new HashMap<>();
        for (SettledCartItem settledItem : settledItems) {
            itemsBySeller.computeIfAbsent(settledItem.sellerId, k -> new ArrayList<>()).add(settledItem);
        }
        return itemsBySeller;
    }

    private List<Map<String, Object>> applyOrderLevelPromotions(Map<Integer, List<SettledCartItem>> itemsBySeller) {
        List<Map<String, Object>> applied = new ArrayList<>();
        for (Map.Entry<Integer, List<SettledCartItem>> entry : itemsBySeller.entrySet()) {
            Map<String, Object> one = applyOrderLevelFullReduction(entry.getKey(), entry.getValue());
            if (one != null) {
                applied.add(one);
            }
        }
        return applied;
    }

    private Map<String, Object> applyOrderLevelFullReduction(Integer sellerId, List<SettledCartItem> sellerItems) {
        Map<Integer, Map<String, Object>> promotionMeta = new HashMap<>();
        Map<Integer, LinkedHashMap<Integer, SettledCartItem>> eligibleItemsByPromotion = new HashMap<>();
        BigDecimal sellerSubtotal = sellerItems.stream()
                .map(item -> item.subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        if (sellerSubtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        for (SettledCartItem item : sellerItems) {
            Map<String, Object> promotionData = promotionService.productPromotions(item.cartItem.getProductId());
            if (promotionData == null || promotionData.isEmpty()) {
                continue;
            }

            Object promotionsRaw = promotionData.get("promotions");
            if (!(promotionsRaw instanceof List<?>)) {
                continue;
            }

            for (Object row : (List<?>) promotionsRaw) {
                if (!(row instanceof Map<?, ?>)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> promotion = (Map<String, Object>) row;

                if (!"FULL_REDUCTION".equalsIgnoreCase(String.valueOf(promotion.get("promotion_type")))) {
                    continue;
                }

                Integer promotionId = toInteger(promotion.get("promotion_id"));
                if (promotionId == null) continue;

                promotionMeta.putIfAbsent(promotionId, promotion);
                // 同一个商品若已享受过该规则，不再重复享受该规则。
                if (item.blockedPromotionIds.contains(promotionId)) {
                    continue;
                }
                eligibleItemsByPromotion
                        .computeIfAbsent(promotionId, k -> new LinkedHashMap<>())
                        .putIfAbsent(item.cartItem.getCartItemId(), item);
            }
        }

        Integer bestPromotionId = null;
        Map<String, Object> bestPromotion = null;
        BigDecimal bestReduction = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal bestEligibleSubtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        LinkedHashMap<Integer, SettledCartItem> bestTargetItems = null;
        Integer bestPriority = Integer.MIN_VALUE;

        for (Map.Entry<Integer, Map<String, Object>> entry : promotionMeta.entrySet()) {
            Integer promotionId = entry.getKey();
            Map<String, Object> promotion = entry.getValue();

            // 订单内任一商品已享受过同规则，则该规则在本次订单级处理中整体跳过。
            boolean consumedByAnyItem = sellerItems.stream()
                    .anyMatch(item -> item.blockedPromotionIds.contains(promotionId));
            if (consumedByAnyItem) {
                continue;
            }

            LinkedHashMap<Integer, SettledCartItem> eligibleItems = eligibleItemsByPromotion.get(promotionId);
            if (eligibleItems == null || eligibleItems.isEmpty()) {
                continue;
            }

            BigDecimal eligibleSubtotal = eligibleItems.values().stream()
                    .map(item -> item.subtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            if (eligibleSubtotal.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal minPurchase = toMoney(promotion.get("min_purchase_amount"));
            if (minPurchase == null) minPurchase = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            // 订单级门槛按卖家子单总额判定。
            if (sellerSubtotal.compareTo(minPurchase) < 0) continue;

            BigDecimal reduction = toMoney(promotion.get("discount_value"));
            if (reduction == null || reduction.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal maxDiscount = toMoney(promotion.get("max_discount_amount"));
            if (maxDiscount != null && reduction.compareTo(maxDiscount) > 0) {
                reduction = maxDiscount;
            }
            if (reduction.compareTo(eligibleSubtotal) > 0) {
                reduction = eligibleSubtotal;
            }
            reduction = reduction.setScale(2, RoundingMode.HALF_UP);
            if (reduction.compareTo(BigDecimal.ZERO) <= 0) continue;

            Integer priority = Optional.ofNullable(toInteger(promotion.get("priority"))).orElse(0);
            boolean better = bestPromotionId == null
                    || priority > bestPriority
                    || (priority.equals(bestPriority) && reduction.compareTo(bestReduction) > 0)
                    || (priority.equals(bestPriority) && reduction.compareTo(bestReduction) == 0 && promotionId < bestPromotionId);

            if (better) {
                bestPromotionId = promotionId;
                bestPromotion = promotion;
                bestReduction = reduction;
                bestEligibleSubtotal = eligibleSubtotal;
                bestTargetItems = eligibleItems;
                bestPriority = priority;
            }
        }

        if (bestPromotionId == null || bestPromotion == null || bestReduction.compareTo(BigDecimal.ZERO) <= 0 || bestTargetItems == null || bestTargetItems.isEmpty()) {
            return null;
        }

        BigDecimal remaining = bestReduction;
        int idx = 0;
        int size = bestTargetItems.size();
        for (SettledCartItem item : bestTargetItems.values()) {
            idx++;
            BigDecimal lineDiscount;
            if (idx == size) {
                lineDiscount = remaining;
            } else {
                lineDiscount = bestReduction
                        .multiply(item.subtotal)
                        .divide(bestEligibleSubtotal, 2, RoundingMode.HALF_UP);
                if (lineDiscount.compareTo(remaining) > 0) {
                    lineDiscount = remaining;
                }
            }

            if (lineDiscount.compareTo(BigDecimal.ZERO) > 0) {
                applyOrderLevelDiscountToItem(item, bestPromotionId, lineDiscount);
                remaining = remaining.subtract(lineDiscount).setScale(2, RoundingMode.HALF_UP);
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("seller_id", sellerId);
        out.put("promotion_id", bestPromotionId);
        out.put("promotion_name", bestPromotion.get("promotion_name"));
        out.put("promotion_type", bestPromotion.get("promotion_type"));
        out.put("seller_subtotal", sellerSubtotal);
        out.put("eligible_amount", bestEligibleSubtotal);
        out.put("eligible_item_count", bestTargetItems.size());
        out.put("discount_amount", bestReduction);
        return out;
    }

    private void applyOrderLevelDiscountToItem(SettledCartItem item, Integer promotionId, BigDecimal lineDiscount) {
        if (item == null || lineDiscount == null || lineDiscount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        item.subtotal = item.subtotal.subtract(lineDiscount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        Integer qty = item.cartItem.getQuantity();
        if (qty != null && qty > 0) {
            item.finalUnitPrice = item.subtotal.divide(BigDecimal.valueOf(qty), 2, RoundingMode.HALF_UP);
        }

        BigDecimal existing = toMoney(item.promotionSettlement.get("order_level_discount_amount"));
        if (existing == null) existing = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        item.promotionSettlement.put("applied", true);
        item.promotionSettlement.put("skipped_reason", null);
        item.promotionSettlement.put("order_level_applied", true);
        item.promotionSettlement.put("order_level_promotion_id", promotionId);
        item.promotionSettlement.put("order_level_discount_amount", existing.add(lineDiscount).setScale(2, RoundingMode.HALF_UP));
        item.promotionSettlement.put("final_unit_price", item.finalUnitPrice);
    }

    private Map<String, Object> toSettlementDetail(SettledCartItem settled) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("cart_item_id", settled.cartItem.getCartItemId());
        detail.put("product_id", settled.cartItem.getProductId());
        detail.put("product_name", settled.cartItem.getProductName());
        detail.put("quantity", settled.cartItem.getQuantity());
        detail.put("original_unit_price", settled.originalUnitPrice);

        BigDecimal originalLine = settled.originalSubtotal();
        BigDecimal finalLine = settled.subtotal;
        BigDecimal lineDiscount = originalLine.subtract(finalLine).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        // 先合并原始促销结算，再用最终行金额相关字段覆盖，避免被旧值覆盖导致前端展示错乱。
        detail.putAll(settled.promotionSettlement);
        detail.put("original_unit_price", settled.originalUnitPrice);
        detail.put("final_unit_price", settled.finalUnitPrice);
        detail.put("line_original_amount", originalLine);
        detail.put("line_final_amount", finalLine);
        detail.put("discount_amount", lineDiscount);
        return detail;
    }

    private Map<String, Object> settlePromotionForCheckout(Integer productId, BigDecimal checkoutUnitPrice) {
        Map<String, Object> settlement = new LinkedHashMap<>();
        settlement.put("applied", false);
        settlement.put("promotion_id", null);
        settlement.put("skipped_reason", "NO_ACTIVE_PROMOTION");
        settlement.put("discount_amount", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        settlement.put("final_unit_price", checkoutUnitPrice);
        settlement.put("active_promotion_ids", Collections.emptyList());

        Map<String, Object> promotionData = promotionService.productPromotions(productId);
        if (promotionData == null || promotionData.isEmpty()) {
            settlement.put("skipped_reason", "PROMOTION_DATA_UNAVAILABLE");
            return settlement;
        }

        boolean hasActivePromotion = Boolean.TRUE.equals(promotionData.get("has_active_promotion"));
        if (!hasActivePromotion) {
            return settlement;
        }

        Set<Integer> activePromotionIds = toPromotionIdSet(promotionData.get("active_promotion_ids"));
        settlement.put("active_promotion_ids", new ArrayList<>(activePromotionIds));
        Map<String, Object> selectedPromotion = null;
        BigDecimal selectedFinalPrice = null;
        boolean hasSameRuleCandidate = false;
        boolean hasUnappliedRuleCandidate = false;

        Object promotionsRaw = promotionData.get("promotions");
        if (promotionsRaw instanceof List<?>) {
            for (Object row : (List<?>) promotionsRaw) {
                if (!(row instanceof Map<?, ?>)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> promotion = (Map<String, Object>) row;
                Integer promotionId = toInteger(promotion.get("promotion_id"));

                if (promotionId != null && activePromotionIds.contains(promotionId)) {
                    hasSameRuleCandidate = true;
                    continue;
                }

                hasUnappliedRuleCandidate = true;
                BigDecimal finalPrice = toMoney(promotion.get("final_price"));
                if (finalPrice == null || finalPrice.compareTo(checkoutUnitPrice) >= 0) {
                    continue;
                }

                if (selectedFinalPrice == null || finalPrice.compareTo(selectedFinalPrice) < 0) {
                    selectedFinalPrice = finalPrice;
                    selectedPromotion = promotion;
                }
            }
        }

        // 兼容：promotions 不可用时，回退到 best_promotion。
        if (selectedPromotion == null) {
            Object bestPromotionRaw = promotionData.get("best_promotion");
            if (bestPromotionRaw instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked")
                Map<String, Object> bestPromotion = (Map<String, Object>) bestPromotionRaw;
                Integer bestPromotionId = toInteger(bestPromotion.get("promotion_id"));
                if (!(bestPromotionId != null && activePromotionIds.contains(bestPromotionId))) {
                    BigDecimal bestFinalPrice = toMoney(bestPromotion.get("final_price"));
                    if (bestFinalPrice != null && bestFinalPrice.compareTo(checkoutUnitPrice) < 0) {
                        selectedPromotion = bestPromotion;
                        selectedFinalPrice = bestFinalPrice;
                    }
                } else {
                    hasSameRuleCandidate = true;
                }
            }
        }

        if (selectedPromotion != null) {
            Integer promotionId = toInteger(selectedPromotion.get("promotion_id"));
            settlement.put("applied", true);
            settlement.put("promotion_id", promotionId);
            settlement.put("skipped_reason", null);
            settlement.put("discount_amount", checkoutUnitPrice.subtract(selectedFinalPrice).setScale(2, RoundingMode.HALF_UP));
            settlement.put("final_unit_price", selectedFinalPrice);
            return settlement;
        }

        if (hasSameRuleCandidate && !hasUnappliedRuleCandidate) {
            settlement.put("skipped_reason", "SAME_PROMOTION_RULE_ALREADY_APPLIED");
        } else {
            settlement.put("skipped_reason", "PROMOTION_NOT_BETTER");
        }

        return settlement;
    }

    private Set<Integer> toPromotionIdSet(Object value) {
        Set<Integer> ids = new HashSet<>();
        if (value == null) {
            return ids;
        }

        if (value instanceof Collection<?>) {
            for (Object item : (Collection<?>) value) {
                Integer id = toInteger(item);
                if (id != null) ids.add(id);
            }
            return ids;
        }

        if (value instanceof String) {
            String cleaned = ((String) value).replaceAll("[\\[\\]\\s]", "");
            if (cleaned.isEmpty()) return ids;
            String[] parts = cleaned.split(",");
            for (String part : parts) {
                Integer id = toInteger(part);
                if (id != null) ids.add(id);
            }
        }
        return ids;
    }

    private Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            String text = String.valueOf(value).trim();
            if (text.isEmpty()) return null;
            return Integer.parseInt(text);
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal toMoney(Object value) {
        if (value == null) return null;
        try {
            if (value instanceof BigDecimal) {
                return ((BigDecimal) value).setScale(2, RoundingMode.HALF_UP);
            }
            if (value instanceof Number) {
                return BigDecimal.valueOf(((Number) value).doubleValue()).setScale(2, RoundingMode.HALF_UP);
            }
            return new BigDecimal(String.valueOf(value)).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return null;
        }
    }

    private static class SettledCartItem {
        private final CartItemResponse cartItem;
        private final Integer sellerId;
        private final BigDecimal originalUnitPrice;
        private BigDecimal finalUnitPrice;
        private BigDecimal subtotal;
        private final Map<String, Object> promotionSettlement;
        private final Set<Integer> blockedPromotionIds;

        private SettledCartItem(CartItemResponse cartItem,
                                Integer sellerId,
                                BigDecimal originalUnitPrice,
                                BigDecimal finalUnitPrice,
                                BigDecimal subtotal,
                                Map<String, Object> promotionSettlement,
                                Set<Integer> blockedPromotionIds) {
            this.cartItem = cartItem;
            this.sellerId = sellerId;
            this.originalUnitPrice = originalUnitPrice;
            this.finalUnitPrice = finalUnitPrice;
            this.subtotal = subtotal;
            this.promotionSettlement = promotionSettlement;
            this.blockedPromotionIds = blockedPromotionIds == null ? new HashSet<>() : blockedPromotionIds;
        }

        private BigDecimal originalSubtotal() {
            Integer qty = cartItem.getQuantity();
            if (qty == null || qty <= 0) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            return originalUnitPrice.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
        }
    }
}
