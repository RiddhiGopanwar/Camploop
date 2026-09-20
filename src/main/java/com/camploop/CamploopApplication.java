package com.camploop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Camploop - Campus Marketplace for Students
 * Version 1
 *
 * A campus-exclusive marketplace where every student has ONE account
 * and can both buy and sell. No separate buyer/seller roles.
 */
@SpringBootApplication
public class CamploopApplication {
    public static void main(String[] args) {
        SpringApplication.run(CamploopApplication.class, args);
    }
}
