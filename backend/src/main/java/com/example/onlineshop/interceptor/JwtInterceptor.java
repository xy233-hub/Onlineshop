// src/main/java/com/example/onlineshop/interceptor/JwtInterceptor.java
package com.example.onlineshop.interceptor;

import com.example.onlineshop.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 跳过地址相关的请求，不需要JWT令牌
        String path = request.getRequestURI();
        if (path.startsWith("/api/customers/addresses")) {
            return true;
        }
        
        String token = request.getHeader("Authorization");
        if (token == null || !JwtUtil.verifyToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("未登录或token无效");
            return false;
        }
        return true;
    }
}
