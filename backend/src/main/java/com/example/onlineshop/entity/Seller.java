// file: backend/src/main/java/com/example/onlineshop/entity/Seller.java
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

    // 手动添加getter和setter方法
    public Integer getSellerId() {
        return sellerId;
    }

    public void setSellerId(Integer sellerId) {
        this.sellerId = sellerId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
