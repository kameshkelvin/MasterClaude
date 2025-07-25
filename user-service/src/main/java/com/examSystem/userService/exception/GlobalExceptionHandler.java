package com.examSystem.userService.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 
 * 基于系统设计文档中的错误处理架构
 * 统一处理应用中的各种异常，返回标准格式的错误响应
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        logger.warn("Validation error in request: {}", request.getDescription(false));

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );

        Map<String, Object> response = buildErrorResponse(
            "VALIDATION_ERROR",
            "请求参数验证失败",
            request.getDescription(false),
            errors
        );

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<?> handleBindException(BindException ex, WebRequest request) {
        logger.warn("Bind error in request: {}", request.getDescription(false));

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );

        Map<String, Object> response = buildErrorResponse(
            "BIND_ERROR",
            "请求数据绑定失败",
            request.getDescription(false),
            errors
        );

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理认证异常
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        logger.warn("Authentication error: {} in request: {}", ex.getMessage(), request.getDescription(false));

        String message = "认证失败";
        if (ex instanceof BadCredentialsException) {
            message = "用户名或密码错误";
        }

        Map<String, Object> response = buildErrorResponse(
            "AUTHENTICATION_ERROR",
            message,
            request.getDescription(false),
            null
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * 处理访问拒绝异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        logger.warn("Access denied: {} in request: {}", ex.getMessage(), request.getDescription(false));

        Map<String, Object> response = buildErrorResponse(
            "ACCESS_DENIED",
            "访问被拒绝，权限不足",
            request.getDescription(false),
            null
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        logger.warn("Illegal argument: {} in request: {}", ex.getMessage(), request.getDescription(false));

        Map<String, Object> response = buildErrorResponse(
            "INVALID_ARGUMENT",
            ex.getMessage(),
            request.getDescription(false),
            null
        );

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理非法状态异常
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleIllegalStateException(IllegalStateException ex, WebRequest request) {
        logger.warn("Illegal state: {} in request: {}", ex.getMessage(), request.getDescription(false));

        Map<String, Object> response = buildErrorResponse(
            "INVALID_STATE",
            ex.getMessage(),
            request.getDescription(false),
            null
        );

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex, WebRequest request) {
        logger.error("Runtime error in request: {}", request.getDescription(false), ex);

        Map<String, Object> response = buildErrorResponse(
            "RUNTIME_ERROR",
            "系统运行时错误：" + ex.getMessage(),
            request.getDescription(false),
            null
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<?> handleNullPointerException(NullPointerException ex, WebRequest request) {
        logger.error("Null pointer error in request: {}", request.getDescription(false), ex);

        Map<String, Object> response = buildErrorResponse(
            "NULL_POINTER_ERROR",
            "系统内部错误，请稍后重试",
            request.getDescription(false),
            null
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 处理数据库相关异常
     */
    @ExceptionHandler({
        org.springframework.dao.DataIntegrityViolationException.class,
        org.springframework.dao.DataAccessException.class
    })
    public ResponseEntity<?> handleDataAccessException(Exception ex, WebRequest request) {
        logger.error("Database error in request: {}", request.getDescription(false), ex);

        String message = "数据库操作失败";
        if (ex.getMessage().contains("constraint")) {
            message = "数据约束冲突，操作失败";
        } else if (ex.getMessage().contains("duplicate")) {
            message = "数据重复，操作失败";
        }

        Map<String, Object> response = buildErrorResponse(
            "DATABASE_ERROR",
            message,
            request.getDescription(false),
            null
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 处理JWT相关异常
     */
    @ExceptionHandler({
        io.jsonwebtoken.JwtException.class,
        io.jsonwebtoken.ExpiredJwtException.class,
        io.jsonwebtoken.MalformedJwtException.class,
        io.jsonwebtoken.SignatureException.class
    })
    public ResponseEntity<?> handleJwtException(Exception ex, WebRequest request) {
        logger.warn("JWT error: {} in request: {}", ex.getMessage(), request.getDescription(false));

        String message = "Token无效";
        if (ex instanceof io.jsonwebtoken.ExpiredJwtException) {
            message = "Token已过期";
        } else if (ex instanceof io.jsonwebtoken.MalformedJwtException) {
            message = "Token格式错误";
        } else if (ex instanceof io.jsonwebtoken.SignatureException) {
            message = "Token签名验证失败";
        }

        Map<String, Object> response = buildErrorResponse(
            "JWT_ERROR",
            message,
            request.getDescription(false),
            null
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception ex, WebRequest request) {
        logger.error("Unexpected error in request: {}", request.getDescription(false), ex);

        Map<String, Object> response = buildErrorResponse(
            "INTERNAL_ERROR",
            "系统内部错误，请稍后重试",
            request.getDescription(false),
            null
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 构建标准错误响应
     */
    private Map<String, Object> buildErrorResponse(String code, String message, String path, Object details) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("code", code);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now());
        response.put("path", path);
        
        if (details != null) {
            response.put("details", details);
        }
        
        // 添加帮助信息
        Map<String, String> help = new HashMap<>();
        help.put("documentation", "/swagger-ui.html");
        help.put("support", "contact@examSystem.com");
        response.put("help", help);
        
        return response;
    }
}