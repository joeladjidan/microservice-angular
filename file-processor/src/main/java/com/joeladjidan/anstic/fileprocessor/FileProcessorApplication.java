package com.joeladjidan.anstic.fileprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class FileProcessorApplication {
    public static void main(String[] args) {
        SpringApplication.run(FileProcessorApplication.class, args);
    }
}

