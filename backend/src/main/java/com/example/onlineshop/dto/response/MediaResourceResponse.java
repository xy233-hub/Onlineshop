package com.example.onlineshop.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

public class MediaResourceResponse {
    @JsonProperty("media_id")
    public Integer mediaId;
    @JsonProperty("product_id")
    public Integer productId;
    @JsonProperty("media_type")
    public String mediaType;
    @JsonProperty("media_url")
    public String mediaUrl;
    @JsonProperty("file_name")
    public String fileName;
    @JsonProperty("file_size")
    public Long fileSize;
    @JsonProperty("mime_type")
    public String mimeType;
    @JsonProperty("display_order")
    public Integer displayOrder;
    @JsonProperty("is_embedded")
    public Boolean isEmbedded;
    @JsonProperty("created_at")
    public String createdAt;

    private static final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public MediaResourceResponse() {}

    public MediaResourceResponse(Integer mediaId, Integer productId, String mediaType, String mediaUrl, String fileName,
                                 Long fileSize, String mimeType, Integer displayOrder, Boolean isEmbedded, LocalDateTime createdAt) {
        this.mediaId = mediaId;
        this.productId = productId;
        this.mediaType = mediaType;
        this.mediaUrl = mediaUrl;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.displayOrder = displayOrder;
        this.isEmbedded = isEmbedded;
        this.createdAt = createdAt != null ? createdAt.format(fmt) : null;
    }
}
