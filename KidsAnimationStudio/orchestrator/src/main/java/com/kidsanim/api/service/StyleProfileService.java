package com.kidsanim.api.service;

import com.kidsanim.api.domain.StyleProfile;
import com.kidsanim.api.dto.CreateStyleProfileRequest;
import com.kidsanim.api.dto.StyleProfileResponse;
import com.kidsanim.api.dto.UpdateStyleProfileRequest;
import com.kidsanim.api.infrastructure.exception.ResourceNotFoundException;
import com.kidsanim.api.repository.StyleProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class StyleProfileService {

    private final StyleProfileRepository styleProfileRepository;

    public StyleProfileService(StyleProfileRepository styleProfileRepository) {
        this.styleProfileRepository = styleProfileRepository;
    }

    @Transactional(readOnly = true)
    public List<StyleProfileResponse> findAll() {
        return styleProfileRepository.findAll().stream()
                .map(StyleProfileResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public StyleProfileResponse findById(UUID id) {
        return StyleProfileResponse.fromEntity(getEntityById(id));
    }

    @Transactional(readOnly = true)
    public StyleProfile getEntityById(UUID id) {
        return styleProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StyleProfile no encontrado con ID: " + id));
    }

    public StyleProfileResponse create(CreateStyleProfileRequest request) {
        StyleProfile profile = new StyleProfile();
        profile.setName(request.name());
        profile.setCheckpointName(request.checkpointName());
        profile.setStylePrompt(request.stylePrompt());
        profile.setNegativePrompt(request.negativePrompt());
        profile.setSampler(request.sampler());
        profile.setSteps(request.steps());
        profile.setCfg(request.cfg());
        profile.setWidth(request.width());
        profile.setHeight(request.height());
        profile.setVideoModel(request.videoModel());
        profile.setVideoFps(request.videoFps());
        profile.setVideoResolution(request.videoResolution());

        StyleProfile saved = styleProfileRepository.save(profile);
        return StyleProfileResponse.fromEntity(saved);
    }

    public StyleProfileResponse update(UUID id, UpdateStyleProfileRequest request) {
        StyleProfile profile = getEntityById(id);
        profile.setName(request.name());
        profile.setCheckpointName(request.checkpointName());
        profile.setStylePrompt(request.stylePrompt());
        profile.setNegativePrompt(request.negativePrompt());
        if (request.sampler() != null) profile.setSampler(request.sampler());
        if (request.steps() != null) profile.setSteps(request.steps());
        if (request.cfg() != null) profile.setCfg(request.cfg());
        if (request.width() != null) profile.setWidth(request.width());
        if (request.height() != null) profile.setHeight(request.height());
        if (request.videoModel() != null) profile.setVideoModel(request.videoModel());
        if (request.videoFps() != null) profile.setVideoFps(request.videoFps());
        if (request.videoResolution() != null) profile.setVideoResolution(request.videoResolution());

        StyleProfile updated = styleProfileRepository.save(profile);
        return StyleProfileResponse.fromEntity(updated);
    }

    public void delete(UUID id) {
        StyleProfile profile = getEntityById(id);
        styleProfileRepository.delete(profile);
    }
}
