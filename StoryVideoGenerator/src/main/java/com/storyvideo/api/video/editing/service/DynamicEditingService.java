package com.storyvideo.api.video.editing.service;

import com.storyvideo.api.domain.StoryGenre;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DynamicEditingService {

    public String resolveVisualEffectsFilter(StoryGenre genre, List<String> sceneEffects) {
        List<String> filters = new ArrayList<>();

        // 1. Efectos base según el género
        if (genre != null) {
            switch (genre) {
                case HORROR -> {
                    filters.add("vignette=PI/4");
                    filters.add("noise=c0s=6:c0f=t");
                }
                case MYSTERY -> {
                    filters.add("vignette=PI/5");
                }
                case SCI_FI -> {
                    filters.add("rgbashift=rh=2:bh=-2:rv=-1:bv=1");
                }
                case DYSTOPIA -> {
                    filters.add("vignette=PI/4");
                    filters.add("noise=c0s=5:c0f=t");
                    filters.add("rgbashift=rh=1:bh=-1");
                }
                case PSYCHOLOGICAL -> {
                    filters.add("vignette=PI/3.5");
                    filters.add("noise=c0s=4:c0f=t");
                }
                case URBAN_LEGEND, STRANGE_EVENTS -> {
                    filters.add("vignette=PI/4.5");
                }
                default -> {
                    filters.add("vignette=PI/5");
                }
            }
        }

        // 2. Efectos personalizados explícitos de la escena
        if (sceneEffects != null) {
            for (String effect : sceneEffects) {
                if (effect == null) continue;
                String normalized = effect.trim().toUpperCase();
                switch (normalized) {
                    case "GLITCH" -> filters.add("rgbashift=rh=6:bh=-6");
                    case "GRAIN" -> filters.add("noise=c0s=8:c0f=t");
                    case "BLUR" -> filters.add("boxblur=luma_radius=2:luma_power=1");
                    case "CHROMATIC_ABERRATION" -> filters.add("rgbashift=rh=3:bh=-3:rv=-1:bv=1");
                    case "VIGNETTE" -> filters.add("vignette=PI/4");
                }
            }
        }

        if (filters.isEmpty()) {
            return "";
        }

        return "," + String.join(",", filters);
    }
}
