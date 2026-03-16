// java
package com.example.onlineshop.controller;

import com.example.onlineshop.service.FavoriteService;
import com.example.onlineshop.service.ShoppingCartService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/customers/cart")
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService cartService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FavoriteService favoriteService;

    // 添加购物车（单个商品）
    @PostMapping("/items")
    public Map<String, Object> addItem(@RequestBody Map<String, Object> body) {
        Integer customerId = parseInteger(body.get("customer_id"));
        Integer productId = parseInteger(body.get("product_id"));
        Integer quantity = parseInteger(body.get("quantity"));

        if (customerId == null || productId == null) {
            return Map.of("code", 400, "message", "参数缺失或类型错误: customer_id/product_id 必填且为数字", "data", Collections.emptyMap());
        }
        return cartService.addItem(customerId, productId, quantity);
    }

    // 查询购物车（分页）
    @GetMapping("/items")
    public Map<String, Object> listItems(@RequestParam("customer_id") Integer customerId,
                                         @RequestParam(value = "page", required = false, defaultValue = "1") int page,
                                         @RequestParam(value = "size", required = false, defaultValue = "10") int size,
                                         @RequestParam(value = "filter_stock", required = false) Boolean filterStock,
                                         @RequestParam(value = "sort_by", required = false) String sortBy,
                                         @RequestParam(value = "order", required = false) String order) {
        if (customerId == null) return Map.of("code", 400, "message", "customer_id 必填", "data", Collections.emptyMap());
        return cartService.listItems(customerId, page, size, filterStock, sortBy, order);
    }

    // 删除购物车项（单/批量），DELETE 支持请求体
    @DeleteMapping("/items")
    public Map<String, Object> deleteItems(@RequestBody Map<String, Object> body) {
        Integer customerId = parseInteger(body.get("customer_id"));
        List<Integer> ids = parseListOfIntegers(body.get("cart_item_ids"));

        if (customerId == null || ids == null || ids.isEmpty()) {
            return Map.of("code", 400, "message", "参数缺失或 cart_item_ids 为空或格式错误", "data", Collections.emptyMap());
        }
        return cartService.deleteItems(customerId, ids);
    }

    // 批量将购物车项转换为收藏（接口 23）
    @PostMapping("/batch-convert-favorite")
    public Map<String, Object> batchConvertFavorite(@RequestBody Map<String, Object> body) {
        Integer customerId = parseInteger(body.get("customer_id"));
        List<Integer> cartItemIds = parseListOfIntegers(body.get("cart_item_ids"));

        if (customerId == null || cartItemIds == null || cartItemIds.isEmpty()) {
            return Map.of("code", 400, "message", "参数缺失或 cart_item_ids 为空或格式错误", "data", Collections.emptyMap());
        }

        // 查询购物车项以获取 product_id / product_name / cart_item_id 等信息
        List<Map<String, Object>> cartItems = cartService.getItemsByIds(customerId, cartItemIds);

        // 提取 productIds（保持顺序/去重）
        List<Integer> productIds = cartItems.stream()
                .map(m -> ((Number) m.get("product_id")).intValue())
                .distinct()
                .collect(Collectors.toList());

        // 调用 FavoriteService 进行批量添加收藏（不删除购物车项）
        Map<String, Object> favResult = favoriteService.batchAddFavorites(customerId, productIds);

        // 准备返回数据，尽量把失败项与原 cart_item_id/product_name 关联起来
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> converted = (List<Map<String, Object>>) favResult.getOrDefault("converted_favorites", Collections.emptyList());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> failed = (List<Map<String, Object>>) favResult.getOrDefault("failed_items", Collections.emptyList());

        // 建立 product_id -> cartItem 映射（若存在多个 cart_item 对应同一 product，可取第一个或合并）
        Map<Integer, Map<String, Object>> pidToCart = new HashMap<>();
        for (Map<String, Object> ci : cartItems) {
            Integer pid = ((Number) ci.get("product_id")).intValue();
            pidToCart.putIfAbsent(pid, ci);
        }

        List<Map<String, Object>> failedItems = new ArrayList<>();
        for (Map<String, Object> f : failed) {
            Map<String, Object> item = new HashMap<>();
            Integer pid = f.get("product_id") == null ? null : ((Number) f.get("product_id")).intValue();
            item.put("product_id", pid);
            Map<String, Object> match = pid == null ? null : pidToCart.get(pid);
            item.put("product_name", match == null ? null : match.get("product_name"));
            if (match != null && match.get("cart_item_id") != null) {
                item.put("cart_item_id", match.get("cart_item_id"));
            }
            item.put("reason", f.get("reason"));
            failedItems.add(item);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("converted_count", favResult.getOrDefault("converted_count", converted.size()));
        data.put("failed_count", favResult.getOrDefault("failed_count", failedItems.size()));
        data.put("converted_favorites", converted);
        data.put("failed_items", failedItems);

        // 不再删除购物车项，前端若需清理购物车由客户端主动调用删除接口
        return Map.of("code", 200, "message", "转换完成", "data", data);
    }

    // 辅助方法：安全解析 Integer（支持 Number / String）
    private Integer parseInteger(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        if (obj instanceof String) {
            String s = ((String) obj).trim();
            if (s.isEmpty()) return null;
            try {
                return Integer.valueOf(s);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    // 辅助方法：解析可能为 List<Integer>、JSON 数组字符串（例如 "[1,2]"）或逗号分隔字符串（"1,2"）
    private List<Integer> parseListOfIntegers(Object obj) {
        if (obj == null) return null;

        if (obj instanceof List) {
            return ((List<?>) obj).stream()
                    .map(this::parseInteger)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        if (obj instanceof String) {
            String s = ((String) obj).trim();
            if (s.isEmpty()) return null;

            // 尝试用 Jackson 解析 JSON 数组字符串
            try {
                return objectMapper.readValue(s, new TypeReference<List<Integer>>() {});
            } catch (IOException ignored) {
            }

            // 回退：移除方括号后按逗号分割
            String cleaned = s.replaceAll("[\\[\\]\\s]", "");
            if (cleaned.isEmpty()) return null;
            String[] parts = cleaned.split(",");
            List<Integer> result = new ArrayList<>();
            for (String p : parts) {
                Integer v = parseInteger(p);
                if (v != null) result.add(v);
            }
            return result.isEmpty() ? null : result;
        }

        return null;
    }
}
