// entity/Seller.java
package com.example.onlineshop.entity;

import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 卖家账号
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seller implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer sellerId;
    private String username;
    private String password; // 建议存储 bcrypt 哈希
    private LocalDateTime createTime;
}
