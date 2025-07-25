package com.examSystem.userService.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户登录请求DTO
 * 
 * 基于系统设计文档中的API接口设计
 * 用于接收和验证用户登录请求参数
 */
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 100, message = "用户名长度必须在3-100字符之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 1, max = 128, message = "密码长度不能超过128字符")
    private String password;

    private boolean rememberMe = false;

    private String deviceType = "web";

    private String deviceFingerprint;

    // 默认构造函数
    public LoginRequest() {}

    // 构造函数
    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public LoginRequest(String username, String password, boolean rememberMe) {
        this.username = username;
        this.password = password;
        this.rememberMe = rememberMe;
    }

    // Getters and Setters
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

    public boolean isRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(boolean rememberMe) {
        this.rememberMe = rememberMe;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getDeviceFingerprint() {
        return deviceFingerprint;
    }

    public void setDeviceFingerprint(String deviceFingerprint) {
        this.deviceFingerprint = deviceFingerprint;
    }

    @Override
    public String toString() {
        return "LoginRequest{" +
                "username='" + username + '\'' +
                ", rememberMe=" + rememberMe +
                ", deviceType='" + deviceType + '\'' +
                ", deviceFingerprint='" + deviceFingerprint + '\'' +
                '}';
    }
}