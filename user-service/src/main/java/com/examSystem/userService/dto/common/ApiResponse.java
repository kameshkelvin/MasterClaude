package com.examSystem.userService.dto.common;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 统一API响应格式
 */
public class ApiResponse<T> {
    
    private boolean success;
    private String message;
    private T data;
    private Map<String, Object> pagination;
    private String errorCode;
    private LocalDateTime timestamp;

    // 默认构造函数
    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }

    // 构造函数
    public ApiResponse(boolean success, String message) {
        this();
        this.success = success;
        this.message = message;
    }

    public ApiResponse(boolean success, String message, T data) {
        this(success, message);
        this.data = data;
    }

    // 静态工厂方法
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(true, "操作成功");
    }

    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "操作成功", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message);
    }

    public static <T> ApiResponse<T> error(String message, String errorCode) {
        ApiResponse<T> response = new ApiResponse<>(false, message);
        response.setErrorCode(errorCode);
        return response;
    }

    // 分页响应
    public ApiResponse<T> withPagination(Map<String, Object> pagination) {
        this.pagination = pagination;
        return this;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Map<String, Object> getPagination() {
        return pagination;
    }

    public void setPagination(Map<String, Object> pagination) {
        this.pagination = pagination;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}