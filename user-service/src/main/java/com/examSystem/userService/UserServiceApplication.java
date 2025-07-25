package com.examSystem.userService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * User Service Application
 * 
 * 在线考试系统用户服务主应用类
 * 提供用户注册、登录、个人信息管理等核心功能
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableTransactionManagement
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}