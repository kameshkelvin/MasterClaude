package com.examSystem.userService.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI配置类
 * 
 * 基于系统设计文档中的API文档架构
 * 配置Swagger UI和API文档生成
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8081}")
    private int serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        // 服务器配置
        Server localServer = new Server()
            .url("http://localhost:" + serverPort)
            .description("本地开发环境");

        Server stagingServer = new Server()
            .url("https://api-staging.exam.yourdomain.com")
            .description("测试环境");

        Server productionServer = new Server()
            .url("https://api.exam.yourdomain.com")
            .description("生产环境");

        // 联系信息
        Contact contact = new Contact()
            .name("考试系统开发团队")
            .email("dev@examSystem.com")
            .url("https://www.examSystem.com");

        // 许可证信息
        License license = new License()
            .name("MIT License")
            .url("https://opensource.org/licenses/MIT");

        // API信息
        Info info = new Info()
            .title("考试系统用户服务 API")
            .description("""
                ## 用户服务API文档
                
                考试系统用户服务提供用户管理、认证授权等核心功能。
                
                ### 主要功能模块
                
                1. **用户认证** (`/api/v1/auth`)
                   - 用户注册、登录、登出
                   - JWT Token管理
                   - 密码重置
                
                2. **用户管理** (`/api/v1/users`)
                   - 用户资料管理
                   - 密码修改
                   - 用户查询（管理员）
                
                ### 认证方式
                
                API使用JWT Bearer Token进行认证。获取Token后，在请求头中添加：
                ```
                Authorization: Bearer <your_access_token>
                ```
                
                ### 错误码说明
                
                - `200` - 请求成功
                - `201` - 创建成功
                - `400` - 请求参数错误
                - `401` - 未认证或Token无效
                - `403` - 权限不足
                - `404` - 资源不存在
                - `500` - 服务器内部错误
                
                ### 数据格式
                
                所有API请求和响应均使用JSON格式，字符编码为UTF-8。
                时间格式统一使用ISO 8601标准 (`yyyy-MM-dd HH:mm:ss`)。
                """)
            .version("1.0.0")
            .contact(contact)
            .license(license);

        // JWT安全配置
        SecurityScheme jwtSecurityScheme = new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description("JWT认证Token，格式：Bearer <token>");

        SecurityRequirement jwtSecurityRequirement = new SecurityRequirement()
            .addList("JWT Authentication");

        // 组件配置
        Components components = new Components()
            .addSecuritySchemes("JWT Authentication", jwtSecurityScheme);

        return new OpenAPI()
            .info(info)
            .servers(List.of(localServer, stagingServer, productionServer))
            .components(components)
            .addSecurityItem(jwtSecurityRequirement);
    }
}