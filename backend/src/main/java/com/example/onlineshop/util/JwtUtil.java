package com.example.onlineshop.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtUtil {

    // 密钥（实际项目中配置在application.properties）
    private static final String SECRET = "your-256-bit-secret-key-for-jwt-encryption-123456";
    // Token有效期：24小时（毫秒）
    private static final long EXPIRATION = 86400000;

    // 生成签名密钥
    private static SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成Token（包含卖家ID和用户名）
     */
    public static String generateToken(Integer sellerId, String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION);

        return Jwts.builder()
                .setSubject(username) // 主题（用户名）
                .claim("sellerId", sellerId) // 自定义载荷（卖家ID）
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSecretKey(), SignatureAlgorithm.HS256) // 签名算法
                .compact();
    }

    /**
     * 生成客户Token（包含客户ID和用户名）
     */
    public static String generateCustomerToken(Integer customerId, String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION);

        return Jwts.builder()
                .setSubject(username) // 主题（用户名）
                .claim("customerId", customerId) // 自定义载荷（客户ID）
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSecretKey(), SignatureAlgorithm.HS256) // 签名算法
                .compact();
    }

    /**
     * 从Token中解析卖家ID
     */
    public static Integer getSellerIdFromToken(String token) {
        try {
            // 去除Bearer前缀
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("sellerId", Integer.class);
        } catch (Exception e) {
            return null;  // Token无效时返回null
        }
    }

    /**
     * 从Token中解析客户ID
     */
    public static Integer getCustomerIdFromToken(String token) {
        try {
            // 去除Bearer前缀
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("customerId", Integer.class);
        } catch (Exception e) {
            return null;  // Token无效时返回null
        }
    }

    /**
     * 验证Token有效性
     */
    public static boolean verifyToken(String token) {
        try {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
