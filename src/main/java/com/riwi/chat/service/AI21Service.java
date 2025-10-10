package com.riwi.chat.service;



import com.riwi.chat.client.AI21Client;
import com.riwi.chat.client.exception.AI21ClientException;
import com.riwi.chat.config.ApiConfig;
import com.riwi.chat.model.dto.AI21Request;
import com.riwi.chat.model.dto.AI21Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de alto nivel para interactuar con AI21 Studio
 * Contiene lógica de negocio y validaciones
 * Delega la comunicación HTTP al AI21Client
 */
@Service
public class AI21Service {
    private static final Logger logger = LoggerFactory.getLogger(AI21Service.class);

    @Autowired
    private AI21Client ai21Client;

    @Autowired
    private ApiConfig apiConfig;

    /**
     * Envía un mensaje simple a AI21 Studio y obtiene la respuesta
     *
     * @param userMessage mensaje del usuario
     * @return respuesta de AI21 Studio
     * @throws RuntimeException si hay error en la comunicación
     */
    public AI21Response sendMessage(String userMessage) {
        logger.debug("Enviando mensaje simple a AI21 Studio");

        // Validar entrada
        validateUserMessage(userMessage);

        try {
            // Crear request con mensaje único
            AI21Request request = buildAI21Request(userMessage);

            // Validar request antes de enviar
            ai21Client.validateRequest(request);

            // Enviar a través del cliente
            AI21Response response = ai21Client.sendChatRequest(request);

            // Validar respuesta
            validateResponse(response);

            logger.info("✅ Mensaje procesado exitosamente. Tokens usados: {}",
                    response.getTotalTokensUsed());

            return response;

        } catch (AI21ClientException e) {
            logger.error("❌ Error del cliente AI21: {}", e.getMessage());
            throw new RuntimeException(e.getUserFriendlyMessage(), e);

        } catch (Exception e) {
            logger.error("❌ Error inesperado procesando mensaje: {}", e.getMessage(), e);
            throw new RuntimeException("Error interno al procesar el mensaje", e);
        }
    }

    /**
     * Envía una conversación completa a AI21 Studio
     *
     * @param messages lista de mensajes de la conversación
     * @return respuesta de AI21 Studio
     * @throws RuntimeException si hay error en la comunicación
     */
    public AI21Response sendConversation(List<AI21Request.Message> messages) {
        logger.debug("Enviando conversación con {} mensajes", messages != null ? messages.size() : 0);

        // Validar entrada
        validateConversation(messages);

        try {
            // Crear request con múltiples mensajes
            AI21Request request = buildAI21Request(messages);

            // Validar request
            ai21Client.validateRequest(request);

            // Enviar a través del cliente
            AI21Response response = ai21Client.sendChatRequest(request);

            // Validar respuesta
            validateResponse(response);

            logger.info("✅ Conversación procesada exitosamente. Tokens usados: {}",
                    response.getTotalTokensUsed());

            return response;

        } catch (AI21ClientException e) {
            logger.error("❌ Error del cliente AI21: {}", e.getMessage());
            throw new RuntimeException(e.getUserFriendlyMessage(), e);

        } catch (Exception e) {
            logger.error("❌ Error inesperado procesando conversación: {}", e.getMessage(), e);
            throw new RuntimeException("Error interno al procesar la conversación", e);
        }
    }

    // ========== MÉTODOS UTILITARIOS PARA CREAR MENSAJES ==========

    /**
     * Crea un mensaje del sistema
     *
     * @param content contenido del mensaje
     * @return mensaje con role "system"
     */
    public AI21Request.Message createSystemMessage(String content) {
        validateMessageContent(content, "system");
        return new AI21Request.Message("system", content);
    }

    /**
     * Crea un mensaje del usuario
     *
     * @param content contenido del mensaje
     * @return mensaje con role "user"
     */
    public AI21Request.Message createUserMessage(String content) {
        validateMessageContent(content, "user");
        return new AI21Request.Message("user", content);
    }

    /**
     * Crea un mensaje del asistente
     *
     * @param content contenido del mensaje
     * @return mensaje con role "assistant"
     */
    public AI21Request.Message createAssistantMessage(String content) {
        validateMessageContent(content, "assistant");
        return new AI21Request.Message("assistant", content);
    }

    // ========== MÉTODOS DE CONSTRUCCIÓN DE REQUESTS ==========

    /**
     * Construye un request para AI21 Studio con un mensaje simple
     *
     * @param userMessage mensaje del usuario
     * @return AI21Request configurado
     */
    private AI21Request buildAI21Request(String userMessage) {
        List<AI21Request.Message> messages = new ArrayList<>();
        messages.add(new AI21Request.Message("user", userMessage));
        return buildAI21Request(messages);
    }

    /**
     * Construye un request para AI21 Studio con múltiples mensajes
     *
     * @param messages lista de mensajes
     * @return AI21Request configurado con parámetros de ApiConfig
     */
    private AI21Request buildAI21Request(List<AI21Request.Message> messages) {
        return new AI21Request.Builder()
                .model(apiConfig.getAi21().getApi().getModel())
                .messages(messages)
                .maxTokens(apiConfig.getAi21().getApi().getMaxTokens())
                .temperature(apiConfig.getAi21().getApi().getTemperature())
                .topP(0.9)
                .n(1)
                .build();
    }

    // ========== MÉTODOS DE VALIDACIÓN ==========

    /**
     * Valida un mensaje de usuario
     *
     * @param userMessage mensaje a validar
     * @throws IllegalArgumentException si el mensaje es inválido
     */
    private void validateUserMessage(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("El mensaje del usuario no puede estar vacío");
        }

        if (userMessage.length() > 10000) {
            throw new IllegalArgumentException("El mensaje es demasiado largo (máximo 10,000 caracteres)");
        }
    }

    /**
     * Valida una conversación completa
     *
     * @param messages lista de mensajes a validar
     * @throws IllegalArgumentException si la conversación es inválida
     */
    private void validateConversation(List<AI21Request.Message> messages) {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("La conversación debe tener al menos un mensaje");
        }

        if (messages.size() > 50) {
            throw new IllegalArgumentException("La conversación tiene demasiados mensajes (máximo 50)");
        }

        // Validar cada mensaje
        for (AI21Request.Message msg : messages) {
            if (msg == null) {
                throw new IllegalArgumentException("La conversación contiene mensajes nulos");
            }
            if (msg.getRole() == null || msg.getRole().trim().isEmpty()) {
                throw new IllegalArgumentException("Todos los mensajes deben tener un role");
            }
            if (msg.getContent() == null || msg.getContent().trim().isEmpty()) {
                throw new IllegalArgumentException("Todos los mensajes deben tener contenido");
            }
        }
    }

    /**
     * Valida el contenido de un mensaje individual
     *
     * @param content contenido a validar
     * @param role role del mensaje (para logging)
     * @throws IllegalArgumentException si el contenido es inválido
     */
    private void validateMessageContent(String content, String role) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("El contenido del mensaje '%s' no puede estar vacío", role)
            );
        }
    }

    /**
     * Valida que la respuesta de AI21 Studio sea correcta
     *
     * @param response respuesta a validar
     * @throws RuntimeException si la respuesta es inválida
     */
    private void validateResponse(AI21Response response) {
        if (response == null) {
            throw new RuntimeException("Respuesta nula de AI21 Studio");
        }

        if (response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new RuntimeException("AI21 Studio no devolvió opciones de respuesta");
        }

        String content = response.getFirstMessageContent();
        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("AI21 Studio devolvió respuesta vacía");
        }

        // Warning si no completó exitosamente
        if (!response.isCompletedSuccessfully()) {
            logger.warn("⚠️ AI21 Studio no completó exitosamente. Reason: {}",
                    response.getChoices().get(0).getFinishReason());
        }
    }

    // ========== MÉTODOS DE ESTADO Y CONFIGURACIÓN ==========

    /**
     * Verifica que el servicio esté configurado correctamente
     *
     * @return true si el servicio está listo para usarse
     */
    public boolean isConfigured() {
        try {
            boolean clientConfigured = ai21Client.isConfigured();

            if (!clientConfigured) {
                logger.warn("⚠️ AI21Service no está configurado correctamente");
            }

            return clientConfigured;

        } catch (Exception e) {
            logger.error("❌ Error verificando configuración: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene información sobre la configuración actual
     *
     * @return String con detalles de configuración
     */
    public String getServiceInfo() {
        return String.format("AI21Service[client=%s, maxTokens=%d, temperature=%.2f]",
                ai21Client.getConfigInfo(),
                apiConfig.getAi21().getApi().getMaxTokens(),
                apiConfig.getAi21().getApi().getTemperature());
    }

    /**
     * Obtiene estadísticas básicas del servicio
     *
     * @return mapa con estadísticas
     */
    public String getStats() {
        return String.format("Configurado: %s, Model: %s",
                isConfigured() ? "Sí" : "No",
                apiConfig.getAi21().getApi().getModel());
    }
}