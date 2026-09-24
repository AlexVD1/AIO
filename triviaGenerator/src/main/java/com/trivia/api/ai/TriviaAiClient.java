package com.trivia.api.ai;

import com.trivia.api.ai.dto.AiGenerationRequest;
import com.trivia.api.ai.dto.AiTriviaItem;

import java.util.List;

/**
 * Puerto / Contrato para clientes de Inteligencia Artificial que generan contenido de trivia.
 *
 * Esta abstracción desacopla la lógica de negocio del proveedor de LLM específico
 * (Google Gemini, OpenAI, Anthropic, etc.), permitiendo intercambiar o simular
 * la IA fácilmente en pruebas unitarias y de integración.
 */
public interface TriviaAiClient {

    /**
     * Genera una lista de trivias según los parámetros solicitados.
     *
     * @param request Parámetros de generación (tema, dificultad, idioma, cantidad, contexto)
     * @return Lista de trivias generadas con sus opciones y explicaciones
     * @throws com.trivia.api.ai.exception.AiClientException si ocurre un error con el proveedor
     */
    List<AiTriviaItem> generate(AiGenerationRequest request);
}
