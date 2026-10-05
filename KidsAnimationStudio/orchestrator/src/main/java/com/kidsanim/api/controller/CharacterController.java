package com.kidsanim.api.controller;

import com.kidsanim.api.dto.*;
import com.kidsanim.api.service.CharacterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/characters")
@Tag(name = "Characters", description = "Gestión de personajes, hojas de referencia y aprobación")
public class CharacterController {

    private final CharacterService characterService;

    public CharacterController(CharacterService characterService) {
        this.characterService = characterService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de un personaje por ID")
    public ResponseEntity<CharacterResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(characterService.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un personaje existente")
    public ResponseEntity<CharacterResponse> update(@PathVariable UUID id,
                                                    @Valid @RequestBody UpdateCharacterRequest request) {
        return ResponseEntity.ok(characterService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un personaje")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        characterService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/generate-sheet")
    @Operation(summary = "Generar candidatas de hoja de referencia mediante AI Gateway")
    public ResponseEntity<CharacterSheetResponse> generateSheet(
            @PathVariable UUID id,
            @RequestBody(required = false) GenerateSheetRequest request) {
        CharacterSheetResponse sheet = characterService.generateSheet(id, request);
        return ResponseEntity.ok(sheet);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Aprobar una imagen de referencia para el personaje (estado -> APPROVED)")
    public ResponseEntity<CharacterResponse> approve(
            @PathVariable UUID id,
            @Valid @RequestBody ApproveCharacterRequest request) {
        CharacterResponse approved = characterService.approve(id, request);
        return ResponseEntity.ok(approved);
    }
}
