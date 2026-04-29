package com.example.onlineshop.controller;

import com.example.onlineshop.dto.request.PriceAlertRequest;
import com.example.onlineshop.service.PriceAlertService;
import com.example.onlineshop.util.JwtUtil;
import com.example.onlineshop.util.ResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
public class PriceAlertController {

    @Autowired
    private PriceAlertService priceAlertService;

    @PostMapping("/price-alerts")
    public Object createPriceAlert(@RequestBody PriceAlertRequest request, HttpServletRequest httpRequest) {
        try {
            String token = httpRequest.getHeader("Authorization");
            if (token == null || token.trim().isEmpty()) {
                return ResponseUtil.custom(401, "未登录或令牌无效", null);
            }

            Integer customerId = JwtUtil.getCustomerIdFromToken(token);
            if (customerId == null) {
                return ResponseUtil.custom(401, "无效的令牌", null);
            }

            if (request.getProductId() == null || request.getAlertType() == null) {
                return ResponseUtil.custom(400, "商品ID和提醒类型必填", null);
            }

            var response = priceAlertService.createPriceAlert(customerId, request);
            return ResponseUtil.success("价格提醒配置成功", response);
        } catch (IllegalArgumentException e) {
            return ResponseUtil.custom(400, e.getMessage(), null);
        } catch (Exception e) {
            return ResponseUtil.error("配置失败: " + e.getMessage());
        }
    }

    @GetMapping("/price-alerts")
    public Object getPriceAlerts(
            HttpServletRequest httpRequest,
            @RequestParam(value = "is_enabled", required = false) Boolean isEnabled,
            @RequestParam(value = "alert_type", required = false) String alertType,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        try {
            String token = httpRequest.getHeader("Authorization");
            if (token == null || token.trim().isEmpty()) {
                return ResponseUtil.custom(401, "未登录或令牌无效", null);
            }

            Integer customerId = JwtUtil.getCustomerIdFromToken(token);
            if (customerId == null) {
                return ResponseUtil.custom(401, "无效的令牌", null);
            }

            Map<String, Object> data = priceAlertService.getPriceAlerts(customerId, isEnabled, alertType, page, size);
            return ResponseUtil.success("查询成功", data);
        } catch (Exception e) {
            return ResponseUtil.error("查询失败: " + e.getMessage());
        }
    }

    @PutMapping("/price-alerts/{alert_id}")
    public Object updatePriceAlert(
            @PathVariable("alert_id") Integer alertId,
            @RequestBody PriceAlertRequest request,
            HttpServletRequest httpRequest) {
        try {
            String token = httpRequest.getHeader("Authorization");
            if (token == null || token.trim().isEmpty()) {
                return ResponseUtil.custom(401, "未登录或令牌无效", null);
            }

            Integer customerId = JwtUtil.getCustomerIdFromToken(token);
            if (customerId == null) {
                return ResponseUtil.custom(401, "无效的令牌", null);
            }

            var response = priceAlertService.updatePriceAlert(customerId, alertId, request);
            return ResponseUtil.success("更新成功", response);
        } catch (IllegalArgumentException e) {
            return ResponseUtil.custom(400, e.getMessage(), null);
        } catch (Exception e) {
            return ResponseUtil.error("更新失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/price-alerts/{alert_id}")
    public Object deletePriceAlert(
            @PathVariable("alert_id") Integer alertId,
            HttpServletRequest httpRequest) {
        try {
            String token = httpRequest.getHeader("Authorization");
            if (token == null || token.trim().isEmpty()) {
                return ResponseUtil.custom(401, "未登录或令牌无效", null);
            }

            Integer customerId = JwtUtil.getCustomerIdFromToken(token);
            if (customerId == null) {
                return ResponseUtil.custom(401, "无效的令牌", null);
            }

            priceAlertService.deletePriceAlert(customerId, alertId);
            return ResponseUtil.success("删除成功", null);
        } catch (IllegalArgumentException e) {
            return ResponseUtil.custom(400, e.getMessage(), null);
        } catch (Exception e) {
            return ResponseUtil.error("删除失败: " + e.getMessage());
        }
    }

    @GetMapping("/price-alerts/notifications")
    public Object getNotifications(
            HttpServletRequest httpRequest,
            @RequestParam(value = "is_read", required = false) Boolean isRead,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        try {
            String token = httpRequest.getHeader("Authorization");
            if (token == null || token.trim().isEmpty()) {
                return ResponseUtil.custom(401, "未登录或令牌无效", null);
            }

            Integer customerId = JwtUtil.getCustomerIdFromToken(token);
            if (customerId == null) {
                return ResponseUtil.custom(401, "无效的令牌", null);
            }

            Map<String, Object> data = priceAlertService.getNotifications(customerId, isRead, page, size);
            return ResponseUtil.success("查询成功", data);
        } catch (Exception e) {
            return ResponseUtil.error("查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/price-alerts/notifications/unread-count")
    public Object getUnreadCount(HttpServletRequest httpRequest) {
        try {
            String token = httpRequest.getHeader("Authorization");
            if (token == null || token.trim().isEmpty()) {
                return ResponseUtil.custom(401, "未登录或令牌无效", null);
            }

            Integer customerId = JwtUtil.getCustomerIdFromToken(token);
            if (customerId == null) {
                return ResponseUtil.custom(401, "无效的令牌", null);
            }

            int count = priceAlertService.getUnreadCount(customerId);
            return ResponseUtil.success("查询成功", Map.of("unread_count", count));
        } catch (Exception e) {
            return ResponseUtil.error("查询失败: " + e.getMessage());
        }
    }

    @PatchMapping("/price-alerts/notifications/{notification_id}/read")
    public Object markNotificationAsRead(
            @PathVariable("notification_id") Integer notificationId,
            HttpServletRequest httpRequest) {
        try {
            String token = httpRequest.getHeader("Authorization");
            if (token == null || token.trim().isEmpty()) {
                return ResponseUtil.custom(401, "未登录或令牌无效", null);
            }

            Integer customerId = JwtUtil.getCustomerIdFromToken(token);
            if (customerId == null) {
                return ResponseUtil.custom(401, "无效的令牌", null);
            }

            priceAlertService.markNotificationAsRead(customerId, notificationId);
            return ResponseUtil.success("标记成功", null);
        } catch (IllegalArgumentException e) {
            return ResponseUtil.custom(400, e.getMessage(), null);
        } catch (Exception e) {
            return ResponseUtil.error("操作失败: " + e.getMessage());
        }
    }

    @PatchMapping("/price-alerts/notifications/batch-read")
    public Object batchMarkNotificationsAsRead(
            @RequestBody Map<String, List<Integer>> body,
            HttpServletRequest httpRequest) {
        try {
            String token = httpRequest.getHeader("Authorization");
            if (token == null || token.trim().isEmpty()) {
                return ResponseUtil.custom(401, "未登录或令牌无效", null);
            }

            Integer customerId = JwtUtil.getCustomerIdFromToken(token);
            if (customerId == null) {
                return ResponseUtil.custom(401, "无效的令牌", null);
            }

            List<Integer> notificationIds = body.get("notification_ids");
            if (notificationIds == null || notificationIds.isEmpty()) {
                return ResponseUtil.custom(400, "通知ID列表不能为空", null);
            }

            priceAlertService.batchMarkNotificationsAsRead(customerId, notificationIds);
            return ResponseUtil.success("批量标记成功", null);
        } catch (IllegalArgumentException e) {
            return ResponseUtil.custom(400, e.getMessage(), null);
        } catch (Exception e) {
            return ResponseUtil.error("操作失败: " + e.getMessage());
        }
    }

    @PatchMapping("/price-alerts/notifications/read-all")
    public Object markAllNotificationsAsRead(
            @RequestBody Map<String, Integer> body,
            HttpServletRequest httpRequest) {
        try {
            String token = httpRequest.getHeader("Authorization");
            if (token == null || token.trim().isEmpty()) {
                return ResponseUtil.custom(401, "未登录或令牌无效", null);
            }

            Integer customerId = JwtUtil.getCustomerIdFromToken(token);
            if (customerId == null) {
                return ResponseUtil.custom(401, "无效的令牌", null);
            }

            // 校验请求体中的 customer_id 是否与令牌一致
            Integer bodyCustomerId = body.get("customer_id");
            if (bodyCustomerId != null && !bodyCustomerId.equals(customerId)) {
                return ResponseUtil.custom(403, "无权操作其他用户的通知", null);
            }

            int markedCount = priceAlertService.markAllNotificationsAsRead(customerId);
            
            Map<String, Object> data = new java.util.HashMap<>();
            data.put("marked_count", markedCount);
            data.put("marked_at", java.time.LocalDateTime.now());
            
            return ResponseUtil.success("批量标记成功", data);
        } catch (Exception e) {
            return ResponseUtil.error("操作失败: " + e.getMessage());
        }
    }
}