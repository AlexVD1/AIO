package com.storyvideo.api;

import com.storyvideo.api.controller.AbstractControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Smoke Test — Contexto de Spring Boot de StoryVideoGenerator")
class StoryVideoApiApplicationTests extends AbstractControllerTest {

    @Test
    @DisplayName("El contexto de Spring Boot arranca correctamente sin errores de configuración")
    void contextLoads() {
        // Verifica que la configuración de beans y dependencias cargue limpiamente
    }
}
