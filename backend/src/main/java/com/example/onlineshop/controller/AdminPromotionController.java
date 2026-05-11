package com.example.onlineshop.controller;

import com.example.onlineshop.dto.response.ApiResponse;
import com.example.onlineshop.service.PromotionService;
import com.example.onlineshop.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/promotions")
public class AdminPromotionController {

    @Autowired
    private PromotionService promotionService;

    @PostMapping("")
    public ApiResponse create(@RequestHeader("Authorization") String token,
                              @RequestBody Map<String, Object> body) {
        Integer operatorId = JwtUtil.getSellerIdFromToken(token);
        if (operatorId == null) return ApiResponse.error(401, "未授权");

        try {
            return new ApiResponse(200, "促销活动创建成功", promotionService.createPromotion(body, operatorId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(500, "创建失败: " + e.getMessage());
        }
    }

    @GetMapping("")
    public ApiResponse list(@RequestHeader("Authorization") String token,
                            @RequestParam(value = "status", required = false) String status,
                            @RequestParam(value = "promotion_type", required = false) String promotionType,
                            @RequestParam(value = "start_date", required = false) String startDate,
                            @RequestParam(value = "end_date", required = false) String endDate,
                            @RequestParam(value = "page", defaultValue = "1") Integer page,
                            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        Integer operatorId = JwtUtil.getSellerIdFromToken(token);
        if (operatorId == null) return ApiResponse.error(401, "未授权");

        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);
        return new ApiResponse(200, "查询成功", promotionService.listPromotions(status, promotionType, start, end, page, size));
    }

    @GetMapping("/{promotion_id}")
    public ApiResponse detail(@RequestHeader("Authorization") String token,
                              @PathVariable("promotion_id") Integer promotionId) {
        Integer operatorId = JwtUtil.getSellerIdFromToken(token);
        if (operatorId == null) return ApiResponse.error(401, "未授权");

        Map<String, Object> detail = promotionService.getPromotionDetail(promotionId);
        if (detail == null) {
            return ApiResponse.error(404, "促销活动不存在");
        }
        return new ApiResponse(200, "查询成功", detail);
    }

    @PutMapping("/{promotion_id}")
    public ApiResponse update(@RequestHeader("Authorization") String token,
                              @PathVariable("promotion_id") Integer promotionId,
                              @RequestBody Map<String, Object> body) {
        Integer operatorId = JwtUtil.getSellerIdFromToken(token);
        if (operatorId == null) return ApiResponse.error(401, "未授权");

        try {
            return new ApiResponse(200, "促销活动更新成功", promotionService.updatePromotion(promotionId, body, operatorId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(500, "更新失败: " + e.getMessage());
        }
    }

    @PostMapping("/{promotion_id}/activate")
    public ApiResponse activate(@RequestHeader("Authorization") String token,
                                @PathVariable("promotion_id") Integer promotionId,
                                @RequestBody(required = false) Map<String, Object> body) {
        Integer operatorId = JwtUtil.getSellerIdFromToken(token);
        if (operatorId == null) return ApiResponse.error(401, "未授权");

        Boolean confirm = body == null ? null : Boolean.valueOf(String.valueOf(body.get("confirm")));
        if (!Boolean.TRUE.equals(confirm)) {
            return ApiResponse.error(400, "confirm 必须为 true");
        }

        try {
            return new ApiResponse(200, "促销活动激活成功", promotionService.activatePromotion(promotionId, operatorId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(500, "激活失败: " + e.getMessage());
        }
    }

    @PostMapping("/{promotion_id}/end")
    public ApiResponse end(@RequestHeader("Authorization") String token,
                           @PathVariable("promotion_id") Integer promotionId,
                           @RequestBody(required = false) Map<String, Object> body) {
        Integer operatorId = JwtUtil.getSellerIdFromToken(token);
        if (operatorId == null) return ApiResponse.error(401, "未授权");

        try {
            return new ApiResponse(200, "促销活动已结束", promotionService.endPromotion(promotionId, operatorId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(500, "结束失败: " + e.getMessage());
        }
    }

    @PostMapping("/{promotion_id}/cancel")
    public ApiResponse cancel(@RequestHeader("Authorization") String token,
                              @PathVariable("promotion_id") Integer promotionId,
                              @RequestBody Map<String, Object> body) {
        Integer operatorId = JwtUtil.getSellerIdFromToken(token);
        if (operatorId == null) return ApiResponse.error(401, "未授权");

        String reason = body == null ? null : String.valueOf(body.get("reason"));
        if (reason == null || reason.isBlank()) {
            return ApiResponse.error(400, "reason 必填");
        }

        try {
            return new ApiResponse(200, "促销活动已取消", promotionService.cancelPromotion(promotionId, operatorId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(500, "取消失败: " + e.getMessage());
        }
    }

    @GetMapping("/rule-definitions")
    public ApiResponse ruleDefinitions() {
        return new ApiResponse(200, "查询成功", promotionService.getRuleDefinitions());
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            return null;
        }
    }
}
