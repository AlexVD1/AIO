package com.storyvideo.api.audio.narration;

import com.storyvideo.api.audio.narration.dto.NarrationRequest;
import com.storyvideo.api.audio.narration.dto.NarrationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service("noOpNarrationService")
public class NoOpNarrationService implements NarrationService {

    private static final Logger log = LoggerFactory.getLogger(NoOpNarrationService.class);

    // Cabecera mínima representativa de un frame de audio MP3
    private static final byte[] DUMMY_MP3_BYTES = new byte[]{
            (byte) 0xFF, (byte) 0xFB, (byte) 0x90, (byte) 0x64, 0x00, 0x00, 0x00, 0x00
    };

    @Override
    public String getProviderName() {
        return "No-Op Narration (Offline Simulated Duration)";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public NarrationResult synthesize(NarrationRequest request) {
        String text = request.text() != null ? request.text().trim() : "";
        int wordCount = text.isEmpty() ? 0 : text.split("\\s+").length;

        // Tasa promedio de locución en español: ~2.5 palabras por segundo + 1 segundo de pausa
        double estimatedDuration = Math.max(3.0, (wordCount / 2.5) + 1.0);
        log.info("NoOpNarrationService: Simulación de locución para escena {} ({} palabras -> {} seg)",
                request.sequenceNumber(), wordCount, estimatedDuration);

        return NarrationResult.mp3(DUMMY_MP3_BYTES, estimatedDuration, "simulated-voice");
    }
}
