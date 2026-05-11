package com.example.onlineshop.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVector implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("product_id")
    private Integer productId;

    @JsonProperty("vector_type")
    private String vectorType;

    @JsonProperty("image_url")
    private String imageUrl;

    @JsonProperty("vector_text")
    private String vectorText;

    @JsonProperty("vector_data")
    private String vectorData;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
