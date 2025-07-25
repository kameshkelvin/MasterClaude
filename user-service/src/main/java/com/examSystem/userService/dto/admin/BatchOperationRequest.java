package com.examSystem.userService.dto.admin;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 批量操作请求DTO
 */
public class BatchOperationRequest {
    
    @NotEmpty(message = "操作对象列表不能为空")
    private List<Long> ids;
    
    @NotNull(message = "操作类型不能为空")
    private String operation;
    
    private String value;
    private String reason;
    private Long operatorId;

    // 默认构造函数
    public BatchOperationRequest() {}

    // 构造函数
    public BatchOperationRequest(List<Long> ids, String operation) {
        this.ids = ids;
        this.operation = operation;
    }

    // Getters and Setters
    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }
}