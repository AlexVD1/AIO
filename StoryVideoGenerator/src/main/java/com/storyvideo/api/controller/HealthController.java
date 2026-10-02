package com.storyvideo.api.controller;

import com.storyvideo.api.dto.HealthResponse;
import com.storyvideo.api.infrastructure.storage.AssetStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "Verificación de salud e integridad del sistema")
public class HealthController {

    private final String imageProvider;
    private final String sdApiUrl;
    private final AssetStorageService storageService;

    public HealthController(
            @Value("${story.image.provider:stable-diffusion-local}") String imageProvider,
            @Value("${story.image.sd-api-url:http://localhost:7860}") String sdApiUrl,
            AssetStorageService storageService) {
        this.imageProvider = imageProvider;
        this.sdApiUrl = sdApiUrl;
        this.storageService = storageService;
    }

    @GetMapping
    @Operation(summary = "Verificar estado del servicio", description = "Retorna el estado operativo general, almacenamiento e integraciones")
    public ResponseEntity<HealthResponse> checkHealth() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("imageProvider", imageProvider);
        details.put("sdApiUrl", sdApiUrl);

        try {
            File storageDir = storageService.resolveAbsolutePath("").toFile();
            details.put("storageReadable", storageDir.canRead());
            details.put("storageWritable", storageDir.canWrite());
            details.put("usableSpaceMB", storageDir.getUsableSpace() / (1024 * 1024));
        } catch (Exception e) {
            details.put("storageError", e.getMessage());
        }

        return ResponseEntity.ok(HealthResponse.ok("story-video-api", "0.0.1-SNAPSHOT", details));
    }
}
