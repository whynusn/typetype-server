package com.typetype;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * TypeType Server 启动类
 *
 * @SpringBootApplication 是三个注解的组合：
 * 1. @Configuration — 配置类
 * 2. @EnableAutoConfiguration — 自动配置（根据依赖自动加载组件）
 * 3. @ComponentScan — 自动扫描当前包及其子包下的组件
 *
 * 🎓 学习点：
 * - Spring Boot 启动流程：加载配置 → 初始化容器 → 启动内嵌 Tomcat
 * - 自动配置原理：根据 classpath 下的依赖决定启用哪些功能
 * - 包扫描：默认扫描启动类所在包及其子包
 */
@SpringBootApplication
public class TypetypeApplication {

    /**
     * 主方法：Spring Boot 应用入口
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(TypetypeApplication.class, args);
    }
}
