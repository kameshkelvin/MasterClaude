package com.examSystem.userService.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 修改密码请求DTO
 * 
 * 基于系统设计文档中的API接口设计
 * 用于接收和验证用户修改密码请求参数
 */
public class ChangePasswordRequest {

    @NotBlank(message = "当前密码不能为空")
    private String currentPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 128, message = "新密码长度必须在8-128字符之间")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", 
             message = "新密码必须包含至少一个小写字母、一个大写字母和一个数字")
    private String newPassword;

    @NotBlank(message = "确认新密码不能为空")
    private String confirmNewPassword;

    // 默认构造函数
    public ChangePasswordRequest() {}

    // 构造函数
    public ChangePasswordRequest(String currentPassword, String newPassword, String confirmNewPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
        this.confirmNewPassword = confirmNewPassword;
    }

    // Getters and Setters
    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmNewPassword() {
        return confirmNewPassword;
    }

    public void setConfirmNewPassword(String confirmNewPassword) {
        this.confirmNewPassword = confirmNewPassword;
    }

    // 业务方法
    public boolean isNewPasswordMatched() {
        return newPassword != null && newPassword.equals(confirmNewPassword);
    }

    @Override
    public String toString() {
        return "ChangePasswordRequest{" +
                "currentPassword='***'" +
                ", newPassword='***'" +
                ", confirmNewPassword='***'" +
                '}';
    }
}