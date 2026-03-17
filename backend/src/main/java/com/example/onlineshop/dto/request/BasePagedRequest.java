package com.example.onlineshop.dto.request;

import lombok.Data;

@Data
public class BasePagedRequest {
    protected Integer page = 1;
    protected Integer size = 10;
}