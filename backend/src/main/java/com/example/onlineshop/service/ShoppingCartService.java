
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
import com.example.onlineshop.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private CustomerMapper customerMapper;

    @Autowired
    private PurchaseIntentMapper purchaseIntentMapper;

    @Autowired
    private PurchaseIntentItemMapper purchaseIntentItemMapper;

    @Autowired
    private PaymentService paymentService;

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

        // 按 seller_id 分组，同一个卖家的商品合并为一个订单
        Map<Integer, List<CartItemResponse>> itemsBySeller = new HashMap<>();
        for (CartItemResponse item : cartItems) {
            // 查询商品的 seller_id
            Product product = productService.getProductById(item.getProductId());
            if (product == null || product.getSellerId() == null) {
                log.warn("商品 ID {} 没有有效的卖家信息", item.getProductId());
                continue;
            }
            Integer sellerId = product.getSellerId();
            itemsBySeller.computeIfAbsent(sellerId, k -> new ArrayList<>()).add(item);
        }

        if (itemsBySeller.isEmpty()) {
            return Map.of("code", 400, "message", "没有有效的商品可以下单", "data", Collections.emptyMap());
        }

        List<Integer> purchaseIds = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        // 为每个卖家的商品创建一个订单
        for (Map.Entry<Integer, List<CartItemResponse>> entry : itemsBySeller.entrySet()) {
            Integer sellerId = entry.getKey();
            List<CartItemResponse> sellerItems = entry.getValue();

            try {
                // 计算该订单的总金额
                BigDecimal orderTotal = sellerItems.stream()
                        .map(item -> BigDecimal.valueOf(item.getUnitPrice()).multiply(BigDecimal.valueOf(item.getQuantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // 创建购买意向
                PurchaseIntent intent = new PurchaseIntent();
                intent.setCustomerId(customerId);
                intent.setSellerId(sellerId);
                intent.setCustomerName(contactName != null ? contactName : customer.getUsername());
                intent.setCustomerPhone(contactPhone != null ? contactPhone : customer.getPhone());
                intent.setCustomerAddress(deliveryAddress != null ? deliveryAddress : customer.getDefaultAddress());
                intent.setQuantity(sellerItems.stream().mapToInt(CartItemResponse::getQuantity).sum());
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
                totalAmount = totalAmount.add(orderTotal);

                // 为每个商品项创建记录
                for (CartItemResponse cartItem : sellerItems) {
                    PurchaseIntentItem item = new PurchaseIntentItem();
                    item.setPurchaseId(intent.getPurchaseId());
                    item.setProductId(cartItem.getProductId());
                    item.setProductName(cartItem.getProductName());
                    item.setProductPrice(BigDecimal.valueOf(cartItem.getUnitPrice()));
                    item.setQuantity(cartItem.getQuantity());
                    item.setSubtotal(BigDecimal.valueOf(cartItem.getUnitPrice()).multiply(BigDecimal.valueOf(cartItem.getQuantity())));

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
            
            Map<String, Object> data = new HashMap<>();
            data.put("payment_id", payment.getPaymentId());
            data.put("purchase_ids", purchaseIds);
            data.put("total_amount", totalAmount);
            data.put("payment_status", payment.getPaymentStatus());

            // 从购物车删除已下单的商品
            cartMapper.deleteByIds(customerId, cartItemIds);

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
}
