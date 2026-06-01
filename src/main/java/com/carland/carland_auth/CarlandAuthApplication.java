package com.carland.carland_auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients

public class CarlandAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(CarlandAuthApplication.class, args);
    }

}
