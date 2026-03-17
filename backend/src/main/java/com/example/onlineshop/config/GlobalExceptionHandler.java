package com.example.onlineshop.config;

import com.example.onlineshop.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数错误：{}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(new ApiResponse(400, e.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidation(MethodArgumentNotValidException e) {
        StringBuilder msg = new StringBuilder("参数验证失败：");
        e.getBindingResult().getFieldErrors().forEach(error -> 
            msg.append(error.getField()).append(": ").append(error.getDefaultMessage()).append("; ")
        );
        log.warn(msg.toString());
        return ResponseEntity.badRequest()
                .body(new ApiResponse(400, msg.toString(), null));
    }

    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public ResponseEntity<ApiResponse> handleSQLIntegrity(SQLIntegrityConstraintViolationException e) {
        log.error("数据库约束违反：{}", e.getMessage());
        String message = e.getMessage();
        if (message.contains("Duplicate entry")) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(400, "数据已存在，请勿重复提交", null));
        } else if (message.contains("foreign key constraint")) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(400, "引用的数据不存在", null));
        }
        return ResponseEntity.badRequest()
                .body(new ApiResponse(400, "数据库操作失败：" + e.getMessage(), null));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse> handleSecurity(SecurityException e) {
        log.warn("安全异常：{}", e.getMessage());
        return ResponseEntity.status(401)
                .body(new ApiResponse(401, e.getMessage(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleGeneral(Exception e) {
        log.error("系统异常：", e);
        return ResponseEntity.status(500)
                .body(new ApiResponse(500, "系统繁忙，请稍后再试：" + e.getMessage(), null));
    }
}