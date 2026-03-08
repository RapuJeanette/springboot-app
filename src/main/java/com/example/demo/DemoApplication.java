package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Clase principal de la aplicación Spring Boot.
 */
@SpringBootApplication
public final class DemoApplication {

    /**
     * Constructor privado para evitar instanciación.
     */
    private DemoApplication() {
    }

    /**
     * Método principal que inicia la aplicación.
     *
     * @param args argumentos de línea de comandos
     */
    public static void main(final String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}

/**
 * Controlador REST de ejemplo.
 */
@RestController
class HelloController {

    /**
     * Endpoint principal.
     *
     * @return mensaje de bienvenida
     */
    @GetMapping("/")
    public String hello() {
        return "Hello CI/CD World!";
    }

    /**
     * Endpoint de verificación de salud.
     *
     * @return estado de la aplicación
     */
    @GetMapping("/health")
    public String health() {
        return "Health check passed!";
    }
}
