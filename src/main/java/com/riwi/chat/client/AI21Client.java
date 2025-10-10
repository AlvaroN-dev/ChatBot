package com.riwi.chat.client;

import com.riwi.chat.client.exception.AI21ClientException;
import com.riwi.chat.config.ApiConfig;
import com.riwi.chat.model.dto.AI21Request;
import com.riwi.chat.model.dto.AI21Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * Cliente HTTP para comunicación con AI21 Studio API
 * Responsable únicamente de hacer las peticiones HTTP y mapear respuestas
 * No contiene lógica de negocio
 */
@Component
public class AI21Client {
    private static final Logger logger = LoggerFactory.getLogger(AI21Client.class);

    @Autowired
    private ApiConfig apiConfig;

    @Autowired
    @Qualifier("ai21RestTemplate")
    private RestTemplate restTemplate;

    /**
     * Envía una petición POST a AI21 Studio Chat Completions API
     *
     * @param request objeto con los parámetros de la petición
     * @return respuesta de AI21 Studio
     * @throws AI21ClientException si hay error en la comunicación
     */
    public AI21Response sendChatRequest(AI21Request request) {
        logger.debug("Enviando request a AI21 Studio API: model={}, messages={}",
                request.getModel(),
                request.getMessages().size());

        try {
            // Crear headers con autenticación
            HttpHeaders headers = buildHeaders();

            // Crear entity con request y headers
            HttpEntity<AI21Request> entity = new HttpEntity<>(request, headers);

            // Realizar petición HTTP POST
            ResponseEntity<AI21Response> response = restTemplate.exchange(
                    apiConfig.getAi21().getApi().getUrl(),
                    HttpMethod.POST,
                    entity,
                    AI21Response.class
            );

            // Validar que la respuesta no sea nula
            if (response.getBody() == null) {
                throw new AI21ClientException("Respuesta vacía de AI21 Studio", null);
            }

            logger.info("✅ Petición exitosa a AI21 Studio. Status: {}, Tokens: {}",
                    response.getStatusCode(),
                    response.getBody().getTotalTokensUsed());

            return response.getBody();

        } catch (HttpClientErrorException e) {
            // Errores 4xx (cliente)
            logger.error("❌ Error 4xx en AI21 Studio: {} - {}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString());

            throw new AI21ClientException(
                    String.format("Error del cliente [%s]: %s", e.getStatusCode(), parseErrorMessage(e)),
                    e,
                    e.getStatusCode().value()
            );

        } catch (HttpServerErrorException e) {
            // Errores 5xx (servidor)
            logger.error("❌ Error 5xx en AI21 Studio: {} - {}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString());

            throw new AI21ClientException(
                    String.format("Error del servidor AI21 [%s]: Servicio temporalmente no disponible", e.getStatusCode()),
                    e,
                    e.getStatusCode().value()
            );

        } catch (ResourceAccessException e) {
            // Errores de timeout o conexión
            logger.error("❌ Error de conexión con AI21 Studio: {}", e.getMessage());

            throw new AI21ClientException(
                    "No se pudo conectar con AI21 Studio: Timeout o error de red",
                    e,
                    HttpStatus.REQUEST_TIMEOUT.value()
            );

        } catch (AI21ClientException e) {
            // Re-lanzar nuestras propias excepciones
            throw e;

        } catch (Exception e) {
            // Cualquier otro error inesperado
            logger.error("❌ Error inesperado en cliente AI21: {}", e.getMessage(), e);

            throw new AI21ClientException(
                    "Error inesperado al comunicarse con AI21 Studio: " + e.getMessage(),
                    e,
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
        }
    }

    /**
     * Construye los headers necesarios para la petición a AI21 Studio
     *
     * @return HttpHeaders configurados con autenticación y content-type
     */
    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);

        // Autenticación Bearer Token
        String apiKey = apiConfig.getAi21().getApi().getKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new AI21ClientException("API Key de AI21 Studio no configurada", null);
        }
        headers.setBearerAuth(apiKey);

        // User-Agent personalizado
        headers.set("User-Agent", "Riwi-ChatBot/1.0");

        return headers;
    }

    /**
     * Extrae mensaje de error legible del cuerpo de respuesta
     *
     * @param e excepción HTTP
     * @return mensaje de error parseado
     */
    private String parseErrorMessage(HttpClientErrorException e) {
        try {
            String responseBody = e.getResponseBodyAsString();

            // Aquí podrías parsear JSON si AI21 devuelve errores estructurados
            // Por ahora retornamos el body completo
            if (responseBody != null && !responseBody.isEmpty()) {
                return responseBody.length() > 200
                        ? responseBody.substring(0, 200) + "..."
                        : responseBody;
            }

            return "Sin detalles adicionales";

        } catch (Exception ex) {
            return "Error al parsear respuesta de error";
        }
    }

    /**
     * Verifica si el cliente está correctamente configurado
     *
     * @return true si la configuración es válida
     */
    public boolean isConfigured() {
        try {
            String apiKey = apiConfig.getAi21().getApi().getKey();
            String apiUrl = apiConfig.getAi21().getApi().getUrl();
            String model = apiConfig.getAi21().getApi().getModel();

            boolean configured = apiKey != null && !apiKey.trim().isEmpty() &&
                    apiUrl != null && !apiUrl.trim().isEmpty() &&
                    model != null && !model.trim().isEmpty();

            if (!configured) {
                logger.warn("⚠️ AI21Client no está completamente configurado");
            }

            return configured;

        } catch (Exception e) {
            logger.error("❌ Error verificando configuración de AI21Client: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene información de configuración (sin exponer la API key)
     *
     * @return String con información de configuración
     */
    public String getConfigInfo() {
        return String.format("AI21Client[url=%s, model=%s, configured=%s]",
                apiConfig.getAi21().getApi().getUrl(),
                apiConfig.getAi21().getApi().getModel(),
                isConfigured());
    }

    /**
     * Valida un request antes de enviarlo
     *
     * @param request request a validar
     * @throws AI21ClientException si el request es inválido
     */
    public void validateRequest(AI21Request request) {
        if (request == null) {
            throw new AI21ClientException("Request no puede ser null", null);
        }

        if (request.getModel() == null || request.getModel().trim().isEmpty()) {
            throw new AI21ClientException("Model es requerido en el request", null);
        }

        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            throw new AI21ClientException("Messages no puede estar vacío", null);
        }

        // Validar que los mensajes tengan role y content
        for (AI21Request.Message msg : request.getMessages()) {
            if (msg.getRole() == null || msg.getRole().trim().isEmpty()) {
                throw new AI21ClientException("Cada mensaje debe tener un role", null);
            }
            if (msg.getContent() == null || msg.getContent().trim().isEmpty()) {
                throw new AI21ClientException("Cada mensaje debe tener contenido", null);
            }
        }
    }
}