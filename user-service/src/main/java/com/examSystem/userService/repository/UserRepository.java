package com.examSystem.userService.repository;

import com.examSystem.userService.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 用户数据访问接口
 * 
 * 基于数据库设计文档中的users表结构
 * 提供用户相关的数据库操作方法
 */
@Repository 
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsernameAndIsDeletedFalse(String username);

    /**
     * 根据邮箱查找用户
     */
    Optional<User> findByEmailAndIsDeletedFalse(String email);

    /**
     * 根据手机号查找用户
     */
    Optional<User> findByPhoneAndIsDeletedFalse(String phone);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsernameAndIsDeletedFalse(String username);

    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmailAndIsDeletedFalse(String email);

    /**
     * 检查手机号是否存在
     */
    boolean existsByPhoneAndIsDeletedFalse(String phone);

    /**
     * 根据邮箱验证Token查找用户
     */
    Optional<User> findByEmailVerificationTokenAndIsDeletedFalse(String token);

    /**
     * 根据密码重置Token查找用户
     */
    Optional<User> findByPasswordResetTokenAndIsDeletedFalse(String token);

    /**
     * 分页查询用户（支持关键字搜索）
     */
    @Query("SELECT u FROM User u WHERE u.isDeleted = false AND " +
           "(LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<User> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 查找活跃用户
     */
    @Query("SELECT u FROM User u WHERE u.isDeleted = false AND u.isActive = true")
    Page<User> findActiveUsers(Pageable pageable);

    /**
     * 查找被锁定的用户
     */
    @Query("SELECT u FROM User u WHERE u.isDeleted = false AND u.isLocked = true")
    Page<User> findLockedUsers(Pageable pageable);

    /**
     * 查找未验证邮箱的用户
     */
    @Query("SELECT u FROM User u WHERE u.isDeleted = false AND u.emailVerified = false")
    Page<User> findUnverifiedUsers(Pageable pageable);

    /**
     * 更新用户最后登录时间
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :loginTime WHERE u.id = :userId")
    void updateLastLoginTime(@Param("userId") Long userId, @Param("loginTime") LocalDateTime loginTime);

    /**
     * 更新用户激活状态
     */
    @Modifying
    @Query("UPDATE User u SET u.isActive = :isActive WHERE u.id = :userId")
    void updateActiveStatus(@Param("userId") Long userId, @Param("isActive") Boolean isActive);

    /**
     * 更新用户锁定状态
     */
    @Modifying
    @Query("UPDATE User u SET u.isLocked = :isLocked WHERE u.id = :userId")
    void updateLockStatus(@Param("userId") Long userId, @Param("isLocked") Boolean isLocked);

    /**
     * 软删除用户
     */
    @Modifying
    @Query("UPDATE User u SET u.isDeleted = true, u.deletedAt = :deletedAt WHERE u.id = :userId")
    void softDeleteUser(@Param("userId") Long userId, @Param("deletedAt") LocalDateTime deletedAt);

    /**
     * 清除邮箱验证Token
     */
    @Modifying
    @Query("UPDATE User u SET u.emailVerificationToken = null, u.emailVerified = true WHERE u.id = :userId")
    void clearEmailVerificationToken(@Param("userId") Long userId);

    /**
     * 清除密码重置Token
     */
    @Modifying
    @Query("UPDATE User u SET u.passwordResetToken = null, u.passwordResetExpires = null WHERE u.id = :userId")
    void clearPasswordResetToken(@Param("userId") Long userId);

    /**
     * 统计用户数量
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.isDeleted = false")
    long countActiveUsers();

    /**
     * 统计今日注册用户数
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.isDeleted = false AND DATE(u.createdAt) = CURRENT_DATE")
    long countTodayRegistrations();

    /**
     * 统计本月注册用户数
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.isDeleted = false AND " +
           "YEAR(u.createdAt) = YEAR(CURRENT_DATE) AND MONTH(u.createdAt) = MONTH(CURRENT_DATE)")
    long countThisMonthRegistrations();

    /**
     * 查找最近登录的用户
     */
    @Query("SELECT u FROM User u WHERE u.isDeleted = false AND u.lastLoginAt IS NOT NULL " +
           "ORDER BY u.lastLoginAt DESC")
    Page<User> findRecentlyActiveUsers(Pageable pageable);

    /**
     * 查找长时间未登录的用户
     */
    @Query("SELECT u FROM User u WHERE u.isDeleted = false AND " +
           "(u.lastLoginAt IS NULL OR u.lastLoginAt < :cutoffDate)")
    Page<User> findInactiveUsers(@Param("cutoffDate") LocalDateTime cutoffDate, Pageable pageable);
}