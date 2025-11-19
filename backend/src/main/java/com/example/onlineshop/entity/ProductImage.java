// file: backend/src/main/java/com/example/onlineshop/entity/ProductImage.java
package com.example.onlineshop.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品图片（多张）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("image_id")
    private Integer imageId;

    @JsonProperty("product_id")
    private Integer productId;

    @JsonProperty("image_url")
    private String imageUrl;

    @JsonProperty("image_order")
    private Integer imageOrder; // 0 为封面

    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
