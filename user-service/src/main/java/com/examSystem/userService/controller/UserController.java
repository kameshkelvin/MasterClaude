package com.examSystem.userService.controller;

import com.examSystem.userService.dto.ChangePasswordRequest;
import com.examSystem.userService.dto.UpdateProfileRequest;
import com.examSystem.userService.entity.User;
import com.examSystem.userService.security.UserDetailsServiceImpl;
import com.examSystem.userService.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户管理控制器
 * 
 * 基于系统设计文档中的API接口设计
 * 提供用户资料管理、密码修改、用户查询等接口
 */
@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    /**
     * 获取当前用户信息
     * 
     * @return 当前用户信息
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCurrentUser() {
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(buildErrorResponse("用户未认证", null));
            }

            User user = userService.findById(currentUserId);
            if (user == null || user.getIsDeleted()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(buildErrorResponse("用户不存在", null));
            }

            Map<String, Object> userInfo = buildUserInfoResponse(user);
            
            logger.debug("Retrieved current user info for ID: {}", currentUserId);
            return ResponseEntity.ok(buildSuccessResponse("获取用户信息成功", userInfo));

        } catch (Exception e) {
            logger.error("Error retrieving current user info", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse("获取用户信息失败", null));
        }
    }

    /**
     * 更新用户资料
     * 
     * @param updateRequest 更新请求
     * @param bindingResult 验证结果
     * @return 更新结果
     */
    @PutMapping("/me/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateProfileRequest updateRequest,
                                         BindingResult bindingResult) {
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(buildErrorResponse("用户未认证", null));
            }

            // 验证请求参数
            if (bindingResult.hasErrors()) {
                Map<String, Object> errors = buildValidationErrors(bindingResult);
                return ResponseEntity.badRequest().body(buildErrorResponse("参数验证失败", errors));
            }

            // 构建更新用户对象
            User userUpdate = new User();
            userUpdate.setFirstName(updateRequest.getFirstName());
            userUpdate.setLastName(updateRequest.getLastName());
            userUpdate.setPhone(updateRequest.getPhone());
            
            // 构建profile JSON
            if (hasProfileData(updateRequest)) {
                String profileJson = buildProfileJson(updateRequest);
                userUpdate.setProfile(profileJson);
            }

            // 执行更新
            User updatedUser = userService.updateUser(currentUserId, userUpdate);
            Map<String, Object> userInfo = buildUserInfoResponse(updatedUser);

            logger.info("User profile updated successfully for ID: {}", currentUserId);
            return ResponseEntity.ok(buildSuccessResponse("用户资料更新成功", userInfo));

        } catch (IllegalArgumentException e) {
            logger.warn("Profile update failed due to validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(buildErrorResponse(e.getMessage(), null));
            
        } catch (Exception e) {
            logger.error("Profile update failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse("更新用户资料失败，请稍后重试", null));
        }
    }

    /**
     * 修改密码
     * 
     * @param changePasswordRequest 修改密码请求
     * @param bindingResult 验证结果
     * @return 修改结果
     */
    @PutMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest changePasswordRequest,
                                          BindingResult bindingResult) {
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(buildErrorResponse("用户未认证", null));
            }

            // 验证请求参数
            if (bindingResult.hasErrors()) {
                Map<String, Object> errors = buildValidationErrors(bindingResult);
                return ResponseEntity.badRequest().body(buildErrorResponse("参数验证失败", errors));
            }

            // 验证新密码确认
            if (!changePasswordRequest.isNewPasswordMatched()) {
                return ResponseEntity.badRequest()
                    .body(buildErrorResponse("新密码与确认密码不匹配", null));
            }

            // 执行密码修改
            userService.updatePassword(
                currentUserId,
                changePasswordRequest.getCurrentPassword(),
                changePasswordRequest.getNewPassword()
            );

            logger.info("Password changed successfully for user ID: {}", currentUserId);
            return ResponseEntity.ok(buildSuccessResponse("密码修改成功", null));

        } catch (IllegalArgumentException e) {
            logger.warn("Password change failed due to validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(buildErrorResponse(e.getMessage(), null));
            
        } catch (Exception e) {
            logger.error("Password change failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse("密码修改失败，请稍后重试", null));
        }
    }

    /**
     * 获取用户统计信息
     * 
     * @return 用户统计信息
     */
    @GetMapping("/me/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserStats() {
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(buildErrorResponse("用户未认证", null));
            }

            Object stats = userService.getUserStats(currentUserId);
            
            logger.debug("Retrieved user stats for ID: {}", currentUserId);
            return ResponseEntity.ok(buildSuccessResponse("获取用户统计信息成功", stats));

        } catch (Exception e) {
            logger.error("Error retrieving user stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse("获取用户统计信息失败", null));
        }
    }

    /**
     * 根据ID获取用户信息（管理员权限）
     * 
     * @param userId 用户ID
     * @return 用户信息
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserById(@PathVariable Long userId) {
        try {
            User user = userService.findById(userId);
            if (user == null || user.getIsDeleted()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(buildErrorResponse("用户不存在", null));
            }

            Map<String, Object> userInfo = buildUserInfoResponse(user);
            
            logger.info("Admin retrieved user info for ID: {}", userId);
            return ResponseEntity.ok(buildSuccessResponse("获取用户信息成功", userInfo));

        } catch (Exception e) {
            logger.error("Error retrieving user info for ID: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse("获取用户信息失败", null));
        }
    }

    /**
     * 分页查询用户列表（管理员权限）
     * 
     * @param keyword 搜索关键字
     * @param page 页码
     * @param size 页大小
     * @param sort 排序字段
     * @param direction 排序方向
     * @return 用户列表
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUsers(@RequestParam(required = false) String keyword,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "10") int size,
                                     @RequestParam(defaultValue = "createdAt") String sort,
                                     @RequestParam(defaultValue = "desc") String direction) {
        try {
            // 构建分页和排序参数
            Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? 
                Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

            // 查询用户
            Page<User> userPage = userService.findUsers(keyword, pageable);

            // 构建响应数据
            Map<String, Object> response = new HashMap<>();
            response.put("users", userPage.getContent().stream()
                .map(this::buildUserInfoResponse)
                .toList());
            response.put("totalElements", userPage.getTotalElements());
            response.put("totalPages", userPage.getTotalPages());
            response.put("currentPage", userPage.getNumber());
            response.put("pageSize", userPage.getSize());
            response.put("hasNext", userPage.hasNext());
            response.put("hasPrevious", userPage.hasPrevious());

            logger.info("Admin retrieved user list: page={}, size={}, total={}", 
                page, size, userPage.getTotalElements());
            
            return ResponseEntity.ok(buildSuccessResponse("获取用户列表成功", response));

        } catch (Exception e) {
            logger.error("Error retrieving user list", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse("获取用户列表失败", null));
        }
    }

    /**
     * 激活/禁用用户（管理员权限）
     * 
     * @param userId 用户ID
     * @param active 是否激活
     * @return 操作结果
     */
    @PutMapping("/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUserStatus(@PathVariable Long userId,
                                            @RequestParam boolean active) {
        try {
            if (active) {
                userService.activateUser(userId);
                logger.info("Admin activated user ID: {}", userId);
            } else {
                userService.deactivateUser(userId);
                logger.info("Admin deactivated user ID: {}", userId);
            }

            String message = active ? "用户激活成功" : "用户禁用成功";
            return ResponseEntity.ok(buildSuccessResponse(message, null));

        } catch (Exception e) {
            logger.error("Error updating user status for ID: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse("更新用户状态失败", null));
        }
    }

    /**
     * 删除用户（管理员权限）
     * 
     * @param userId 用户ID
     * @return 删除结果
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        try {
            userService.deleteUser(userId);
            
            logger.info("Admin deleted user ID: {}", userId);
            return ResponseEntity.ok(buildSuccessResponse("用户删除成功", null));

        } catch (Exception e) {
            logger.error("Error deleting user ID: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse("删除用户失败", null));
        }
    }

    // 辅助方法

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsServiceImpl.CustomUserPrincipal) {
            UserDetailsServiceImpl.CustomUserPrincipal userPrincipal = 
                (UserDetailsServiceImpl.CustomUserPrincipal) authentication.getPrincipal();
            return userPrincipal.getUserId();
        }
        return null;
    }

    /**
     * 构建用户信息响应
     */
    private Map<String, Object> buildUserInfoResponse(User user) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("firstName", user.getFirstName());
        userInfo.put("lastName", user.getLastName());
        userInfo.put("phone", user.getPhone());
        userInfo.put("profile", user.getProfile());
        userInfo.put("isActive", user.getIsActive());
        userInfo.put("isLocked", user.getIsLocked());
        userInfo.put("emailVerified", user.getEmailVerified());
        userInfo.put("createdAt", user.getCreatedAt());
        userInfo.put("lastLoginAt", user.getLastLoginAt());
        userInfo.put("roles", user.getRoles().stream()
            .filter(role -> role.isValid())
            .map(role -> role.getRole().name())
            .toList());
        return userInfo;
    }

    /**
     * 检查是否有profile数据
     */
    private boolean hasProfileData(UpdateProfileRequest request) {
        return request.getBio() != null || request.getPosition() != null ||
               request.getOrganization() != null || request.getLocation() != null ||
               request.getWebsite() != null;
    }

    /**
     * 构建profile JSON字符串
     */
    private String buildProfileJson(UpdateProfileRequest request) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;

        if (request.getBio() != null) {
            json.append("\"bio\":\"").append(request.getBio()).append("\"");
            first = false;
        }
        
        if (request.getPosition() != null) {
            if (!first) json.append(",");
            json.append("\"position\":\"").append(request.getPosition()).append("\"");
            first = false;
        }
        
        if (request.getOrganization() != null) {
            if (!first) json.append(",");
            json.append("\"organization\":\"").append(request.getOrganization()).append("\"");
            first = false;
        }
        
        if (request.getLocation() != null) {
            if (!first) json.append(",");
            json.append("\"location\":\"").append(request.getLocation()).append("\"");
            first = false;
        }
        
        if (request.getWebsite() != null) {
            if (!first) json.append(",");
            json.append("\"website\":\"").append(request.getWebsite()).append("\"");
        }

        json.append("}");
        return json.toString();
    }

    /**
     * 构建验证错误信息
     */
    private Map<String, Object> buildValidationErrors(BindingResult bindingResult) {
        Map<String, Object> errors = new HashMap<>();
        bindingResult.getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        return errors;
    }

    /**
     * 构建成功响应
     */
    private Map<String, Object> buildSuccessResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now());
        
        if (data != null) {
            response.put("data", data);
        }
        
        return response;
    }

    /**
     * 构建错误响应
     */
    private Map<String, Object> buildErrorResponse(String message, Map<String, Object> details) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now());
        
        if (details != null) {
            response.put("errors", details);
        }
        
        return response;
    }
}