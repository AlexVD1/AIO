package com.kidsanim.api.service;

import com.kidsanim.api.domain.Series;
import com.kidsanim.api.domain.StyleProfile;
import com.kidsanim.api.dto.CreateSeriesRequest;
import com.kidsanim.api.dto.SeriesDetailResponse;
import com.kidsanim.api.dto.SeriesResponse;
import com.kidsanim.api.dto.UpdateSeriesRequest;
import com.kidsanim.api.infrastructure.exception.ResourceNotFoundException;
import com.kidsanim.api.repository.SeriesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SeriesServiceTest {

    private SeriesRepository seriesRepository;
    private StyleProfileService styleProfileService;
    private SeriesService seriesService;

    @BeforeEach
    void setUp() {
        seriesRepository = mock(SeriesRepository.class);
        styleProfileService = mock(StyleProfileService.class);
        seriesService = new SeriesService(seriesRepository, styleProfileService);
    }

    @Test
    void createSeriesAssociatesStyleProfile() {
        UUID styleId = UUID.randomUUID();
        StyleProfile styleProfile = new StyleProfile("3D Style", "ckpt.safetensors", "style", "neg");
        styleProfile.setId(styleId);

        when(styleProfileService.getEntityById(styleId)).thenReturn(styleProfile);
        when(seriesRepository.save(any(Series.class))).thenAnswer(inv -> {
            Series s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        CreateSeriesRequest request = new CreateSeriesRequest("Tito el zorrito", "Aventuras", "es-MX",
                2, 5, "16:9", styleId, "playful");

        SeriesResponse response = seriesService.create(request);

        assertThat(response.name()).isEqualTo("Tito el zorrito");
        assertThat(response.styleProfileId()).isEqualTo(styleId);
        assertThat(response.defaultBgmMood()).isEqualTo("playful");
    }

    @Test
    void findByIdReturnsDetailWithCounts() {
        UUID seriesId = UUID.randomUUID();
        UUID styleId = UUID.randomUUID();
        StyleProfile styleProfile = new StyleProfile("3D Style", "ckpt.safetensors", "style", "neg");
        styleProfile.setId(styleId);

        Series series = new Series("Tito", "Desc", styleProfile);
        series.setId(seriesId);

        when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(series));

        SeriesDetailResponse detail = seriesService.findById(seriesId);

        assertThat(detail.name()).isEqualTo("Tito");
        assertThat(detail.styleProfile().name()).isEqualTo("3D Style");
        assertThat(detail.characterCount()).isEqualTo(0);
    }

    @Test
    void findByIdThrowsWhenNotFound() {
        UUID seriesId = UUID.randomUUID();
        when(seriesRepository.findById(seriesId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> seriesService.findById(seriesId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(seriesId.toString());
    }

    @Test
    void updateSeriesModifiesFields() {
        UUID seriesId = UUID.randomUUID();
        UUID styleId = UUID.randomUUID();
        StyleProfile styleProfile = new StyleProfile("3D Style", "ckpt.safetensors", "style", "neg");
        styleProfile.setId(styleId);

        Series series = new Series("Tito", "Desc", styleProfile);
        series.setId(seriesId);

        when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(series));
        when(styleProfileService.getEntityById(styleId)).thenReturn(styleProfile);
        when(seriesRepository.save(any(Series.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateSeriesRequest request = new UpdateSeriesRequest("Tito 2.0", "Nueva desc", "es-MX",
                3, 6, "16:9", styleId, "calm");

        SeriesResponse updated = seriesService.update(seriesId, request);

        assertThat(updated.name()).isEqualTo("Tito 2.0");
        assertThat(updated.description()).isEqualTo("Nueva desc");
        assertThat(updated.defaultBgmMood()).isEqualTo("calm");
    }

    @Test
    void deleteSeriesRemovesEntity() {
        UUID seriesId = UUID.randomUUID();
        Series series = new Series();
        series.setId(seriesId);

        when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(series));
        doNothing().when(seriesRepository).delete(series);

        seriesService.delete(seriesId);

        verify(seriesRepository).delete(series);
    }
}
