package com.example.onlineshop.interceptor;

import com.example.onlineshop.annotation.RequireCustomerAuth;
import com.example.onlineshop.annotation.RequireSellerAuth;
import com.example.onlineshop.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;

@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();

        if (method.isAnnotationPresent(RequireCustomerAuth.class)) {
            return validateCustomerAuth(request, response);
        }

        if (method.isAnnotationPresent(RequireSellerAuth.class)) {
            return validateSellerAuth(request, response);
        }

        return true;
    }

    private boolean validateCustomerAuth(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String token = request.getHeader("Authorization");
        if (token == null || token.isBlank()) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未授权\",\"data\":null}");
            return false;
        }

        Integer customerId = JwtUtil.getCustomerIdFromToken(token);
        if (customerId == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token 无效或已过期\",\"data\":null}");
            return false;
        }

        request.setAttribute("customerId", customerId);
        return true;
    }

    private boolean validateSellerAuth(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String token = request.getHeader("Authorization");
        if (token == null || token.isBlank()) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未授权\",\"data\":null}");
            return false;
        }

        Integer sellerId = JwtUtil.getSellerIdFromToken(token);
        if (sellerId == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token 无效或已过期\",\"data\":null}");
            return false;
        }

        request.setAttribute("sellerId", sellerId);
        return true;
    }
}