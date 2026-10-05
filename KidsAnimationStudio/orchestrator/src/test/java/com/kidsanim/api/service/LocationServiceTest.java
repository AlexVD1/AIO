package com.kidsanim.api.service;

import com.kidsanim.api.domain.Location;
import com.kidsanim.api.domain.Series;
import com.kidsanim.api.dto.CreateLocationRequest;
import com.kidsanim.api.dto.LocationResponse;
import com.kidsanim.api.dto.UpdateLocationRequest;
import com.kidsanim.api.infrastructure.exception.ResourceNotFoundException;
import com.kidsanim.api.repository.LocationRepository;
import com.kidsanim.api.repository.SeriesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LocationServiceTest {

    private LocationRepository locationRepository;
    private SeriesRepository seriesRepository;
    private LocationService locationService;

    @BeforeEach
    void setUp() {
        locationRepository = mock(LocationRepository.class);
        seriesRepository = mock(SeriesRepository.class);
        locationService = new LocationService(locationRepository, seriesRepository);
    }

    @Test
    void createLocationAssociatesWithSeries() {
        UUID seriesId = UUID.randomUUID();
        Series series = new Series();
        series.setId(seriesId);

        when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(series));
        when(locationRepository.save(any(Location.class))).thenAnswer(inv -> {
            Location l = inv.getArgument(0);
            l.setId(UUID.randomUUID());
            return l;
        });

        CreateLocationRequest req = new CreateLocationRequest("Huerto", "prompt huerto", null);
        LocationResponse res = locationService.create(seriesId, req);

        assertThat(res.name()).isEqualTo("Huerto");
        assertThat(res.prompt()).isEqualTo("prompt huerto");
        assertThat(res.seriesId()).isEqualTo(seriesId);
    }

    @Test
    void updateLocationModifiesFields() {
        UUID locId = UUID.randomUUID();
        Location loc = new Location(new Series(), "Old", "old prompt");
        loc.setId(locId);

        when(locationRepository.findById(locId)).thenReturn(Optional.of(loc));
        when(locationRepository.save(any(Location.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateLocationRequest req = new UpdateLocationRequest("New", "new prompt", "/storage/loc.png");
        LocationResponse res = locationService.update(locId, req);

        assertThat(res.name()).isEqualTo("New");
        assertThat(res.prompt()).isEqualTo("new prompt");
        assertThat(res.referenceImagePath()).isEqualTo("/storage/loc.png");
    }

    @Test
    void findByIdThrowsWhenNotFound() {
        UUID locId = UUID.randomUUID();
        when(locationRepository.findById(locId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> locationService.findById(locId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
