package com.examSystem.userService.service;

import com.examSystem.userService.entity.User;
import com.examSystem.userService.repository.UserRepository;
import com.examSystem.userService.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 用户服务单元测试
 * 
 * 基于系统设计文档中的测试架构
 * 测试用户注册、登录、信息管理等核心功能
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private String testUsername = "testuser";
    private String testEmail = "test@example.com";
    private String testPassword = "TestPassword123";
    private String encodedPassword = "encoded_password";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername(testUsername);
        testUser.setEmail(testEmail);
        testUser.setPasswordHash(encodedPassword);
        testUser.setIsActive(true);
        testUser.setIsLocked(false);
        testUser.setIsDeleted(false);
        testUser.setEmailVerified(false);
    }

    @Test
    void testRegister_Success() {
        // Given
        when(userRepository.existsByUsernameAndIsDeletedFalse(testUsername)).thenReturn(false);
        when(userRepository.existsByEmailAndIsDeletedFalse(testEmail)).thenReturn(false);
        when(passwordEncoder.encode(testPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.register(testUsername, testEmail, testPassword, null);

        // Then
        assertNotNull(result);
        assertEquals(testUsername, result.getUsername());
        assertEquals(testEmail, result.getEmail());
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode(testPassword);
    }

    @Test
    void testRegister_UsernameExists() {
        // Given
        when(userRepository.existsByUsernameAndIsDeletedFalse(testUsername)).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.register(testUsername, testEmail, testPassword, null)
        );

        assertTrue(exception.getMessage().contains("用户名已存在"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testRegister_EmailExists() {
        // Given
        when(userRepository.existsByUsernameAndIsDeletedFalse(testUsername)).thenReturn(false);
        when(userRepository.existsByEmailAndIsDeletedFalse(testEmail)).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.register(testUsername, testEmail, testPassword, null)
        );

        assertTrue(exception.getMessage().contains("邮箱已存在"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testRegister_InvalidInput() {
        // Test empty username
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.register("", testEmail, testPassword, null)
        );
        assertTrue(exception.getMessage().contains("用户名不能为空"));

        // Test empty email
        exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.register(testUsername, "", testPassword, null)
        );
        assertTrue(exception.getMessage().contains("邮箱不能为空"));

        // Test short password
        exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.register(testUsername, testEmail, "short", null)
        );
        assertTrue(exception.getMessage().contains("密码长度不能少于8位"));
    }

    @Test
    void testAuthenticate_Success() {
        // Given
        when(userRepository.findByUsernameAndIsDeletedFalse(testUsername))
            .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(testPassword, encodedPassword)).thenReturn(true);

        // When
        User result = userService.authenticate(testUsername, testPassword);

        // Then
        assertNotNull(result);
        assertEquals(testUsername, result.getUsername());
        verify(passwordEncoder).matches(testPassword, encodedPassword);
    }

    @Test
    void testAuthenticate_UserNotFound() {
        // Given
        when(userRepository.findByUsernameAndIsDeletedFalse(testUsername))
            .thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.authenticate(testUsername, testPassword)
        );

        assertTrue(exception.getMessage().contains("用户名或密码错误"));
    }

    @Test
    void testAuthenticate_WrongPassword() {
        // Given
        when(userRepository.findByUsernameAndIsDeletedFalse(testUsername))
            .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(testPassword, encodedPassword)).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.authenticate(testUsername, testPassword)
        );

        assertTrue(exception.getMessage().contains("用户名或密码错误"));
    }

    @Test
    void testAuthenticate_AccountDisabled() {
        // Given
        testUser.setIsActive(false);
        when(userRepository.findByUsernameAndIsDeletedFalse(testUsername))
            .thenReturn(Optional.of(testUser));

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.authenticate(testUsername, testPassword)
        );

        assertTrue(exception.getMessage().contains("账户已被禁用"));
    }

    @Test
    void testAuthenticate_AccountLocked() {
        // Given
        testUser.setIsLocked(true);
        when(userRepository.findByUsernameAndIsDeletedFalse(testUsername))
            .thenReturn(Optional.of(testUser));

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.authenticate(testUsername, testPassword)
        );

        assertTrue(exception.getMessage().contains("账户已被锁定"));
    }

    @Test
    void testFindByUsername_Success() {
        // Given
        when(userRepository.findByUsernameAndIsDeletedFalse(testUsername))
            .thenReturn(Optional.of(testUser));

        // When
        User result = userService.findByUsername(testUsername);

        // Then
        assertNotNull(result);
        assertEquals(testUsername, result.getUsername());
    }

    @Test
    void testFindByUsername_NotFound() {
        // Given
        when(userRepository.findByUsernameAndIsDeletedFalse(testUsername))
            .thenReturn(Optional.empty());

        // When
        User result = userService.findByUsername(testUsername);

        // Then
        assertNull(result);
    }

    @Test
    void testFindByEmail_Success() {
        // Given
        when(userRepository.findByEmailAndIsDeletedFalse(testEmail))
            .thenReturn(Optional.of(testUser));

        // When
        User result = userService.findByEmail(testEmail);

        // Then
        assertNotNull(result);
        assertEquals(testEmail, result.getEmail());
    }

    @Test
    void testUpdatePassword_Success() {
        // Given
        Long userId = 1L;
        String oldPassword = "oldPassword";
        String newPassword = "newPassword123";
        String newEncodedPassword = "new_encoded_password";

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(oldPassword, encodedPassword)).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn(newEncodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        assertDoesNotThrow(() -> userService.updatePassword(userId, oldPassword, newPassword));

        // Then
        verify(passwordEncoder).matches(oldPassword, encodedPassword);
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(testUser);
    }

    @Test
    void testUpdatePassword_WrongOldPassword() {
        // Given
        Long userId = 1L;
        String oldPassword = "wrongPassword";
        String newPassword = "newPassword123";

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(oldPassword, encodedPassword)).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.updatePassword(userId, oldPassword, newPassword)
        );

        assertTrue(exception.getMessage().contains("原密码错误"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testUpdatePassword_ShortNewPassword() {
        // Given
        Long userId = 1L;
        String oldPassword = "oldPassword";
        String newPassword = "short";

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.updatePassword(userId, oldPassword, newPassword)
        );

        assertTrue(exception.getMessage().contains("新密码长度不能少于8位"));
    }

    @Test
    void testExistsByUsername() {
        // Given
        when(userRepository.existsByUsernameAndIsDeletedFalse(testUsername)).thenReturn(true);

        // When
        boolean result = userService.existsByUsername(testUsername);

        // Then
        assertTrue(result);
        verify(userRepository).existsByUsernameAndIsDeletedFalse(testUsername);
    }

    @Test
    void testExistsByEmail() {
        // Given
        when(userRepository.existsByEmailAndIsDeletedFalse(testEmail)).thenReturn(true);

        // When
        boolean result = userService.existsByEmail(testEmail);

        // Then
        assertTrue(result);
        verify(userRepository).existsByEmailAndIsDeletedFalse(testEmail);
    }

    @Test
    void testActivateUser() {
        // Given
        Long userId = 1L;

        // When
        userService.activateUser(userId);

        // Then
        verify(userRepository).updateActiveStatus(userId, true);
    }

    @Test
    void testDeactivateUser() {
        // Given
        Long userId = 1L;

        // When
        userService.deactivateUser(userId);

        // Then
        verify(userRepository).updateActiveStatus(userId, false);
    }

    @Test
    void testLockUser() {
        // Given
        Long userId = 1L;
        String reason = "Suspicious activity";

        // When
        userService.lockUser(userId, reason);

        // Then
        verify(userRepository).updateLockStatus(userId, true);
    }

    @Test
    void testUnlockUser() {
        // Given
        Long userId = 1L;

        // When
        userService.unlockUser(userId);

        // Then
        verify(userRepository).updateLockStatus(userId, false);
    }

    @Test
    void testDeleteUser() {
        // Given
        Long userId = 1L;

        // When
        userService.deleteUser(userId);

        // Then
        verify(userRepository).softDeleteUser(eq(userId), any());
    }
}