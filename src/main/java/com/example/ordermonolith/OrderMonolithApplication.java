package com.example.ordermonolith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * Spring Boot entry point.
 *
 * <p>{@link ConfigurationPropertiesScan} activates the {@code @ConfigurationProperties}
 * records ({@code PricingProperties}, {@code PaymentProperties}) so pricing rules
 * and payment config come from {@code application.yml} / the environment rather
 * than from literals in the flow.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class OrderMonolithApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderMonolithApplication.class, args);
    }

    /**
     * The single shared {@link RestTemplate} for outbound calls. Previously one
     * payment branch injected this bean while another {@code new}-ed its own;
     * every {@code PaymentStrategy} now takes this one by constructor injection.
     */
    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
