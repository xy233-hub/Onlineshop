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
}
