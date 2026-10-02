package com.storyvideo.api.controller;

import com.storyvideo.api.domain.BatchJobStatus;
import com.storyvideo.api.domain.StoryStatus;
import com.storyvideo.api.export.service.AutoCleanupService;
import com.storyvideo.api.repository.BatchJobRepository;
import com.storyvideo.api.repository.StoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/stats")
@Tag(name = "Stats", description = "Estadísticas globales de uso del sistema")
public class StatsController {

    private final StoryRepository storyRepository;
    private final BatchJobRepository batchJobRepository;
    private final AutoCleanupService autoCleanupService;

    public StatsController(
            StoryRepository storyRepository,
            BatchJobRepository batchJobRepository,
            AutoCleanupService autoCleanupService) {
        this.storyRepository = storyRepository;
        this.batchJobRepository = batchJobRepository;
        this.autoCleanupService = autoCleanupService;
    }

    @GetMapping
    @Operation(summary = "Estadísticas del sistema", description = "Retorna contadores de historias, renders, lotes y espacio en disco")
    public ResponseEntity<Map<String, Object>> getSystemStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        long totalStories = storyRepository.count();
        long totalBatches = batchJobRepository.count();
        long usableDiskMb = autoCleanupService.checkDiskSpace();

        stats.put("totalStories", totalStories);
        stats.put("totalBatches", totalBatches);
        stats.put("usableDiskSpaceMb", usableDiskMb);
        stats.put("serviceStatus", "OPERATIONAL");

        return ResponseEntity.ok(stats);
    }
}
