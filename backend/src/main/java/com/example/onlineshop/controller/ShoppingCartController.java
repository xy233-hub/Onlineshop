// java
package com.example.onlineshop.controller;

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
