package com.chatapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ChatAppApplication {
    public static void main(String[] args) {
        System.out.println("Starting app");
        try {
            SpringApplication.run(ChatAppApplication.class, args);
        } catch (Exception e) {
            System.err.println("Application failed to start:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
