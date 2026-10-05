package com.kidsanim.api.gateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.kidsanim.api.infrastructure.config.ExternalServicesProperties;
import com.kidsanim.api.infrastructure.exception.AiGatewayException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiGatewayClientIntegrationTest {

    private static WireMockServer wireMockServer;
    private AiGatewayClient aiGatewayClient;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
        ExternalServicesProperties properties = new ExternalServicesProperties(
                wireMockServer.baseUrl(),
                "http://localhost:8188",
                "http://localhost:11434",
                2500
        );
        var httpClient = java.net.http.HttpClient.newBuilder()
                .version(java.net.http.HttpClient.Version.HTTP_1_1)
                .build();
        var clientBuilder = RestClient.builder()
                .requestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory(httpClient));
        aiGatewayClient = new AiGatewayClient(properties, clientBuilder);
    }

    @Test
    void generateCharacterSheetSuccess() {
        wireMockServer.stubFor(post(urlEqualTo("/image/character-sheet"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "images": ["/storage/cand1.png", "/storage/cand2.png"],
                                  "seeds": [101, 102]
                                }
                                """)
                        .withStatus(200)));

        AiGatewayClient.SheetAiRequest req = new AiGatewayClient.SheetAiRequest(
                "un zorrito", "3D cartoon", "scary", 42L, 1024, 1024, "/storage", 2
        );

        AiGatewayClient.SheetAiResponse res = aiGatewayClient.generateCharacterSheet(req);

        assertThat(res).isNotNull();
        assertThat(res.images()).hasSize(2);
        assertThat(res.images().get(0)).isEqualTo("/storage/cand1.png");
        assertThat(res.seeds()).containsExactly(101L, 102L);

        wireMockServer.verify(postRequestedFor(urlEqualTo("/image/character-sheet")));
    }

    @Test
    void generateCharacterSheetGatewayErrorThrowsAiGatewayException() {
        wireMockServer.stubFor(post(urlEqualTo("/image/character-sheet"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("{\"detail\":\"ComfyUI OOM or unreachable\"}")));

        AiGatewayClient.SheetAiRequest req = new AiGatewayClient.SheetAiRequest(
                "un zorrito", "3D cartoon", "scary", 42L, 1024, 1024, "/storage", 2
        );

        assertThatThrownBy(() -> aiGatewayClient.generateCharacterSheet(req))
                .isInstanceOf(AiGatewayException.class)
                .hasMessageContaining("500");
    }

    @Test
    void generateTtsSuccess() {
        wireMockServer.stubFor(post(urlEqualTo("/tts"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "audioPath": "/storage/audio.wav",
                                  "durationMs": 3500
                                }
                                """)
                        .withStatus(200)));

        AiGatewayClient.TtsAiRequest req = new AiGatewayClient.TtsAiRequest(
                "Hola amiguitos", "ef_dora", "-12%", "default", "/storage/out.wav"
        );

        AiGatewayClient.TtsAiResponse res = aiGatewayClient.generateTts(req);

        assertThat(res.audioPath()).isEqualTo("/storage/audio.wav");
        assertThat(res.durationMs()).isEqualTo(3500);
    }

    @Test
    void alignAudioSuccess() {
        wireMockServer.stubFor(post(urlEqualTo("/align"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "words": [
                                    {"word": "hola", "startMs": 0, "endMs": 400},
                                    {"word": "amigos", "startMs": 450, "endMs": 1100}
                                  ]
                                }
                                """)
                        .withStatus(200)));

        AiGatewayClient.AlignAiRequest req = new AiGatewayClient.AlignAiRequest(
                "/storage/audio.wav", "hola amigos", "es"
        );

        AiGatewayClient.AlignAiResponse res = aiGatewayClient.alignAudio(req);

        assertThat(res.words()).hasSize(2);
        assertThat(res.words().get(0).word()).isEqualTo("hola");
        assertThat(res.words().get(0).endMs()).isEqualTo(400);
        assertThat(res.words().get(1).word()).isEqualTo("amigos");
    }

    @Test
    void generateKeyframeSuccess() {
        wireMockServer.stubFor(post(urlEqualTo("/image/keyframe"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "imagePath": "/storage/kf_1.png",
                                  "seed": 999
                                }
                                """)
                        .withStatus(200)));

        AiGatewayClient.KeyframeAiRequest req = new AiGatewayClient.KeyframeAiRequest(
                "Tito en el bosque", "scary", List.of(), 999L, 1024, 576, "/storage/kf_1.png"
        );

        AiGatewayClient.KeyframeAiResponse res = aiGatewayClient.generateKeyframe(req);

        assertThat(res.imagePath()).isEqualTo("/storage/kf_1.png");
        assertThat(res.seed()).isEqualTo(999L);
    }

    @Test
    void calculateSimilaritySuccess() {
        wireMockServer.stubFor(post(urlEqualTo("/qa/similarity"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "score": 0.885
                                }
                                """)
                        .withStatus(200)));

        AiGatewayClient.SimilarityAiRequest req = new AiGatewayClient.SimilarityAiRequest(
                "/storage/kf.png", "/storage/ref.png"
        );

        AiGatewayClient.SimilarityAiResponse res = aiGatewayClient.calculateSimilarity(req);

        assertThat(res.score()).isEqualTo(0.885);
    }

    @Test
    void generateVideoI2VSuccess() {
        wireMockServer.stubFor(post(urlEqualTo("/video/i2v"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "clipPath": "/storage/clip_1.mp4",
                                  "lastFramePath": "/storage/last_1.png",
                                  "frames": 90,
                                  "durationMs": 3000
                                }
                                """)
                        .withStatus(200)));

        AiGatewayClient.VideoI2VAiRequest req = new AiGatewayClient.VideoI2VAiRequest(
                "LTX", "/storage/img.png", "caminar feliz", "bad quality",
                3000, 30, 1024, 576, 1234L, "/storage/out.mp4"
        );

        AiGatewayClient.VideoI2VAiResponse res = aiGatewayClient.generateVideoI2V(req);

        assertThat(res.clipPath()).isEqualTo("/storage/clip_1.mp4");
        assertThat(res.lastFramePath()).isEqualTo("/storage/last_1.png");
        assertThat(res.frames()).isEqualTo(90);
        assertThat(res.durationMs()).isEqualTo(3000);
    }

    @Test
    void freeGpuSuccess() {
        wireMockServer.stubFor(post(urlEqualTo("/gpu/free"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"freedMb\": 4200}")
                        .withStatus(200)));

        AiGatewayClient.GpuFreeResponse res = aiGatewayClient.freeGpu();

        assertThat(res.freedMb()).isEqualTo(4200);
    }
}
