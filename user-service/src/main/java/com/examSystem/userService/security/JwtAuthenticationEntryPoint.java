package com.examSystem.userService.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT认证入口点
 * 
 * 基于系统设计文档中的错误处理架构
 * 处理未认证请求的统一响应格式
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, 
                        HttpServletResponse response, 
                        AuthenticationException authException) throws IOException, ServletException {
        
        logger.warn("Unauthorized access attempt: {} - {}", 
            request.getRequestURI(), authException.getMessage());

        // 设置响应状态和内容类型
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // 构建错误响应体
        Map<String, Object> errorResponse = buildErrorResponse(request, authException);

        // 写入响应
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    /**
     * 构建统一的错误响应格式
     */
    private Map<String, Object> buildErrorResponse(HttpServletRequest request, 
                                                  AuthenticationException authException) {
        Map<String, Object> response = new HashMap<>();
        
        // 基本错误信息
        response.put("success", false);
        response.put("code", "UNAUTHORIZED");
        response.put("message", "认证失败，请先登录");
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        // 请求信息
        Map<String, Object> request_info = new HashMap<>();
        request_info.put("path", request.getRequestURI());
        request_info.put("method", request.getMethod());
        request_info.put("remote_addr", getClientIpAddress(request));
        response.put("request", request_info);
        
        // 详细错误信息（开发环境可以包含更多信息）
        Map<String, Object> error_details = new HashMap<>();
        error_details.put("type", "AuthenticationException");
        error_details.put("reason", determineErrorReason(request, authException));
        
        // 生产环境隐藏敏感错误信息
        String profile = System.getProperty("spring.profiles.active", "dev");
        if ("dev".equals(profile) || "test".equals(profile)) {
            error_details.put("exception_message", authException.getMessage());
        }
        
        response.put("error", error_details);
        
        // 帮助信息
        Map<String, Object> help = new HashMap<>();
        help.put("login_endpoint", "/api/v1/auth/login");
        help.put("register_endpoint", "/api/v1/auth/register");
        help.put("documentation", "/swagger-ui.html");
        response.put("help", help);
        
        return response;
    }

    /**
     * 确定错误原因
     */
    private String determineErrorReason(HttpServletRequest request, AuthenticationException authException) {
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || authHeader.isEmpty()) {
            return "missing_authorization_header";
        }
        
        if (!authHeader.startsWith("Bearer ")) {
            return "invalid_authorization_format";
        }
        
        String token = authHeader.substring(7);
        if (token.isEmpty()) {
            return "empty_token";
        }
        
        // JWT格式检查
        String[] tokenParts = token.split("\\.");
        if (tokenParts.length != 3) {
            return "invalid_token_format";
        }
        
        // 其他认证异常
        if (authException.getMessage().contains("expired")) {
            return "token_expired";
        }
        
        if (authException.getMessage().contains("invalid")) {
            return "token_invalid";
        }
        
        return "authentication_failed";
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        String xForwardedProto = request.getHeader("X-Forwarded-Proto");
        if (xForwardedProto != null) {
            String remoteAddr = request.getRemoteAddr();
            return remoteAddr != null ? remoteAddr : "unknown";
        }
        
        return request.getRemoteAddr();
    }
}