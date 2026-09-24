package com.trivia.api.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trivia.api.ai.dto.AiGenerationRequest;
import com.trivia.api.ai.dto.AiTriviaItem;
import com.trivia.api.ai.exception.AiClientException;
import com.trivia.api.domain.Dificultad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GeminiTriviaAiClient — Tests unitarios")
class GeminiTriviaAiClientTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private ObjectMapper objectMapper;
    private GeminiTriviaAiClient client;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        client = new GeminiTriviaAiClient(
                "fake-api-key",
                "gemini-3.5-flash-lite",
                30,
                objectMapper,
                httpClient
        );
    }

    @Test
    @DisplayName("Lanza AiClientException si la clave de API está vacía o es nula")
    void debeLanzarExcepcionSiNoHayApiKey() {
        GeminiTriviaAiClient clientSinKey = new GeminiTriviaAiClient(
                "",
                "gemini-3.5-flash-lite",
                30,
                objectMapper,
                httpClient
        );

        AiGenerationRequest req = new AiGenerationRequest(
                "ASTRONOMIA", null, Dificultad.FACIL, "es-MX", 4, 1, List.of()
        );

        assertThatThrownBy(() -> clientSinKey.generate(req))
                .isInstanceOf(AiClientException.class)
                .hasMessageContaining("AI_API_KEY");
    }

    @Test
    @DisplayName("Procesa correctamente una respuesta exitosa de Gemini")
    void debeProcesarRespuestaExitosa() throws IOException, InterruptedException {
        String geminiJson = """
        {
          "candidates": [
            {
              "content": {
                "parts": [
                  {
                    "text": "[{\\"pregunta\\":\\"¿Cuál es el planeta más grande?\\",\\"opciones\\":[{\\"letra\\":\\"A\\",\\"texto\\":\\"Marte\\",\\"correcta\\":false},{\\"letra\\":\\"B\\",\\"texto\\":\\"Júpiter\\",\\"correcta\\":true}],\\"explicacion\\":\\"Júpiter es el mayor planeta del sistema solar.\\"}]"
                  }
                ]
              }
            }
          ]
        }
        """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(geminiJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        AiGenerationRequest req = new AiGenerationRequest(
                "ASTRONOMIA", "Planetas", Dificultad.MEDIA, "es-MX", 2, 1, List.of()
        );

        List<AiTriviaItem> resultado = client.generate(req);

        assertThat(resultado).hasSize(1);
        AiTriviaItem item = resultado.get(0);
        assertThat(item.pregunta()).isEqualTo("¿Cuál es el planeta más grande?");
        assertThat(item.opciones()).hasSize(2);
        assertThat(item.opciones().get(1).letra()).isEqualTo("B");
        assertThat(item.opciones().get(1).correcta()).isTrue();
        assertThat(item.explicacion()).contains("Júpiter");
    }

    @Test
    @DisplayName("Limpia delimitadores de código markdown en la respuesta antes de deserializar")
    void debeLimpiarDelimitadoresMarkdown() throws IOException, InterruptedException {
        String geminiJson = """
        {
          "candidates": [
            {
              "content": {
                "parts": [
                  {
                    "text": "```json\\n[{\\"pregunta\\":\\"¿Cuál es la estrella más cercana?\\",\\"opciones\\":[{\\"letra\\":\\"A\\",\\"texto\\":\\"Sol\\",\\"correcta\\":true}],\\"explicacion\\":\\"El Sol es la estrella más cercana a la Tierra.\\"}]\\n```"
                  }
                ]
              }
            }
          ]
        }
        """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(geminiJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        AiGenerationRequest req = new AiGenerationRequest(
                "ASTRONOMIA", null, Dificultad.FACIL, "es-MX", 1, 1, List.of()
        );

        List<AiTriviaItem> resultado = client.generate(req);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).pregunta()).isEqualTo("¿Cuál es la estrella más cercana?");
    }

    @Test
    @DisplayName("Lanza AiClientException con código HTTP en caso de error 4xx o 5xx del API")
    void debeLanzarExcepcionConErroresHttp() throws IOException, InterruptedException {
        when(httpResponse.statusCode()).thenReturn(429);
        when(httpResponse.body()).thenReturn("{\"error\": {\"message\": \"Resource exhausted\"}}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        AiGenerationRequest req = new AiGenerationRequest(
                "HISTORIA", null, Dificultad.DIFICIL, "es-MX", 4, 2, List.of()
        );

        assertThatThrownBy(() -> client.generate(req))
                .isInstanceOf(AiClientException.class)
                .hasMessageContaining("Resource exhausted")
                .matches(e -> ((AiClientException) e).getStatusCode() == 429);
    }

    @Test
    @DisplayName("Incluye las preguntas existentes en el prompt para evitar repeticiones")
    void debeIncluirPreguntasExistentesEnElPrompt() throws IOException, InterruptedException {
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"[]\"}]}}]}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        List<String> existentes = List.of(
                "¿Quién descubrió América?",
                "¿En qué año cayó el Imperio Romano?"
        );

        AiGenerationRequest req = new AiGenerationRequest(
                "HISTORIA", "Universal", Dificultad.MEDIA, "es-MX", 4, 2, existentes
        );

        client.generate(req);

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());

        HttpRequest sentRequest = captor.getValue();
        assertThat(sentRequest.uri().toString()).contains("key=fake-api-key");
    }
}
