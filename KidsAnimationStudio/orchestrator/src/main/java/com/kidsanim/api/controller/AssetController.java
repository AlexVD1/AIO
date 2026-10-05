package com.kidsanim.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/v1/assets")
@Tag(name = "Assets", description = "Visualización y descarga de archivos multimedia")
public class AssetController {

    @GetMapping("/view")
    @Operation(summary = "Visualizar archivo multimedia local (imagen, audio, video)")
    public ResponseEntity<Resource> viewAsset(@RequestParam String path) {
        if (path == null || path.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            return ResponseEntity.notFound().build();
        }

        String contentType = null;
        try {
            contentType = Files.probeContentType(file.toPath());
        } catch (IOException ignored) {}

        if (contentType == null) {
            String lower = file.getName().toLowerCase();
            if (lower.endsWith(".png")) contentType = "image/png";
            else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) contentType = "image/jpeg";
            else if (lower.endsWith(".webp")) contentType = "image/webp";
            else if (lower.endsWith(".wav")) contentType = "audio/wav";
            else if (lower.endsWith(".mp3")) contentType = "audio/mpeg";
            else if (lower.endsWith(".mp4")) contentType = "video/mp4";
            else if (lower.endsWith(".json")) contentType = "application/json";
            else contentType = "application/octet-stream";
        }

        FileSystemResource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }
}
