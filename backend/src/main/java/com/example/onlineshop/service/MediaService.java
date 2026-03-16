package com.example.onlineshop.service;

import com.example.onlineshop.entity.MediaResource;
import com.example.onlineshop.entity.ProductImage;
import com.example.onlineshop.mapper.MediaResourceMapper;
import com.example.onlineshop.mapper.ProductImageMapper;
import com.example.onlineshop.dto.response.MediaResourceResponse;
import com.example.onlineshop.dto.response.ProductImageResponse;
import com.example.onlineshop.dto.response.TempMediaResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

@Service
public class MediaService {

    private final ProductImageMapper productImageMapper;
    private final MediaResourceMapper mediaResourceMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${media.upload-dir}")
    private String uploadDir;

    @Value("${media.base-url}")
    private String baseUrl;

    public MediaService(ProductImageMapper productImageMapper, MediaResourceMapper mediaResourceMapper) {
        this.productImageMapper = productImageMapper;
        this.mediaResourceMapper = mediaResourceMapper;
    }

    // 现有逻辑：直接存储并写入关联表（保持原实现）
    public Object store(MultipartFile file, String purpose, Integer productId, Integer displayOrder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件为空");
        }

        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);

        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.'));
        }
        String filename = UUID.randomUUID().toString() + ext;
        Path target = dir.resolve(filename);

        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        String mediaUrl = baseUrl.endsWith("/") ? baseUrl + filename : baseUrl + "/" + filename;

        String contentType = file.getContentType();
        String mediaType = "file";
        if (contentType != null) {
            if (contentType.startsWith("image")) mediaType = "image";
            else if (contentType.startsWith("video")) mediaType = "video";
            else if (contentType.startsWith("audio")) mediaType = "audio";
        }

        LocalDateTime createdAt = LocalDateTime.now();
        int order = displayOrder == null ? 0 : displayOrder;

        if ("gallery".equals(purpose)) {
            ProductImage pi = ProductImage.builder()
                    .productId(productId)
                    .imageUrl(mediaUrl)
                    .imageOrder(order)
                    .build();
            productImageMapper.insert(pi);
            return new ProductImageResponse(pi.getImageId(), pi.getProductId(), pi.getImageUrl(), pi.getImageOrder(), createdAt);
        } else {
            MediaResource mr = MediaResource.builder()
                    .productId(productId)
                    .mediaType(mediaType)
                    .mediaUrl(mediaUrl)
                    .fileName(original)
                    .fileSize((int) file.getSize())
                    .mimeType(contentType)
                    .displayOrder(order)
                    .isEmbedded(true)
                    .createdAt(createdAt)
                    .build();
            mediaResourceMapper.insert(mr);
            return new MediaResourceResponse(mr.getMediaId(), mr.getProductId(), mr.getMediaType(), mr.getMediaUrl(),
                    mr.getFileName(), (long) mr.getFileSize(), mr.getMimeType(), mr.getDisplayOrder(), mr.getIsEmbedded(), createdAt);
        }
    }

    // 临时保存文件，返回 tempKey + 临时 URL。会在 uploadDir/temp 下写入文件和 meta 文件。
    public TempMediaResponse storeTemporary(MultipartFile file, String purpose, Integer displayOrder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件为空");
        }

        Path tempDir = Paths.get(uploadDir, "temp").toAbsolutePath().normalize();
        Files.createDirectories(tempDir);

        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.'));
        }
        String key = UUID.randomUUID().toString();
        String filename = key + ext;
        Path target = tempDir.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        // 写 meta 文件，后续 associate 使用
        Map<String, Object> meta = new HashMap<>();
        meta.put("filename", filename);
        meta.put("originalName", original);
        meta.put("purpose", purpose);
        meta.put("displayOrder", displayOrder == null ? 0 : displayOrder);
        meta.put("contentType", file.getContentType());
        meta.put("fileSize", file.getSize());

        Path metaPath = tempDir.resolve(key + ".meta");
        Files.writeString(metaPath, objectMapper.writeValueAsString(meta), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        String url = (baseUrl.endsWith("/") ? baseUrl : baseUrl + "/") + "temp/" + filename;
        return new TempMediaResponse(key, url, file.getSize(), file.getContentType(), purpose);
    }

    // 在商品创建后，用 tempKey 关联到 product：读取 meta，把文件移动到正式目录并写入对应表
    public void associateTemporaryToProduct(String tempKey, Integer productId) throws Exception {
        if (tempKey == null || tempKey.isBlank()) {
            throw new IllegalArgumentException("tempKey 为空");
        }
        Path tempDir = Paths.get(uploadDir, "temp").toAbsolutePath().normalize();
        Path metaPath = tempDir.resolve(tempKey + ".meta");
        if (!Files.exists(metaPath)) {
            throw new IllegalArgumentException("临时资源不存在或已过期: " + tempKey);
        }
        Map<?,?> meta = objectMapper.readValue(Files.readString(metaPath), Map.class);
        String filename = (String) meta.get("filename");
        String purpose = (String) meta.get("purpose");
        Integer displayOrder = meta.get("displayOrder") == null ? 0 : ((Number) meta.get("displayOrder")).intValue();
        String originalName = (String) meta.get("originalName");
        String contentType = meta.get("contentType") == null ? null : (String) meta.get("contentType");
        Long fileSize = meta.get("fileSize") == null ? 0L : ((Number) meta.get("fileSize")).longValue();

        Path tempFile = tempDir.resolve(filename);
        if (!Files.exists(tempFile)) {
            // 清理 meta 并抛错
            Files.deleteIfExists(metaPath);
            throw new IllegalArgumentException("临时文件丢失: " + filename);
        }

        // 移动到正式目录并生成新的唯一文件名
        Path destDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(destDir);
        String ext = "";
        if (filename != null && filename.contains(".")) {
            ext = filename.substring(filename.lastIndexOf('.'));
        }
        String newFilename = UUID.randomUUID().toString() + ext;
        Path destFile = destDir.resolve(newFilename);
        Files.move(tempFile, destFile, StandardCopyOption.REPLACE_EXISTING);

        String mediaUrl = baseUrl.endsWith("/") ? baseUrl + newFilename : baseUrl + "/" + newFilename;

        if ("gallery".equals(purpose)) {
            ProductImage pi = ProductImage.builder()
                    .productId(productId)
                    .imageUrl(mediaUrl)
                    .imageOrder(displayOrder)
                    .build();
            productImageMapper.insert(pi);
        } else { // embedded -> media_resources
            String mediaType = "file";
            if (contentType != null) {
                if (contentType.startsWith("image")) mediaType = "image";
                else if (contentType.startsWith("video")) mediaType = "video";
                else if (contentType.startsWith("audio")) mediaType = "audio";
            }
            MediaResource mr = MediaResource.builder()
                    .productId(productId)
                    .mediaType(mediaType)
                    .mediaUrl(mediaUrl)
                    .fileName(originalName)
                    .fileSize(fileSize == null ? 0 : fileSize.intValue())
                    .mimeType(contentType)
                    .displayOrder(displayOrder)
                    .isEmbedded(true)
                    .build();
            mediaResourceMapper.insert(mr);
        }

        // 删除 meta
        Files.deleteIfExists(metaPath);
    }
}
