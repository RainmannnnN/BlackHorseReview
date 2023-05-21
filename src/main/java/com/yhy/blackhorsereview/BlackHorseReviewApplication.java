package com.yhy.blackhorsereview;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true)
@SpringBootApplication
@MapperScan("com.yhy.blackhorsereview.mapper")
public class BlackHorseReviewApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlackHorseReviewApplication.class, args);
    }

}
