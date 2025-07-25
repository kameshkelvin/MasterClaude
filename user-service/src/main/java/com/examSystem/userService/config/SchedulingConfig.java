package com.examSystem.userService.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 调度配置类
 * 
 * 启用Spring的调度和异步功能
 * 用于考试自动开始、结束等定时任务
 */
@Configuration
@EnableScheduling
@EnableAsync
public class SchedulingConfig {
    // Spring Boot会自动配置默认的TaskScheduler和TaskExecutor
    // 如果需要自定义，可以在这里添加Bean配置
}