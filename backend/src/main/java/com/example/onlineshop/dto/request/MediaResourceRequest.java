
package com.example.onlineshop.dto.request;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

@Data
public class MediaResourceRequest {
    @JsonProperty("media_type")
    private String mediaType; // image|video|audio

    @JsonProperty("media_url")
    private String mediaUrl;

    @JsonProperty("file_name")
    private String fileName;

    @JsonProperty("file_size")
    private Integer fileSize;

    @JsonProperty("mime_type")
    private String mimeType;

    @JsonProperty("display_order")
    private Integer displayOrder; // 默认 0

    @JsonProperty("is_embedded")
    private Boolean isEmbedded; // 默认 false

    @JsonProperty("temp_key")
    private String tempKey;

    private LocalDateTime createdAt;
}


