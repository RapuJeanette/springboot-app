package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Clase principal de la aplicación Spring Boot.
 */
@SpringBootApplication
public class DemoApplication {

    /**
     * Método principal que inicia la aplicación.
     */
    public static void main(final String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}

@RestController
class HelloController {

    /**
     * Endpoint principal.
     */
    @GetMapping("/")
    public String hello() {
        return "Hello CI/CD World!";
    }

    /**
     * Endpoint de health check.
     */
    @GetMapping("/health")
    public String health() {
        return "Health check passed!";
    }
}