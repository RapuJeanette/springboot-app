package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Main Spring Boot application class.
 * <p>
 * This class serves as the entry point for
 * the Spring Boot application and exposes
 * a simple health check endpoint at <code>/health</code>.
 * </p>
 * <p>
 * Annotations:
 * <ul>
 *   <li>{@link org.springframework.boot.autoconfigure.SpringBootApplication} -
 * Indicates a Spring Boot application.</li>
 *   <li>{@link org.springframework.web.bind.annotation.RestController}
 * - Marks this class as a REST controller.</li>
 * </ul>
 * </p>
 */
@SpringBootApplication
@RestController
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class DemoApplication {

    /**
     * The main entry point of the Spring Boot application.
     *
     * @param args command-line arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    /**
     * Health check endpoint.
     * <p>
     * Returns a simple message confirming that
     * the Spring Boot application is running.
     * </p>
     *
     * @return a string message indicating the status of the application
     */
    @GetMapping("/health")
    public static String healthCheck() {
        return "OK - Usando IA Generativa para el pipeline";
    }

    /**
     * Root endpoint '/'
     */
    @GetMapping("/")
    public static String root() {
        return "Bienvenido a la aplicación. Endpoint de salud en /health";
    }

}
