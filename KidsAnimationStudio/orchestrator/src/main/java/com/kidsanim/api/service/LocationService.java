package com.kidsanim.api.service;

import com.kidsanim.api.domain.Location;
import com.kidsanim.api.domain.Series;
import com.kidsanim.api.dto.CreateLocationRequest;
import com.kidsanim.api.dto.LocationResponse;
import com.kidsanim.api.dto.UpdateLocationRequest;
import com.kidsanim.api.infrastructure.exception.ResourceNotFoundException;
import com.kidsanim.api.repository.LocationRepository;
import com.kidsanim.api.repository.SeriesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class LocationService {

    private final LocationRepository locationRepository;
    private final SeriesRepository seriesRepository;

    public LocationService(LocationRepository locationRepository, SeriesRepository seriesRepository) {
        this.locationRepository = locationRepository;
        this.seriesRepository = seriesRepository;
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> findBySeriesId(UUID seriesId) {
        if (!seriesRepository.existsById(seriesId)) {
            throw new ResourceNotFoundException("Serie no encontrada con ID: " + seriesId);
        }
        return locationRepository.findBySeriesId(seriesId).stream()
                .map(LocationResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public LocationResponse findById(UUID id) {
        return LocationResponse.fromEntity(getEntityById(id));
    }

    @Transactional(readOnly = true)
    public Location getEntityById(UUID id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Locación no encontrada con ID: " + id));
    }

    public LocationResponse create(UUID seriesId, CreateLocationRequest request) {
        Series series = seriesRepository.findById(seriesId)
                .orElseThrow(() -> new ResourceNotFoundException("Serie no encontrada con ID: " + seriesId));

        Location location = new Location();
        location.setSeries(series);
        location.setName(request.name());
        location.setPrompt(request.prompt());
        location.setReferenceImagePath(request.referenceImagePath());

        Location saved = locationRepository.save(location);
        return LocationResponse.fromEntity(saved);
    }

    public LocationResponse update(UUID id, UpdateLocationRequest request) {
        Location location = getEntityById(id);
        location.setName(request.name());
        location.setPrompt(request.prompt());
        location.setReferenceImagePath(request.referenceImagePath());

        Location updated = locationRepository.save(location);
        return LocationResponse.fromEntity(updated);
    }

    public void delete(UUID id) {
        Location location = getEntityById(id);
        locationRepository.delete(location);
    }
}
