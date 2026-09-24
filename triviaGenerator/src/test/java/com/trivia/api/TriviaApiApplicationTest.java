package com.trivia.api;

import com.trivia.api.controller.AbstractControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test de smoke: verifica que el contexto de Spring Boot
 * carga correctamente sin errores de configuración.
 *
 * Extiende AbstractControllerTest para heredar la configuración base
 * y los mocks de servicios que dependen de JPA (ya que JPA está deshabilitado
 * en tests sin contenedor de BD).
 */
@DisplayName("Smoke Test — Contexto de Spring Boot carga correctamente")
class TriviaApiApplicationTest extends AbstractControllerTest {

    @Test
    @DisplayName("El contexto de Spring carga sin errores")
    void contextLoads() {
        // Si Spring Boot no puede arrancar, este test lanza una excepción
        // y falla antes de llegar al assert.
    }
}
