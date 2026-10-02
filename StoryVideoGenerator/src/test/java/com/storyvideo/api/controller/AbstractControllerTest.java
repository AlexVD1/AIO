package com.storyvideo.api.controller;

import com.storyvideo.api.infrastructure.storage.AssetStorageService;
import com.storyvideo.api.repository.*;
import com.storyvideo.api.service.StoryGenerationService;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Clase base para tests de integración web y smoke tests.
 *
 * Mantiene desacoplados los tests de la base de datos PostgreSQL real,
 * permitiendo ejecutar toda la suite de pruebas unitarias sin contenedores activos.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractControllerTest {

    @MockBean
    protected StoryRepository storyRepository;

    @MockBean
    protected StorySceneRepository storySceneRepository;

    @MockBean
    protected StoryCharacterRepository storyCharacterRepository;

    @MockBean
    protected StoryFingerprintRepository storyFingerprintRepository;

    @MockBean
    protected VisualStyleRepository visualStyleRepository;

    @MockBean
    protected BatchJobRepository batchJobRepository;

    @MockBean
    protected GeneratedAssetRepository generatedAssetRepository;

    @MockBean
    protected VideoProjectRepository videoProjectRepository;

    @MockBean
    protected VideoRenderRepository videoRenderRepository;

    @MockBean
    protected StoryGenerationService storyGenerationService;

    @MockBean
    protected com.storyvideo.api.service.StoryVisualService storyVisualService;

    @MockBean
    protected com.storyvideo.api.service.StoryAudioService storyAudioService;

    @MockBean
    protected com.storyvideo.api.service.VideoRenderService videoRenderService;

    @MockBean
    protected com.storyvideo.api.export.service.ExportService exportService;

    @MockBean
    protected com.storyvideo.api.pipeline.service.StoryPipelineExecutor storyPipelineExecutor;

    @MockBean
    protected AssetStorageService storageService;
}
