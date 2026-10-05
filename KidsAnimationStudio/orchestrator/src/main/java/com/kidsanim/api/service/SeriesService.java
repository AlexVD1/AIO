package com.kidsanim.api.service;

import com.kidsanim.api.domain.Series;
import com.kidsanim.api.domain.StyleProfile;
import com.kidsanim.api.dto.CreateSeriesRequest;
import com.kidsanim.api.dto.SeriesDetailResponse;
import com.kidsanim.api.dto.SeriesResponse;
import com.kidsanim.api.dto.UpdateSeriesRequest;
import com.kidsanim.api.infrastructure.exception.ResourceNotFoundException;
import com.kidsanim.api.repository.SeriesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SeriesService {

    private final SeriesRepository seriesRepository;
    private final StyleProfileService styleProfileService;

    public SeriesService(SeriesRepository seriesRepository, StyleProfileService styleProfileService) {
        this.seriesRepository = seriesRepository;
        this.styleProfileService = styleProfileService;
    }

    @Transactional(readOnly = true)
    public List<SeriesResponse> findAll() {
        return seriesRepository.findAll().stream()
                .map(SeriesResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public SeriesDetailResponse findById(UUID id) {
        return SeriesDetailResponse.fromEntity(getEntityById(id));
    }

    @Transactional(readOnly = true)
    public Series getEntityById(UUID id) {
        return seriesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serie no encontrada con ID: " + id));
    }

    public SeriesResponse create(CreateSeriesRequest request) {
        StyleProfile styleProfile = styleProfileService.getEntityById(request.styleProfileId());

        Series series = new Series();
        series.setName(request.name());
        series.setDescription(request.description());
        series.setLanguage(request.language());
        series.setTargetAgeMin(request.targetAgeMin());
        series.setTargetAgeMax(request.targetAgeMax());
        series.setAspectRatio(request.aspectRatio());
        series.setStyleProfile(styleProfile);
        series.setDefaultBgmMood(request.defaultBgmMood());

        Series saved = seriesRepository.save(series);
        return SeriesResponse.fromEntity(saved);
    }

    public SeriesResponse update(UUID id, UpdateSeriesRequest request) {
        Series series = getEntityById(id);
        StyleProfile styleProfile = styleProfileService.getEntityById(request.styleProfileId());

        series.setName(request.name());
        series.setDescription(request.description());
        series.setLanguage(request.language());
        series.setTargetAgeMin(request.targetAgeMin());
        series.setTargetAgeMax(request.targetAgeMax());
        series.setAspectRatio(request.aspectRatio());
        series.setStyleProfile(styleProfile);
        series.setDefaultBgmMood(request.defaultBgmMood());

        Series updated = seriesRepository.save(series);
        return SeriesResponse.fromEntity(updated);
    }

    public void delete(UUID id) {
        Series series = getEntityById(id);
        seriesRepository.delete(series);
    }
}
