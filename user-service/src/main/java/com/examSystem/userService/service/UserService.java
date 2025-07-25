package com.examSystem.userService.service;

import com.examSystem.userService.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 用户服务接口
 * 
 * 基于系统设计文档中的用户管理架构
 * 定义用户相关的业务操作接口
 */
public interface UserService {

    /**
     * 用户注册
     */
    User register(String username, String email, String password, String phone);

    /**
     * 用户登录验证
     */
    User authenticate(String username, String password);

    /**
     * 根据用户名查找用户
     */
    User findByUsername(String username);

    /**
     * 根据邮箱查找用户
     */
    User findByEmail(String email);

    /**
     * 根据用户ID查找用户
     */
    User findById(Long userId);

    /**
     * 更新用户信息
     */
    User updateUser(Long userId, User userUpdate);

    /**
     * 更新用户密码
     */
    void updatePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 重置密码
     */
    void resetPassword(String email);

    /**
     * 激活用户账户
     */
    void activateUser(Long userId);

    /**
     * 禁用用户账户
     */
    void deactivateUser(Long userId);

    /**
     * 锁定用户账户
     */
    void lockUser(Long userId, String reason);

    /**
     * 解锁用户账户
     */
    void unlockUser(Long userId);

    /**
     * 删除用户（软删除）
     */
    void deleteUser(Long userId);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 分页查询用户
     */
    Page<User> findUsers(String keyword, Pageable pageable);

    /**
     * 验证邮箱
     */
    void verifyEmail(String token);

    /**
     * 发送邮箱验证码
     */
    void sendEmailVerification(String email);

    /**
     * 更新用户最后登录时间
     */
    void updateLastLoginTime(Long userId);

    /**
     * 获取用户统计信息
     */
    Object getUserStats(Long userId);
}