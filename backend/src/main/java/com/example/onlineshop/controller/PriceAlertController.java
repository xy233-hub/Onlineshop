package com.example.onlineshop.controller;

import com.example.onlineshop.dto.response.ApiResponse;
import com.example.onlineshop.service.PriceAlertService;
import com.example.onlineshop.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers/price-alerts")
public class PriceAlertController {

    @Autowired
    private PriceAlertService priceAlertService;

    @PostMapping("")
    public ApiResponse create(@RequestHeader("Authorization") String token,
                              @RequestBody Map<String, Object> body) {
        try {
            Integer customerId = JwtUtil.getCustomerIdFromToken(token);
            if (customerId == null) {
                return ApiResponse.error(401, "未授权");
            }

            Integer productId = toInt(body.get("product_id"));
            String alertType = body.get("alert_type") == null ? null : String.valueOf(body.get("alert_type"));
            BigDecimal threshold = toDecimal(body.get("threshold_percentage"));
            Boolean enabled = body.get("is_enabled") == null ? null : Boolean.valueOf(String.valueOf(body.get("is_enabled")));
            if (productId == null || alertType == null) {
                return ApiResponse.error(400, "product_id、alert_type 必填");
            }

            return new ApiResponse(200, "价格提醒配置成功",
                    priceAlertService.createAlert(customerId, productId, alertType, threshold, enabled));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(500, "配置失败: " + e.getMessage());
        }
    }

    @GetMapping("")
    public ApiResponse list(@RequestHeader("Authorization") String token,
                            @RequestParam(value = "is_enabled", required = false) Boolean isEnabled,
                            @RequestParam(value = "alert_type", required = false) String alertType,
                            @RequestParam(value = "page", defaultValue = "1") Integer page,
                            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        Integer customerId = JwtUtil.getCustomerIdFromToken(token);
        if (customerId == null) {
            return ApiResponse.error(401, "未授权");
        }

        return new ApiResponse(200, "查询成功",
                priceAlertService.listAlerts(customerId, isEnabled, alertType, page, size));
    }

    @PutMapping("/{alert_id}")
    public ApiResponse update(@RequestHeader("Authorization") String token,
                              @PathVariable("alert_id") Integer alertId,
                              @RequestBody Map<String, Object> body) {
        Integer customerId = JwtUtil.getCustomerIdFromToken(token);
        if (customerId == null) {
            return ApiResponse.error(401, "未授权");
        }

        try {
            Boolean enabled = body.get("is_enabled") == null ? null : Boolean.valueOf(String.valueOf(body.get("is_enabled")));
            BigDecimal threshold = toDecimal(body.get("threshold_percentage"));
            return new ApiResponse(200, "提醒配置更新成功",
                    priceAlertService.updateAlert(customerId, alertId, enabled, threshold));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(500, "更新失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{alert_id}")
    public ApiResponse delete(@RequestHeader("Authorization") String token,
                              @PathVariable("alert_id") Integer alertId) {
        Integer customerId = JwtUtil.getCustomerIdFromToken(token);
        if (customerId == null) {
            return ApiResponse.error(401, "未授权");
        }

        try {
            return new ApiResponse(200, "提醒配置删除成功", priceAlertService.deleteAlert(customerId, alertId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(500, "删除失败: " + e.getMessage());
        }
    }

    @GetMapping("/notifications")
    public ApiResponse notifications(@RequestHeader("Authorization") String token,
                                     @RequestParam(value = "is_read", required = false) Boolean isRead,
                                     @RequestParam(value = "page", defaultValue = "1") Integer page,
                                     @RequestParam(value = "size", defaultValue = "10") Integer size) {
        Integer customerId = JwtUtil.getCustomerIdFromToken(token);
        if (customerId == null) {
            return ApiResponse.error(401, "未授权");
        }
        return new ApiResponse(200, "查询成功", priceAlertService.listNotifications(customerId, isRead, page, size));
    }

    @PatchMapping("/notifications/{notification_id}/read")
    public ApiResponse markRead(@RequestHeader("Authorization") String token,
                                @PathVariable("notification_id") Integer notificationId) {
        Integer customerId = JwtUtil.getCustomerIdFromToken(token);
        if (customerId == null) {
            return ApiResponse.error(401, "未授权");
        }

        try {
            return new ApiResponse(200, "标记已读成功", priceAlertService.markRead(customerId, notificationId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(500, "标记失败: " + e.getMessage());
        }
    }

    @PatchMapping("/notifications/read-all")
    public ApiResponse markAllRead(@RequestHeader("Authorization") String token) {
        Integer customerId = JwtUtil.getCustomerIdFromToken(token);
        if (customerId == null) {
            return ApiResponse.error(401, "未授权");
        }
        return new ApiResponse(200, "批量标记成功", priceAlertService.markAllRead(customerId));
    }

    @GetMapping("/notifications/unread-count")
    public ApiResponse getUnreadCount(@RequestHeader("Authorization") String token) {
        Integer customerId = JwtUtil.getCustomerIdFromToken(token);
        if (customerId == null) {
            return ApiResponse.error(401, "未授权");
        }
        try {
            int count = priceAlertService.getUnreadCount(customerId);
            return new ApiResponse(200, "查询成功", Map.of("unread_count", count));
        } catch (Exception e) {
            return ApiResponse.error(500, "查询失败: " + e.getMessage());
        }
    }

    @PatchMapping("/notifications/batch-read")
    public ApiResponse batchMarkRead(@RequestHeader("Authorization") String token,
                                     @RequestBody Map<String, List<Integer>> body) {
        Integer customerId = JwtUtil.getCustomerIdFromToken(token);
        if (customerId == null) {
            return ApiResponse.error(401, "未授权");
        }

        List<Integer> notificationIds = body.get("notification_ids");
        if (notificationIds == null || notificationIds.isEmpty()) {
            return ApiResponse.error(400, "通知ID列表不能为空");
        }

        try {
            priceAlertService.batchMarkNotificationsAsRead(customerId, notificationIds);
            return new ApiResponse(200, "批量标记成功", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(500, "操作失败: " + e.getMessage());
        }
    }

    private Integer toInt(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }
}

