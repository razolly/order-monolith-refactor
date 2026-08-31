package com.example.ordermonolith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * Entry point for the (intentionally messy) Order monolith.
 *
 * <p>Everything in this project compiles and runs as-is. The point of the exercise is
 * NOT to fix bugs, it is to restructure working-but-ugly code into something a senior
 * engineer would be comfortable shipping and maintaining.
 */
@SpringBootApplication
public class OrderMonolithApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderMonolithApplication.class, args);
    }

    /**
     * A single shared RestTemplate. It is currently new-ed up inside the controller in
     * some places and injected in others - part of the mess to clean up.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
