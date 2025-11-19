// file: backend/src/main/java/com/example/onlineshop/entity/MediaResource.java
package com.example.onlineshop.entity;

import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 媒体资源（图片/视频/音频）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaResource implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer mediaId;
    private Integer productId;
    private String mediaType;
    private String mediaUrl;
    private String fileName;
    private Integer fileSize;
    private String mimeType;
    private Integer displayOrder;
    private Boolean isEmbedded;
    private LocalDateTime createdAt;

    // 手动添加getter和setter方法
    public Integer getMediaId() {
        return mediaId;
    }

    public void setMediaId(Integer mediaId) {
        this.mediaId = mediaId;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getFileSize() {
        return fileSize;
    }

    public void setFileSize(Integer fileSize) {
        this.fileSize = fileSize;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Boolean getIsEmbedded() {
        return isEmbedded;
    }

    public void setIsEmbedded(Boolean isEmbedded) {
        this.isEmbedded = isEmbedded;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
