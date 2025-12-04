// java
package com.example.onlineshop.service;

import com.example.onlineshop.dto.response.CartItemResponse;
import com.example.onlineshop.entity.Customer;
import com.example.onlineshop.entity.PurchaseIntent;
import com.example.onlineshop.entity.PurchaseIntentItem;
import com.example.onlineshop.entity.ShoppingCartItem;
import com.example.onlineshop.mapper.ProductMapper;
import com.example.onlineshop.mapper.PurchaseIntentItemMapper;
import com.example.onlineshop.mapper.PurchaseIntentMapper;
import com.example.onlineshop.mapper.ShoppingCartMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ShoppingCartService {

    @Autowired
    private ShoppingCartMapper cartMapper;

    @Autowired
    private ProductService productService;

    @Autowired
    private PurchaseIntentMapper purchaseIntentMapper;

    @Autowired
    private PurchaseIntentItemMapper purchaseIntentItemMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private CustomerService customerService;

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
     * 批量从购物车下单：为所有成功校验的购物车项统一创建一个 purchase_intent，并为每项写入 purchase_intent_items，
     * 成功后删除对应购物车项。部分项失败时不会回滚成功项。
     */
    @Transactional
    public Map<String, Object> batchPurchase(Integer customerId, List<Integer> cartItemIds,
                                             String contactName, String contactPhone, String deliveryAddress, String note) {
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            return Map.of("code", 400, "message", "cart_item_ids 不能为空", "data", Collections.emptyMap());
        }

        Customer customer = customerService.findById(customerId);

        List<Map<String, Object>> successItems = new ArrayList<>();
        List<Map<String, Object>> failedItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<Integer> deletedCartIds = new ArrayList<>();

        // 临时收集可创建的项
        class ToCreate {
            Integer cartItemId;
            Integer productId;
            Integer quantity;
            BigDecimal unitPrice;
            BigDecimal subtotal;
            String productName;
        }
        List<ToCreate> toCreate = new ArrayList<>();

        // 先逐项校验并收集可创建项，不做删除/插入
        for (Integer cartItemId : cartItemIds) {
            try {
                ShoppingCartItem cartItem = cartMapper.findByIdAndCustomer(cartItemId, customerId);
                if (cartItem == null) {
                    failedItems.add(Map.of("cart_item_id", cartItemId, "reason", "购物车项不存在或不属于当前用户"));
                    continue;
                }

                Integer productId = cartItem.getProductId();
                Integer qty = cartItem.getQuantity();
                if (!productService.isProductOnline(productId)) {
                    failedItems.add(Map.of("cart_item_id", cartItemId, "product_id", productId, "reason", "商品已下架或不可购买"));
                    continue;
                }

                Integer stock = productService.getProductStock(productId);
                if (stock == null || stock < qty) {
                    failedItems.add(Map.of("cart_item_id", cartItemId, "product_id", productId, "reason", "商品库存不足"));
                    continue;
                }

                var product = productMapper.findById(productId);
                if (product == null) {
                    failedItems.add(Map.of("cart_item_id", cartItemId, "product_id", productId, "reason", "商品不存在"));
                    continue;
                }
                BigDecimal unitPrice = product.getPrice() == null ? BigDecimal.ZERO : product.getPrice();
                BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(qty));

                ToCreate tc = new ToCreate();
                tc.cartItemId = cartItemId;
                tc.productId = productId;
                tc.quantity = qty;
                tc.unitPrice = unitPrice;
                tc.subtotal = subtotal;
                tc.productName = product.getProductName();
                toCreate.add(tc);

                totalAmount = totalAmount.add(subtotal);
            } catch (Exception ex) {
                failedItems.add(Map.of("cart_item_id", cartItemId, "reason", "处理异常: " + ex.getMessage()));
            }
        }

        // 如果没有任何可创建项，直接返回失败信息（保持不回滚任何东西）
        if (toCreate.isEmpty()) {
            Map<String, Object> dataEmpty = new HashMap<>();
            dataEmpty.put("success_items", Collections.emptyList());
            dataEmpty.put("failed_items", failedItems);
            dataEmpty.put("deleted_cart_item_ids", Collections.emptyList());
            dataEmpty.put("total_amount", BigDecimal.ZERO);
            dataEmpty.put("success_count", 0);
            dataEmpty.put("failed_count", failedItems.size());
            return Map.of("code", 200, "message", "部分/全部商品无法下单", "data", dataEmpty);
        }

        // 创建单个 PurchaseIntent 代表整笔批量下单
        PurchaseIntent intent = new PurchaseIntent();
        intent.setCustomerId(customerId);
        String finalName = (contactName == null || contactName.isBlank()) && customer != null ? customer.getUsername() : contactName;
        String finalPhone = (contactPhone == null || contactPhone.isBlank()) && customer != null ? customer.getPhone() : contactPhone;
        String finalAddress = (deliveryAddress == null || deliveryAddress.isBlank()) && customer != null ? customer.getDefaultAddress() : deliveryAddress;
        intent.setCustomerName(finalName);
        intent.setCustomerPhone(finalPhone);
        intent.setCustomerAddress(finalAddress);
        // 总数量为所有项数量之和
        int totalQty = toCreate.stream().mapToInt(tc -> tc.quantity).sum();
        intent.setQuantity(totalQty);
        intent.setTotalAmount(totalAmount);
        intent.setPurchaseStatus("CUSTOMER_ORDERED");
        intent.setSellerNotes(note);
        intent.setCreatedAt(LocalDateTime.now());
        intent.setUpdatedAt(LocalDateTime.now());

        int irIntent = purchaseIntentMapper.insert(intent);
        if (irIntent <= 0 || intent.getPurchaseId() == null) {
            // 创建总单失败，返回所有项失败
            Map<String, Object> dataFail = new HashMap<>();
            dataFail.put("success_items", Collections.emptyList());
            dataFail.put("failed_items", failedItems);
            dataFail.put("deleted_cart_item_ids", Collections.emptyList());
            dataFail.put("total_amount", BigDecimal.ZERO);
            dataFail.put("success_count", 0);
            dataFail.put("failed_count", failedItems.size() + toCreate.size());
            return Map.of("code", 500, "message", "创建购买意向失败", "data", dataFail);
        }

        // 逐项写入 purchase_intent_items，并在成功时删除对应购物车项
        for (ToCreate tc : toCreate) {
            try {
                PurchaseIntentItem item = new PurchaseIntentItem();
                item.setPurchaseId(intent.getPurchaseId());
                item.setProductId(tc.productId);
                item.setProductName(tc.productName);
                item.setProductPrice(tc.unitPrice);
                item.setQuantity(tc.quantity);
                item.setSubtotal(tc.subtotal);

                int ir = purchaseIntentItemMapper.insert(item);
                if (ir <= 0) {
                    failedItems.add(Map.of("cart_item_id", tc.cartItemId, "product_id", tc.productId, "reason", "创建购买意向商品项失败"));
                    continue;
                }

                // 删除购物车项
                cartMapper.deleteById(tc.cartItemId);
                deletedCartIds.add(tc.cartItemId);

                Map<String, Object> success = new HashMap<>();
                success.put("purchase_id", intent.getPurchaseId());
                success.put("product_id", tc.productId);
                success.put("product_name", tc.productName);
                success.put("quantity", tc.quantity);
                success.put("unit_price", tc.unitPrice);
                success.put("subtotal", tc.subtotal);
                success.put("created_at", intent.getCreatedAt());
                successItems.add(success);
            } catch (Exception ex) {
                failedItems.add(Map.of("cart_item_id", tc.cartItemId, "product_id", tc.productId, "reason", "处理异常: " + ex.getMessage()));
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("order_id", intent.getPurchaseId());
        data.put("success_items", successItems);
        data.put("failed_items", failedItems);
        data.put("deleted_cart_item_ids", deletedCartIds);
        data.put("total_amount", totalAmount);
        data.put("success_count", successItems.size());
        data.put("failed_count", failedItems.size());

        return Map.of("code", 200, "message", "批量下单完成", "data", data);
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
}
