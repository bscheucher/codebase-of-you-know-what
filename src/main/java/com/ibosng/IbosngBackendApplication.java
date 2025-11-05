package com.ibosng;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication()
public class IbosngBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(IbosngBackendApplication.class, args);
    }

    @PostConstruct
    public void init() {
        System.out.println("iBos-nG running!");
    }

}
