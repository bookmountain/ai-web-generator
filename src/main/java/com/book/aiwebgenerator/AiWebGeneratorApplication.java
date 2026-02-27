package com.book.aiwebgenerator;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.book.aiwebgenerator.mapper")
public class AiWebGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiWebGeneratorApplication.class, args);
    }

}
