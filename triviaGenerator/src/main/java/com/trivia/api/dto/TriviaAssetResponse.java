package com.trivia.api.dto;

import com.trivia.api.domain.TipoAsset;
import com.trivia.api.domain.TriviaAsset;

import java.util.UUID;

/**
 * DTO para representar un asset (imagen PNG) asociado a una trivia.
 */
public record TriviaAssetResponse(
        UUID id,
        TipoAsset tipo,
        String url
) {
    public static TriviaAssetResponse from(TriviaAsset a) {
        return new TriviaAssetResponse(
                a.getId(),
                a.getTipo(),
                a.getUrl()
        );
    }
}
