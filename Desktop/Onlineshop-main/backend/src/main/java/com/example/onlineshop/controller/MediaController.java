package com.example.onlineshop.controller;

import com.example.onlineshop.dto.response.ApiResponse;
import com.example.onlineshop.dto.response.TempMediaResponse;
import com.example.onlineshop.service.MediaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    @Autowired
    private MediaService mediaService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "product_id", required = false) Integer productId,
            @RequestParam(value = "purpose", required = false, defaultValue = "gallery") String purpose,
            @RequestParam(value = "display_order", required = false, defaultValue = "0") Integer displayOrder
    ) {
        try {
            if (!"gallery".equals(purpose) && !"embedded".equals(purpose)) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "purpose 只允许 gallery 或 embedded"));
            }

            if (productId == null) {
                // 临时存储，返回 tempKey + 临时 URL
                TempMediaResponse tmp = mediaService.storeTemporary(file, purpose, displayOrder);
                return ResponseEntity.ok(ApiResponse.ok(tmp));
            } else {
                Object result = mediaService.store(file, purpose, productId, displayOrder);
                return ResponseEntity.ok(ApiResponse.ok(result));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(500, "上传失败: " + e.getMessage()));
        }
    }
}

