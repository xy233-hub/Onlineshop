package com.example.onlineshop.dto.request;

import lombok.Data;

@Data
public class AiImageSearchRequest {
    private String imageUrl;
    private Integer page = 1;
    private Integer size = 10;
}
