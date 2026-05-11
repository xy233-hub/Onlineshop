package com.example.onlineshop.controller;

import com.example.onlineshop.dto.response.ApiResponse;
import com.example.onlineshop.dto.response.FavoriteResponse;
import com.example.onlineshop.service.FavoriteService;
import com.example.onlineshop.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ApiResponse> addFavorite(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Integer> body) {
        try {
            Integer customerId = JwtUtil.getCustomerIdFromToken(token);
            if (customerId == null) {
                return ResponseEntity.status(401)
                        .body(new ApiResponse(401, "未授权", null));
            }

            Integer productId = body.get("product_id");
            if (productId == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(400, "product_id 必填", null));
            }

            FavoriteResponse resp = favoriteService.addFavorite(customerId, productId);
            return ResponseEntity.ok(
                    new ApiResponse(200, "收藏成功", resp)
            );
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(400, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "收藏失败：" + e.getMessage(), null));
        }
    }

    // 查询收藏（分页）
    @GetMapping
    public ResponseEntity<ApiResponse> listFavorites(
            @RequestHeader("Authorization") String token,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        try {
            Integer customerId = JwtUtil.getCustomerIdFromToken(token);
            if (customerId == null) {
                return ResponseEntity.status(401)
                        .body(new ApiResponse(401, "未授权", null));
            }

            List<FavoriteResponse> items = favoriteService.listFavorites(customerId, page, size);
            Map<String, Object> data = Map.of(
                    "page", page,
                    "size", size,
                    "items", items
            );
            return ResponseEntity.ok(
                    new ApiResponse(200, "查询成功", data)
            );
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "查询失败：" + e.getMessage(), null));
        }
    }

    // 删除收藏（仅本人）
    @DeleteMapping("/{favorite_id}")
    public ResponseEntity<ApiResponse> deleteFavorite(
            @RequestHeader("Authorization") String token,
            @PathVariable("favorite_id") Integer favoriteId) {
        try {
            Integer customerId = JwtUtil.getCustomerIdFromToken(token);
            if (customerId == null) {
                return ResponseEntity.status(401)
                        .body(new ApiResponse(401, "未授权", null));
            }

            boolean ok = favoriteService.deleteFavorite(customerId, favoriteId);
            if (!ok) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(400, "删除失败：不存在或无权限", null));
            }
            return ResponseEntity.ok(
                    new ApiResponse(200, "删除成功", Map.of("favorite_id", favoriteId))
            );
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse(500, "删除失败：" + e.getMessage(), null));
        }
    }
}