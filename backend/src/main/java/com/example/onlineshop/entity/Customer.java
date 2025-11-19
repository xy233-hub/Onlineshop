// file: backend/src/main/java/com/example/onlineshop/entity/Customer.java
package com.example.onlineshop.entity;

import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 客户（买家）信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer customerId;
    private String username;
    private String password;
    private String phone;
    private String defaultAddress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
