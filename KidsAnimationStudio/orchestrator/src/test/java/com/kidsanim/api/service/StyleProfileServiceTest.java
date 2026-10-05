package com.kidsanim.api.service;

import com.kidsanim.api.domain.StyleProfile;
import com.kidsanim.api.domain.enums.VideoModel;
import com.kidsanim.api.dto.CreateStyleProfileRequest;
import com.kidsanim.api.dto.StyleProfileResponse;
import com.kidsanim.api.dto.UpdateStyleProfileRequest;
import com.kidsanim.api.infrastructure.exception.ResourceNotFoundException;
import com.kidsanim.api.repository.StyleProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StyleProfileServiceTest {

    private StyleProfileRepository repository;
    private StyleProfileService service;

    @BeforeEach
    void setUp() {
        repository = mock(StyleProfileRepository.class);
        service = new StyleProfileService(repository);
    }

    @Test
    void createProfileUsesDefaults() {
        when(repository.save(any(StyleProfile.class))).thenAnswer(inv -> {
            StyleProfile p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        CreateStyleProfileRequest req = new CreateStyleProfileRequest(
                "3D Style", "ckpt.safetensors", "style", "neg",
                null, null, new BigDecimal("2.00"), null, null, null, null, null
        );

        StyleProfileResponse res = service.create(req);

        assertThat(res.name()).isEqualTo("3D Style");
        assertThat(res.sampler()).isEqualTo("dpmpp_sde");
        assertThat(res.steps()).isEqualTo(8);
        assertThat(res.width()).isEqualTo(1024);
        assertThat(res.height()).isEqualTo(576);
        assertThat(res.videoModel()).isEqualTo(VideoModel.LTX);
    }

    @Test
    void updateProfileModifiesFields() {
        UUID id = UUID.randomUUID();
        StyleProfile profile = new StyleProfile("Old", "ckpt", "style", "neg");
        profile.setId(id);

        when(repository.findById(id)).thenReturn(Optional.of(profile));
        when(repository.save(any(StyleProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateStyleProfileRequest req = new UpdateStyleProfileRequest(
                "New Name", "ckpt2", "style2", "neg2",
                "euler", 12, new BigDecimal("2.50"), 768, 768,
                VideoModel.WAN, 24, "1024x576"
        );

        StyleProfileResponse res = service.update(id, req);

        assertThat(res.name()).isEqualTo("New Name");
        assertThat(res.videoModel()).isEqualTo(VideoModel.WAN);
        assertThat(res.sampler()).isEqualTo("euler");
    }

    @Test
    void findByIdThrowsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
