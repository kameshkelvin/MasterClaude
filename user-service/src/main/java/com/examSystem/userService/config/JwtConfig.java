package com.examSystem.userService.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT配置类
 * 
 * 基于系统设计文档中的JWT认证架构
 * 提供Token生成、解析、验证等核心功能
 */
@Component
public class JwtConfig {

    @Value("${jwt.secret:exam-system-jwt-secret-key-2024}")
    private String jwtSecret;

    @Value("${jwt.expiration:3600000}") // 1小时，单位毫秒
    private Long jwtExpiration;

    @Value("${jwt.refresh-expiration:604800000}") // 7天，单位毫秒
    private Long refreshTokenExpiration;

    @Value("${jwt.issuer:exam-system}")
    private String issuer;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * 从Token中提取用户名
     */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * 从Token中提取过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /**
     * 从Token中提取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return Long.valueOf(claims.get("userId").toString());
    }

    /**
     * 从Token中提取角色信息
     */
    @SuppressWarnings("unchecked")
    public java.util.List<String> getRolesFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return (java.util.List<String>) claims.get("roles");
    }

    /**
     * 从Token中提取组织ID
     */
    public Long getOrganizationIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        Object orgId = claims.get("organizationId");
        return orgId != null ? Long.valueOf(orgId.toString()) : null;
    }

    /**
     * 从Token中提取指定声明
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 从Token中提取所有声明
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 检查Token是否过期
     */
    public Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    /**
     * 生成访问Token
     */
    public String generateAccessToken(Long userId, String username, java.util.List<String> roles, Long organizationId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("roles", roles);
        claims.put("organizationId", organizationId);
        claims.put("type", "access");
        
        return createToken(claims, username, jwtExpiration);
    }

    /**
     * 生成刷新Token
     */
    public String generateRefreshToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "refresh");
        
        return createToken(claims, username, refreshTokenExpiration);
    }

    /**
     * 创建Token
     */
    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuer(issuer)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 验证Token
     */
    public Boolean validateToken(String token, String username) {
        try {
            final String tokenUsername = getUsernameFromToken(token);
            return (username.equals(tokenUsername) && !isTokenExpired(token));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 验证刷新Token
     */
    public Boolean validateRefreshToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            String tokenType = (String) claims.get("type");
            return "refresh".equals(tokenType) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从Token中提取Token类型
     */
    public String getTokenType(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return (String) claims.get("type");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取Token剩余有效时间（毫秒）
     */
    public Long getTokenRemainingTime(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            Date now = new Date();
            return expiration.getTime() - now.getTime();
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * 检查Token是否即将过期（15分钟内）
     */
    public Boolean isTokenNearExpiry(String token) {
        Long remainingTime = getTokenRemainingTime(token);
        return remainingTime != null && remainingTime < 900000; // 15分钟
    }

    // Getters for configuration values
    public Long getJwtExpiration() {
        return jwtExpiration;
    }

    public Long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    public String getIssuer() {
        return issuer;
    }
}