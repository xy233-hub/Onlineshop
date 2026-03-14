package com.example.onlineshop.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TokenResponse {
    @JsonProperty("token")
    private String token;

    @JsonProperty("seller_info")
    private SellerInfo sellerInfo;

    public TokenResponse(String token, Integer sellerId, String username, String createTime) {
        this.token = token;
        this.sellerInfo = new SellerInfo(sellerId, username, createTime);
    }

    @Data
    public static class SellerInfo {
        @JsonProperty("seller_id")
        private Integer sellerId;

        @JsonProperty("username")
        private String username;

        @JsonProperty("create_time")
        private String createTime;

        public SellerInfo(Integer sellerId, String username, String createTime) {
            this.sellerId = sellerId;
            this.username = username;
            this.createTime = createTime;
        }
    }
}
