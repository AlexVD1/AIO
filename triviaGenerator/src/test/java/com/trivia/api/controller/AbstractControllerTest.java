package com.trivia.api.controller;

import com.trivia.api.service.CatalogService;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Clase base para tests de integración de controllers.
 *
 * POR QUÉ ESTA CLASE EXISTE:
 *   La estrategia de tests sin BD (application.properties con autoconfigure.exclude)
 *   deshabilita JPA. Pero Spring Data JPA repositories son beans de Spring.
 *   Cuando Spring intenta crear los controllers, inyecta los services, que inyectan
 *   los repositories. Sin JPA activo, los repositories no existen → BeanCreationException.
 *
 *   Solución: declarar @MockBean para cada Service que tenga dependencias JPA.
 *   Los @MockBean reemplazan el bean real por un mock de Mockito en el contexto.
 *   Esto funciona incluso sin JPA activo porque Mockito no necesita la BD.
 *
 * PATRÓN:
 *   Todos los ControllerTest extienden esta clase:
 *     class HealthControllerTest extends AbstractControllerTest { ... }
 *     class CatalogControllerTest extends AbstractControllerTest { ... }
 *     class TriviaControllerTest extends AbstractControllerTest { ... }
 *
 *   Cada ControllerTest puede sobreescribir el comportamiento del @MockBean específico
 *   usando when(...) en sus @BeforeEach o @Test.
 *
 * MANTENIMIENTO:
 *   Cuando añadamos un nuevo @Service con dependencias JPA, agregarlo aquí como @MockBean.
 *   Los tests existentes no necesitan modificarse.
 *
 * @SpringBootTest hereda al extender — no necesita redeclararse en cada subclase.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractControllerTest {

    // ========================================================
    // @MockBean de todos los services con dependencias JPA
    // Agregar aquí cada nuevo @Service que use repositories
    // ========================================================

    /**
     * Mock del CatalogService.
     * Expuesto como campo protected para que los test hijos puedan
     * configurar comportamientos con when(...).thenReturn(...).
     */
    @MockBean
    protected CatalogService catalogService;

    @MockBean
    protected com.trivia.api.service.TriviaDuplicateService triviaDuplicateService;

    @MockBean
    protected com.trivia.api.service.TriviaGenerationService triviaGenerationService;

    @MockBean
    protected com.trivia.api.service.TriviaQueryService triviaQueryService;

    @MockBean
    protected com.trivia.api.service.AsyncTriviaPipelineExecutor asyncPipelineExecutor;
}
