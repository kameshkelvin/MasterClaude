package com.examSystem.userService.security;

import com.examSystem.userService.entity.User;
import com.examSystem.userService.entity.UserRole;
import com.examSystem.userService.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Spring Security用户详情服务实现
 * 
 * 基于系统设计文档中的用户认证架构
 * 负责加载用户信息和权限，为Spring Security提供用户认证数据
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    @Autowired
    private UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("Loading user details for username: {}", username);

        try {
            // 根据用户名查找用户
            User user = userService.findByUsername(username);
            if (user == null) {
                logger.warn("User not found with username: {}", username);
                throw new UsernameNotFoundException("User not found with username: " + username);
            }

            // 检查用户状态
            if (!user.getIsActive()) {
                logger.warn("User account is disabled: {}", username);
                throw new UsernameNotFoundException("User account is disabled: " + username);
            }

            if (user.getIsDeleted()) {
                logger.warn("User account is deleted: {}", username);
                throw new UsernameNotFoundException("User account is deleted: " + username);
            }

            // 获取用户角色和权限
            Collection<? extends GrantedAuthority> authorities = getAuthorities(user);

            logger.debug("Successfully loaded user details for: {} with {} authorities", 
                username, authorities.size());

            // 创建Spring Security UserDetails对象
            return new CustomUserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getEmail(),
                user.getIsActive(),
                !user.getIsDeleted(),
                !user.getIsLocked(),
                true, // accountNonExpired - 可以根据业务需求调整
                authorities
            );

        } catch (Exception e) {
            logger.error("Error loading user details for username: {}", username, e);
            throw new UsernameNotFoundException("Error loading user: " + username, e);
        }
    }

    /**
     * 获取用户权限集合
     */
    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        return user.getRoles().stream()
            .filter(UserRole::isValid) // 只取有效的角色
            .map(userRole -> new SimpleGrantedAuthority("ROLE_" + userRole.getRole().name()))
            .collect(Collectors.toList());
    }

    /**
     * 自定义用户主体类
     * 扩展Spring Security的UserDetails接口，包含更多用户信息
     */
    public static class CustomUserPrincipal implements UserDetails {
        
        private final Long userId;
        private final String username;
        private final String password;
        private final String email;
        private final boolean enabled;
        private final boolean accountNonDeleted;
        private final boolean accountNonLocked;
        private final boolean accountNonExpired;
        private final Collection<? extends GrantedAuthority> authorities;

        public CustomUserPrincipal(Long userId, String username, String password, String email,
                                 boolean enabled, boolean accountNonDeleted, boolean accountNonLocked,
                                 boolean accountNonExpired, Collection<? extends GrantedAuthority> authorities) {
            this.userId = userId;
            this.username = username;
            this.password = password;
            this.email = email;
            this.enabled = enabled;
            this.accountNonDeleted = accountNonDeleted;
            this.accountNonLocked = accountNonLocked;
            this.accountNonExpired = accountNonExpired;
            this.authorities = authorities;
        }

        // UserDetails接口实现
        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
        }

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public boolean isAccountNonExpired() {
            return accountNonExpired;
        }

        @Override
        public boolean isAccountNonLocked() {
            return accountNonLocked;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true; // 密码不过期，可以根据业务需求调整
        }

        @Override
        public boolean isEnabled() {
            return enabled && accountNonDeleted;
        }

        // 自定义属性访问器
        public Long getUserId() {
            return userId;
        }

        public String getEmail() {
            return email;
        }

        public boolean isAccountNonDeleted() {
            return accountNonDeleted;
        }

        @Override
        public String toString() {
            return "CustomUserPrincipal{" +
                    "userId=" + userId +
                    ", username='" + username + '\'' +
                    ", email='" + email + '\'' +
                    ", enabled=" + enabled +
                    ", accountNonDeleted=" + accountNonDeleted +
                    ", accountNonLocked=" + accountNonLocked +
                    ", accountNonExpired=" + accountNonExpired +
                    ", authorities=" + authorities +
                    '}';
        }
    }
}