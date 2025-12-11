// file: backend/src/main/java/com/example/onlineshop/entity/Category.java
package com.example.onlineshop.entity;

import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品类别（支持两级）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer categoryId;
    private Integer parentId; // null 表示一级类别
    private String categoryName;
    private Integer categoryLevel; // 1 或 2
    private LocalDateTime createdAt;
}

