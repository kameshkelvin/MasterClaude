package com.examSystem.userService.service.impl;

import com.examSystem.userService.entity.User;
import com.examSystem.userService.repository.UserRepository;
import com.examSystem.userService.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 用户服务实现类
 * 
 * 基于系统设计文档中的用户管理架构
 * 实现用户注册、登录、信息管理等核心业务逻辑
 */
@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public User register(String username, String email, String password, String phone) {
        logger.info("Starting user registration for username: {}, email: {}", username, email);

        // 验证输入参数
        validateRegistrationInput(username, email, password);

        // 检查用户名和邮箱是否已存在
        if (userRepository.existsByUsernameAndIsDeletedFalse(username)) {
            throw new IllegalArgumentException("用户名已存在: " + username);
        }

        if (userRepository.existsByEmailAndIsDeletedFalse(email)) {
            throw new IllegalArgumentException("邮箱已存在: " + email);
        }

        if (StringUtils.hasText(phone) && userRepository.existsByPhoneAndIsDeletedFalse(phone)) {
            throw new IllegalArgumentException("手机号已存在: " + phone);
        }

        try {
            // 创建新用户
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(password));
            
            if (StringUtils.hasText(phone)) {
                user.setPhone(phone);
            }

            // 设置默认值
            user.setIsActive(true);
            user.setEmailVerified(false);
            user.setIsLocked(false);
            user.setIsDeleted(false);
            user.setEmailVerificationToken(UUID.randomUUID().toString());

            // 保存用户
            User savedUser = userRepository.save(user);
            
            logger.info("User registered successfully with ID: {}", savedUser.getId());
            
            // 发送邮箱验证（异步处理）
            // emailService.sendVerificationEmail(savedUser.getEmail(), savedUser.getEmailVerificationToken());
            
            return savedUser;

        } catch (Exception e) {
            logger.error("Error during user registration for username: {}", username, e);
            throw new RuntimeException("用户注册失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public User authenticate(String username, String password) {
        logger.debug("Authenticating user: {}", username);

        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new IllegalArgumentException("用户名和密码不能为空");
        }

        try {
            // 查找用户
            User user = findByUsername(username);
            if (user == null) {
                logger.warn("Authentication failed - user not found: {}", username);
                throw new IllegalArgumentException("用户名或密码错误");
            }

            // 检查账户状态
            if (!user.getIsActive()) {
                logger.warn("Authentication failed - account disabled: {}", username);
                throw new IllegalArgumentException("账户已被禁用");
            }

            if (user.getIsLocked()) {
                logger.warn("Authentication failed - account locked: {}", username);
                throw new IllegalArgumentException("账户已被锁定");
            }

            // 验证密码
            if (!passwordEncoder.matches(password, user.getPasswordHash())) {
                logger.warn("Authentication failed - wrong password for user: {}", username);
                throw new IllegalArgumentException("用户名或密码错误");
            }

            logger.info("User authenticated successfully: {}", username);
            return user;

        } catch (Exception e) {
            logger.error("Error during authentication for user: {}", username, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }

        return userRepository.findByUsernameAndIsDeletedFalse(username).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }

        return userRepository.findByEmailAndIsDeletedFalse(email).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(Long userId) {
        if (userId == null) {
            return null;
        }

        return userRepository.findById(userId).orElse(null);
    }

    @Override
    public User updateUser(Long userId, User userUpdate) {
        logger.info("Updating user information for ID: {}", userId);

        User existingUser = findById(userId);
        if (existingUser == null || existingUser.getIsDeleted()) {
            throw new IllegalArgumentException("用户不存在");
        }

        try {
            // 更新允许修改的字段
            if (StringUtils.hasText(userUpdate.getFirstName())) {
                existingUser.setFirstName(userUpdate.getFirstName());
            }
            
            if (StringUtils.hasText(userUpdate.getLastName())) {
                existingUser.setLastName(userUpdate.getLastName());
            }
            
            if (StringUtils.hasText(userUpdate.getPhone())) {
                // 检查手机号是否已被其他用户使用
                if (userRepository.existsByPhoneAndIsDeletedFalse(userUpdate.getPhone())) {
                    User phoneUser = userRepository.findByPhoneAndIsDeletedFalse(userUpdate.getPhone()).orElse(null);
                    if (phoneUser != null && !phoneUser.getId().equals(userId)) {
                        throw new IllegalArgumentException("手机号已被其他用户使用");
                    }
                }
                existingUser.setPhone(userUpdate.getPhone());
            }

            if (userUpdate.getProfile() != null) {
                existingUser.setProfile(userUpdate.getProfile());
            }

            User savedUser = userRepository.save(existingUser);
            logger.info("User information updated successfully for ID: {}", userId);
            
            return savedUser;

        } catch (Exception e) {
            logger.error("Error updating user information for ID: {}", userId, e);
            throw new RuntimeException("更新用户信息失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        logger.info("Updating password for user ID: {}", userId);

        if (!StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
            throw new IllegalArgumentException("旧密码和新密码不能为空");
        }

        if (newPassword.length() < 8) {
            throw new IllegalArgumentException("新密码长度不能少于8位");
        }

        User user = findById(userId);
        if (user == null || user.getIsDeleted()) {
            throw new IllegalArgumentException("用户不存在");
        }

        try {
            // 验证旧密码
            if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
                throw new IllegalArgumentException("原密码错误");
            }

            // 更新密码
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            logger.info("Password updated successfully for user ID: {}", userId);

        } catch (Exception e) {
            logger.error("Error updating password for user ID: {}", userId, e);
            throw new RuntimeException("更新密码失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void resetPassword(String email) {
        logger.info("Initiating password reset for email: {}", email);

        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("邮箱不能为空");
        }

        User user = findByEmail(email);
        if (user == null) {
            // 出于安全考虑，不透露邮箱是否存在
            logger.warn("Password reset requested for non-existent email: {}", email);
            return;
        }

        try {
            // 生成密码重置Token
            String resetToken = UUID.randomUUID().toString();
            user.setPasswordResetToken(resetToken);
            user.setPasswordResetExpires(LocalDateTime.now().plusHours(1)); // 1小时有效期

            userRepository.save(user);

            // 发送重置密码邮件（异步处理）
            // emailService.sendPasswordResetEmail(user.getEmail(), resetToken);

            logger.info("Password reset token generated for email: {}", email);

        } catch (Exception e) {
            logger.error("Error generating password reset token for email: {}", email, e);
            throw new RuntimeException("密码重置失败", e);
        }
    }

    @Override
    public void activateUser(Long userId) {
        logger.info("Activating user ID: {}", userId);
        userRepository.updateActiveStatus(userId, true);
    }

    @Override
    public void deactivateUser(Long userId) {
        logger.info("Deactivating user ID: {}", userId);
        userRepository.updateActiveStatus(userId, false);
    }

    @Override
    public void lockUser(Long userId, String reason) {
        logger.info("Locking user ID: {} with reason: {}", userId, reason);
        userRepository.updateLockStatus(userId, true);
    }

    @Override
    public void unlockUser(Long userId) {
        logger.info("Unlocking user ID: {}", userId);
        userRepository.updateLockStatus(userId, false);
    }

    @Override
    public void deleteUser(Long userId) {
        logger.info("Soft deleting user ID: {}", userId);
        userRepository.softDeleteUser(userId, LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return StringUtils.hasText(username) && userRepository.existsByUsernameAndIsDeletedFalse(username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return StringUtils.hasText(email) && userRepository.existsByEmailAndIsDeletedFalse(email);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> findUsers(String keyword, Pageable pageable) {
        if (StringUtils.hasText(keyword)) {
            return userRepository.findByKeyword(keyword, pageable);
        } else {
            return userRepository.findActiveUsers(pageable);
        }
    }

    @Override
    public void verifyEmail(String token) {
        logger.info("Verifying email with token: {}", token);

        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("验证Token不能为空");
        }

        User user = userRepository.findByEmailVerificationTokenAndIsDeletedFalse(token).orElse(null);
        if (user == null) {
            throw new IllegalArgumentException("无效的验证Token");
        }

        try {
            userRepository.clearEmailVerificationToken(user.getId());
            logger.info("Email verified successfully for user ID: {}", user.getId());

        } catch (Exception e) {
            logger.error("Error verifying email for token: {}", token, e);
            throw new RuntimeException("邮箱验证失败", e);
        }
    }

    @Override
    public void sendEmailVerification(String email) {
        logger.info("Sending email verification to: {}", email);

        User user = findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        if (user.getEmailVerified()) {
            throw new IllegalArgumentException("邮箱已验证");
        }

        try {
            // 重新生成验证Token
            String verificationToken = UUID.randomUUID().toString();
            user.setEmailVerificationToken(verificationToken);
            userRepository.save(user);

            // 发送验证邮件（异步处理）
            // emailService.sendVerificationEmail(user.getEmail(), verificationToken);

            logger.info("Email verification sent to: {}", email);

        } catch (Exception e) {
            logger.error("Error sending email verification to: {}", email, e);
            throw new RuntimeException("发送验证邮件失败", e);
        }
    }

    @Override
    public void updateLastLoginTime(Long userId) {
        if (userId != null) {
            userRepository.updateLastLoginTime(userId, LocalDateTime.now());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Object getUserStats(Long userId) {
        User user = findById(userId);
        if (user == null || user.getIsDeleted()) {
            throw new IllegalArgumentException("用户不存在");
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("userId", user.getId());
        stats.put("username", user.getUsername());
        stats.put("email", user.getEmail());
        stats.put("isActive", user.getIsActive());
        stats.put("isLocked", user.getIsLocked());
        stats.put("emailVerified", user.getEmailVerified());
        stats.put("createdAt", user.getCreatedAt());
        stats.put("lastLoginAt", user.getLastLoginAt());
        stats.put("roleCount", user.getRoles().size());

        return stats;
    }

    /**
     * 验证注册输入参数
     */
    private void validateRegistrationInput(String username, String email, String password) {
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        if (username.length() < 3 || username.length() > 50) {
            throw new IllegalArgumentException("用户名长度必须在3-50字符之间");
        }

        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("邮箱不能为空");
        }

        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }

        if (!StringUtils.hasText(password)) {
            throw new IllegalArgumentException("密码不能为空");
        }

        if (password.length() < 8) {
            throw new IllegalArgumentException("密码长度不能少于8位");
        }
    }

    /**
     * 简单的邮箱格式验证
     */
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
    }
}