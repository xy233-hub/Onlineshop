package com.example.onlineshop.controller;

import com.example.onlineshop.entity.LogisticsProvider;
import com.example.onlineshop.service.LogisticsProviderService;
import com.example.onlineshop.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/logistics")
public class LogisticsController {

    @Autowired
    private LogisticsProviderService logisticsProviderService;

    /**
     * 46. 获取物流商列表
     */
    @GetMapping("/providers")
    public ResponseEntity<Object> getProviders(@RequestParam(value = "type", required = false) String type) {
        try {
            List<LogisticsProvider> providers;
            if (type != null && !type.trim().isEmpty()) {
                providers = logisticsProviderService.getProvidersByType(type);
            } else {
                providers = logisticsProviderService.getAllProviders();
            }

            List<Map<String, Object>> result = providers.stream().map(provider -> {
                Map<String, Object> map = new HashMap<>();
                map.put("provider_id", provider.getProviderId());
                map.put("provider_name", provider.getProviderName());
                map.put("provider_code", provider.getProviderCode());
                map.put("enabled", provider.getEnabled());
                map.put("sort_order", provider.getSortOrder());
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(ResponseUtil.success("查询成功", result));
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseUtil.error("查询失败：" + e.getMessage()));
        }
    }
}