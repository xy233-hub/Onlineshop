
package com.example.onlineshop.dto.request;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;

@Data
public class ImageRequest {
    @JsonProperty("temp_key")
    private String tempKey;

    // 允许为空（如果使用 temp_key 则不需要 imageUrl）
    @JsonProperty("image_url")
    private String imageUrl;

    @JsonProperty("image_order")
    @Min(value = 0, message = "image_order 必须 >= 0")
    private Integer imageOrder = 0;
}

