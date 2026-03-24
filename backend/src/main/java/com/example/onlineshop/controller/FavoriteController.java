package com.example.onlineshop.controller;

import com.example.onlineshop.dto.response.FavoriteResponse;
import com.example.onlineshop.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers/favorites")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    // 添加收藏
    @PostMapping
    public Map<String, Object> addFavorite(@RequestBody Map<String, Integer> body) {
        Integer customerId = body.get("customer_id");
        Integer productId = body.get("product_id");
        FavoriteResponse resp = favoriteService.addFavorite(customerId, productId);
        return Map.of("code", 200, "message", "收藏成功", "data", resp);
    }

    // 查询收藏（分页）
    @GetMapping
    public Map<String, Object> listFavorites(@RequestParam("customer_id") Integer customerId,
                                             @RequestParam(value = "page", defaultValue = "1") int page,
                                             @RequestParam(value = "size", defaultValue = "10") int size) {
        List<FavoriteResponse> items = favoriteService.listFavorites(customerId, page, size);
        return Map.of("code", 200, "message", "success", "data", Map.of(
                "page", page, "size", size, "items", items
        ));
    }

    // 删除收藏（仅本人）
    @DeleteMapping("/{favorite_id}")
    public Map<String, Object> deleteFavorite(@PathVariable("favorite_id") Integer favoriteId,
                                              @RequestParam("customer_id") Integer customerId) {
        boolean ok = favoriteService.deleteFavorite(customerId, favoriteId);
        if (!ok) return Map.of("code", 400, "message", "删除失败：不存在或无权限", "data", null);
        return Map.of("code", 200, "message", "删除成功", "data", Map.of("favorite_id", favoriteId));
    }
}
