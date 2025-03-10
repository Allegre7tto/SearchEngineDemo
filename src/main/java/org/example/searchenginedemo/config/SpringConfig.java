package org.example.searchenginedemo.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("org.example.searchenginedemo.mapper")
public class SpringConfig {
}
