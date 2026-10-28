package com.guolian.pm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 国联征信项目管理平台 - 底座脚手架启动类。
 */
@SpringBootApplication
@MapperScan("com.guolian.pm.**.mapper")
@EnableAsync
@EnableScheduling
public class PmApplication {

    public static void main(String[] args) {
        SpringApplication.run(PmApplication.class, args);
    }
}
