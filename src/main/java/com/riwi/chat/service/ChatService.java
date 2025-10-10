package com.riwi.chat.service;


import com.riwi.chat.model.dto.AI21Request;
import com.riwi.chat.model.dto.AI21Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatService {
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    @Autowired
    private AI21Service ai21Service;

    // Almacén temporal de conversaciones (en producción usar Redis o BD)
    private final ConcurrentHashMap<String, List<AI21Request.Message>> conversationHistory = new ConcurrentHashMap<>();

    // Mensaje del sistema por defecto
    private static final String DEFAULT_SYSTEM_MESSAGE =
            "Eres un asistente virtual útil y amigable. Responde de manera concisa y clara.";

    /**
     * Procesa un mensaje simple del usuario
     * @param userMessage mensaje del usuario
     * @return respuesta del chatbot
     */
    public String processMessage(String userMessage) {
        return processMessage(userMessage, null);
    }

    /**
     * Procesa un mensaje del usuario con ID de sesión
     * @param userMessage mensaje del usuario
     * @param sessionId ID de la sesión (para mantener historial)
     * @return respuesta del chatbot
     */
    public String processMessage(String userMessage, String sessionId) {
        logger.debug("Procesando mensaje: '{}' para sesión: {}", userMessage, sessionId);

        try {
            // Validar entrada
            if (userMessage == null || userMessage.trim().isEmpty()) {
                return "Por favor, envía un mensaje válido.";
            }

            // Verificar configuración
            if (!ai21Service.isConfigured()) {
                return "El servicio no está disponible temporalmente. Intenta más tarde.";
            }

            // Obtener o crear conversación
            List<AI21Request.Message> conversation = getOrCreateConversation(sessionId);

            // Agregar mensaje del usuario
            conversation.add(ai21Service.createUserMessage(userMessage.trim()));

            // Enviar conversación a AI21 Studio
            AI21Response response = ai21Service.sendConversation(conversation);

            // Extraer respuesta
            String assistantMessage = response.getFirstMessageContent();

            if (assistantMessage == null || assistantMessage.trim().isEmpty()) {
                return "No pude generar una respuesta. Intenta reformular tu pregunta.";
            }

            // Agregar respuesta del asistente al historial
            conversation.add(ai21Service.createAssistantMessage(assistantMessage));

            // Limpiar historial si es muy largo (evitar exceder límites de tokens)
            cleanupConversationIfNeeded(conversation);

            logger.info("Mensaje procesado exitosamente para sesión: {}. Tokens usados: {}",
                    sessionId, response.getTotalTokensUsed());

            return assistantMessage;

        } catch (Exception e) {
            logger.error("Error procesando mensaje para sesión {}: {}", sessionId, e.getMessage(), e);
            return handleError(e);
        }
    }

    /**
     * Procesa una conversación completa
     * @param messages lista de mensajes
     * @return respuesta del chatbot
     */
    public String processConversation(List<AI21Request.Message> messages) {
        logger.debug("Procesando conversación con {} mensajes", messages.size());

        try {
            if (messages == null || messages.isEmpty()) {
                return "No se proporcionaron mensajes para procesar.";
            }

            // Agregar mensaje del sistema si no existe
            List<AI21Request.Message> fullConversation = ensureSystemMessage(messages);

            // Enviar a AI21 Studio
            AI21Response response = ai21Service.sendConversation(fullConversation);

            String assistantMessage = response.getFirstMessageContent();

            if (assistantMessage == null || assistantMessage.trim().isEmpty()) {
                return "No pude generar una respuesta para esta conversación.";
            }

            logger.info("Conversación procesada exitosamente. Tokens usados: {}",
                    response.getTotalTokensUsed());

            return assistantMessage;

        } catch (Exception e) {
            logger.error("Error procesando conversación: {}", e.getMessage(), e);
            return handleError(e);
        }
    }

    /**
     * Obtiene el historial de una conversación
     * @param sessionId ID de la sesión
     * @return lista de mensajes del historial
     */
    public List<AI21Request.Message> getConversationHistory(String sessionId) {
        if (sessionId == null) {
            return new ArrayList<>();
        }
        return conversationHistory.getOrDefault(sessionId, new ArrayList<>());
    }

    /**
     * Limpia el historial de una conversación
     * @param sessionId ID de la sesión
     */
    public void clearConversation(String sessionId) {
        if (sessionId != null) {
            conversationHistory.remove(sessionId);
            logger.info("Historial limpiado para sesión: {}", sessionId);
        }
    }

    /**
     * Limpia todos los historiales
     */
    public void clearAllConversations() {
        conversationHistory.clear();
        logger.info("Todos los historiales han sido limpiados");
    }

    /**
     * Obtiene estadísticas del servicio
     * @return información sobre conversaciones activas
     */
    public String getServiceStats() {
        return String.format("Conversaciones activas: %d, Configurado: %s",
                conversationHistory.size(),
                ai21Service.isConfigured() ? "Sí" : "No");
    }

    // Métodos privados

    /**
     * Obtiene una conversación existente o crea una nueva
     */
    private List<AI21Request.Message> getOrCreateConversation(String sessionId) {
        if (sessionId == null) {
            // Conversación temporal sin historial
            List<AI21Request.Message> tempConversation = new ArrayList<>();
            tempConversation.add(ai21Service.createSystemMessage(DEFAULT_SYSTEM_MESSAGE));
            return tempConversation;
        }

        return conversationHistory.computeIfAbsent(sessionId, k -> {
            List<AI21Request.Message> newConversation = new ArrayList<>();
            newConversation.add(ai21Service.createSystemMessage(DEFAULT_SYSTEM_MESSAGE));
            return newConversation;
        });
    }

    /**
     * Asegura que la conversación tenga un mensaje del sistema
     */
    private List<AI21Request.Message> ensureSystemMessage(List<AI21Request.Message> messages) {
        List<AI21Request.Message> result = new ArrayList<>(messages);

        // Verificar si ya tiene mensaje del sistema
        boolean hasSystemMessage = messages.stream()
                .anyMatch(msg -> "system".equals(msg.getRole()));

        if (!hasSystemMessage) {
            result.add(0, ai21Service.createSystemMessage(DEFAULT_SYSTEM_MESSAGE));
        }

        return result;
    }

    /**
     * Limpia la conversación si es muy larga
     */
    private void cleanupConversationIfNeeded(List<AI21Request.Message> conversation) {
        final int MAX_MESSAGES = 20; // Mantener máximo 20 mensajes

        if (conversation.size() > MAX_MESSAGES) {
            // Mantener el primer mensaje (sistema) y los últimos mensajes
            AI21Request.Message systemMessage = conversation.get(0);
            List<AI21Request.Message> recentMessages = conversation.subList(
                    conversation.size() - (MAX_MESSAGES - 1),
                    conversation.size()
            );

            conversation.clear();
            conversation.add(systemMessage);
            conversation.addAll(recentMessages);

            logger.debug("Historial limpiado, manteniendo {} mensajes", conversation.size());
        }
    }


    /**
     * Maneja errores y devuelve mensajes amigables al usuario
     */
    private String handleError(Exception e) {
        String errorMessage = e.getMessage();

        if (errorMessage.contains("API key")) {
            return "Error de configuración. Contacta al administrador.";
        }

        if (errorMessage.contains("timeout") || errorMessage.contains("conexión")) {
            return "El servicio está temporalmente lento. Intenta de nuevo en unos segundos.";
        }

        if (errorMessage.contains("rate limit")) {
            return "Demasiadas solicitudes. Espera un momento antes de intentar de nuevo.";
        }

        if (errorMessage.contains("quota") || errorMessage.contains("usage")) {
            return "Servicio temporalmente no disponible. Intenta más tarde.";
        }

        // Error genérico
        return "Ocurrió un error inesperado. Por favor intenta de nuevo.";
    }

}
