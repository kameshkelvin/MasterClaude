package com.examSystem.userService.security;

import com.examSystem.userService.config.JwtConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器
 * 
 * 基于系统设计文档中的JWT认证流程
 * 处理请求中的JWT Token验证和用户认证
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // 从请求中提取JWT Token
            String jwt = getJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt)) {
                // 验证Token格式和基本有效性
                if (isValidTokenFormat(jwt)) {
                    // 从Token中获取用户名
                    String username = jwtConfig.getUsernameFromToken(jwt);
                    
                    if (StringUtils.hasText(username) && SecurityContextHolder.getContext().getAuthentication() == null) {
                        // 加载用户详情
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        
                        // 验证Token是否有效
                        if (jwtConfig.validateToken(jwt, userDetails.getUsername())) {
                            // 创建认证对象
                            UsernamePasswordAuthenticationToken authentication = 
                                new UsernamePasswordAuthenticationToken(
                                    userDetails, 
                                    null, 
                                    userDetails.getAuthorities()
                                );
                            
                            // 设置认证详情
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            
                            // 将认证信息设置到安全上下文
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            
                            logger.debug("JWT authentication successful for user: {}", username);
                        } else {
                            logger.debug("JWT validation failed for token");
                        }
                    }
                } else {
                    logger.debug("Invalid JWT token format");
                }
            }
        } catch (Exception ex) {
            logger.error("Cannot set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求中提取JWT Token
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }

    /**
     * 验证Token格式是否有效
     */
    private boolean isValidTokenFormat(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        
        // JWT Token应该包含三个部分，用点号分隔
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return false;
        }
        
        // 检查每个部分是否为空
        for (String part : parts) {
            if (!StringUtils.hasText(part)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * 检查请求是否应该跳过JWT认证
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        
        // 跳过认证的路径
        return path.startsWith("/api/v1/auth/register") ||
               path.startsWith("/api/v1/auth/login") ||
               path.startsWith("/api/v1/auth/refresh") ||
               path.startsWith("/api/v1/auth/forgot-password") ||
               path.startsWith("/api/v1/auth/reset-password") ||
               path.startsWith("/api/v1/auth/verify-email") ||
               path.startsWith("/actuator/health") ||
               path.startsWith("/actuator/info") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/swagger-ui") ||
               path.equals("/swagger-ui.html");
    }
}