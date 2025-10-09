package com.riwi.chat.controller;

import com.riwi.chat.model.dto.AI21Request;
import com.riwi.chat.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/chat")
@Validated
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
public class ChatController {
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private ChatService chatService;

    /**
     * Endpoint principal para enviar mensajes al chatbot
     * POST /api/chat/message
     *
     * @param request objeto con el mensaje y sessionId opcional
     * @return respuesta del chatbot con metadata
     */
    @PostMapping("/message")
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody @Valid MessageRequest request) {
        logger.info("Recibido mensaje: '{}' para sesión: {}",
                request.getMessage(),
                request.getSessionId() != null ? request.getSessionId() : "sin-sesion");

        try {
            // Procesar mensaje
            String response = chatService.processMessage(request.getMessage(), request.getSessionId());

            // Crear respuesta exitosa
            Map<String, Object> responseBody = createSuccessResponse(response, request.getSessionId());

            return ResponseEntity.ok(responseBody);

        } catch (IllegalArgumentException e) {
            logger.warn("Petición inválida: {}", e.getMessage());
            return createErrorResponse("Petición inválida: " + e.getMessage(), HttpStatus.BAD_REQUEST);

        } catch (Exception e) {
            logger.error("Error procesando mensaje: {}", e.getMessage(), e);
            return createErrorResponse("Error interno del servidor. Intenta nuevamente.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Endpoint para enviar conversaciones completas
     * POST /api/chat/conversation
     *
     * @param request objeto con lista de mensajes
     * @return respuesta del chatbot
     */
    @PostMapping("/conversation")
    public ResponseEntity<Map<String, Object>> sendConversation(@RequestBody @Valid ConversationRequest request) {
        logger.info("Recibida conversación con {} mensajes", request.getMessages().size());

        try {
            // Validar que la conversación no esté vacía
            if (request.getMessages().isEmpty()) {
                return createErrorResponse("La conversación debe tener al menos un mensaje", HttpStatus.BAD_REQUEST);
            }

            // Procesar conversación
            String response = chatService.processConversation(request.getMessages());

            // Crear respuesta exitosa
            Map<String, Object> responseBody = createSuccessResponse(response, null);
            responseBody.put("processedMessages", request.getMessages().size());

            return ResponseEntity.ok(responseBody);

        } catch (IllegalArgumentException e) {
            logger.warn("Conversación inválida: {}", e.getMessage());
            return createErrorResponse("Conversación inválida: " + e.getMessage(), HttpStatus.BAD_REQUEST);

        } catch (Exception e) {
            logger.error("Error procesando conversación: {}", e.getMessage(), e);
            return createErrorResponse("Error interno del servidor. Intenta nuevamente.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Obtiene el historial de una conversación
     * GET /api/chat/history/{sessionId}
     *
     * @param sessionId ID de la sesión
     * @return historial de mensajes de la sesión
     */
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<Map<String, Object>> getHistory(@PathVariable @NotBlank String sessionId) {
        logger.debug("Solicitado historial para sesión: {}", sessionId);

        try {
            List<AI21Request.Message> history = chatService.getConversationHistory(sessionId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("sessionId", sessionId);
            response.put("messages", history);
            response.put("messageCount", history.size());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error obteniendo historial para sesión {}: {}", sessionId, e.getMessage(), e);
            return createErrorResponse("Error obteniendo historial", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Limpia el historial de una conversación
     * DELETE /api/chat/history/{sessionId}
     *
     * @param sessionId ID de la sesión a limpiar
     * @return confirmación de limpieza
     */
    @DeleteMapping("/history/{sessionId}")
    public ResponseEntity<Map<String, Object>> clearHistory(@PathVariable @NotBlank String sessionId) {
        logger.info("Limpiando historial para sesión: {}", sessionId);

        try {
            chatService.clearConversation(sessionId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Historial limpiado exitosamente");
            response.put("sessionId", sessionId);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error limpiando historial para sesión {}: {}", sessionId, e.getMessage(), e);
            return createErrorResponse("Error limpiando historial", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Genera un nuevo ID de sesión
     * GET /api/chat/session/new
     *
     * @return nuevo sessionId generado
     */
    @GetMapping("/session/new")
    public ResponseEntity<Map<String, Object>> createNewSession() {
        String newSessionId = UUID.randomUUID().toString();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("sessionId", newSessionId);
        response.put("message", "Nueva sesión creada");
        response.put("timestamp", LocalDateTime.now());

        logger.info("Nueva sesión creada: {}", newSessionId);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint de health check para verificar el estado del servicio
     * GET /api/chat/health
     *
     * @return estado del servicio y estadísticas
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            String stats = chatService.getServiceStats();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "UP");
            response.put("service", "ChatBot Service - AI21 Studio");
            response.put("timestamp", LocalDateTime.now());
            response.put("stats", stats);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error en health check: {}", e.getMessage(), e);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "DOWN");
            response.put("service", "ChatBot Service - AI21 Studio");
            response.put("error", e.getMessage());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }

    /**
     * Endpoint para limpiar todas las conversaciones (operación administrativa)
     * DELETE /api/chat/admin/clear-all
     *
     * @return confirmación de limpieza total
     */
    @DeleteMapping("/admin/clear-all")
    public ResponseEntity<Map<String, Object>> clearAllConversations() {
        logger.warn("⚠️ Limpiando TODAS las conversaciones - operación de administrador");

        try {
            chatService.clearAllConversations();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Todas las conversaciones han sido limpiadas");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error limpiando todas las conversaciones: {}", e.getMessage(), e);
            return createErrorResponse("Error en operación de limpieza", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ========== MÉTODOS PRIVADOS UTILITARIOS ==========

    /**
     * Crea una respuesta exitosa estándar
     *
     * @param message contenido de la respuesta
     * @param sessionId ID de la sesión (opcional)
     * @return mapa con la respuesta estructurada
     */
    private Map<String, Object> createSuccessResponse(String message, String sessionId) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now());

        if (sessionId != null && !sessionId.trim().isEmpty()) {
            response.put("sessionId", sessionId);
        }

        return response;
    }

    /**
     * Crea una respuesta de error estándar
     *
     * @param errorMessage mensaje descriptivo del error
     * @param status código HTTP de status
     * @return ResponseEntity con el error estructurado
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(String errorMessage, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", errorMessage);
        response.put("timestamp", LocalDateTime.now());
        response.put("status", status.value());

        return ResponseEntity.status(status).body(response);
    }

    // ========== CLASES INTERNAS PARA DTOs ==========

    /**
     * DTO para request de mensaje simple
     */
    public static class MessageRequest {
        @NotBlank(message = "El mensaje no puede estar vacío")
        private String message;

        private String sessionId; // Opcional - si no se proporciona, se crea conversación temporal

        // Constructores
        public MessageRequest() {}

        public MessageRequest(String message, String sessionId) {
            this.message = message;
            this.sessionId = sessionId;
        }

        // Getters y Setters
        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        @Override
        public String toString() {
            return "MessageRequest{" +
                    "message='" + message + '\'' +
                    ", sessionId='" + sessionId + '\'' +
                    '}';
        }
    }

    /**
     * DTO para request de conversación completa
     */
    public static class ConversationRequest {
        @NotEmpty(message = "La conversación debe tener al menos un mensaje")
        private List<AI21Request.Message> messages;

        // Constructores
        public ConversationRequest() {}

        public ConversationRequest(List<AI21Request.Message> messages) {
            this.messages = messages;
        }

        // Getters y Setters
        public List<AI21Request.Message> getMessages() {
            return messages;
        }

        public void setMessages(List<AI21Request.Message> messages) {
            this.messages = messages;
        }

        @Override
        public String toString() {
            return "ConversationRequest{" +
                    "messages=" + messages +
                    '}';
        }
    }

    // ========== MANEJO GLOBAL DE ERRORES DE VALIDACIÓN ==========

    /**
     * Maneja errores de validación de constraints
     */
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(jakarta.validation.ConstraintViolationException e) {
        logger.error("Error de validación: {}", e.getMessage());
        return createErrorResponse("Datos de entrada inválidos: " + e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Maneja errores de validación de argumentos de método
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(
            org.springframework.web.bind.MethodArgumentNotValidException e) {

        // Extraer el primer error de validación
        String errorMessage = "Formato de request inválido";
        if (e.getBindingResult().hasErrors() && e.getBindingResult().getFieldError() != null) {
            errorMessage = e.getBindingResult().getFieldError().getDefaultMessage();
        }

        logger.error("Error de validación de argumentos: {}", errorMessage);
        return createErrorResponse(errorMessage, HttpStatus.BAD_REQUEST);
    }

    /**
     * Maneja errores generales no capturados
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception e) {
        logger.error("Error no manejado: {}", e.getMessage(), e);
        return createErrorResponse("Error inesperado en el servidor", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
