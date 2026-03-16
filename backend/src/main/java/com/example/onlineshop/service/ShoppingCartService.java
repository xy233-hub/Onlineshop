
package com.example.onlineshop.service;

import com.example.onlineshop.dto.response.CartItemResponse;
import com.example.onlineshop.entity.ShoppingCartItem;
import com.example.onlineshop.mapper.ShoppingCartMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ShoppingCartService {

    @Autowired
    private ShoppingCartMapper cartMapper;

    @Autowired
    private ProductService productService;

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
}
