package com.trivia.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Punto de entrada de la aplicación trivia-api.
 *
 * NOTA — Fase 2:
 * DataSource, JPA y Flyway están deshabilitados mediante
 * spring.autoconfigure.exclude en application.yml.
 * Esto permite arrancar sin base de datos durante el desarrollo inicial.
 * En la Fase 3 se eliminarán esos excludes y se habilitará la DB.
 *
 * @EnableAsync — Habilita el procesamiento asíncrono de Spring.
 * En la Fase 14 se usará para que POST /api/v1/trivias devuelva
 * HTTP 202 inmediatamente mientras la generación ocurre en background.
 */
@SpringBootApplication
@EnableAsync
public class TriviaApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TriviaApiApplication.class, args);
    }
}
