package com.qiankubx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication
public class QianKuApplication {

    public static void main(String[] args) {
        SpringApplication.run(QianKuApplication.class, args);
    }
}
