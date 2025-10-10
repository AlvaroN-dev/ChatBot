package com.riwi.chat.model;

import java.time.LocalDateTime;

/**
 * DTO para enviar responses al frontend
 * Representa la respuesta que envía el backend al cliente
 */
public class ChatResponse {

    private boolean success;
    private String message;
    private String sessionId;
    private LocalDateTime timestamp;
    private Integer tokensUsed;     // Opcional: información de tokens consumidos
    private String error;           // Opcional: mensaje de error si success = false

    // Constructores
    public ChatResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ChatResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public ChatResponse(boolean success, String message, String sessionId) {
        this.success = success;
        this.message = message;
        this.sessionId = sessionId;
        this.timestamp = LocalDateTime.now();
    }

    // Métodos estáticos para crear respuestas comunes

    /**
     * Crea una respuesta exitosa
     */
    public static ChatResponse success(String message, String sessionId) {
        ChatResponse response = new ChatResponse(true, message, sessionId);
        return response;
    }

    /**
     * Crea una respuesta de error
     */
    public static ChatResponse error(String errorMessage) {
        ChatResponse response = new ChatResponse();
        response.setSuccess(false);
        response.setError(errorMessage);
        return response;
    }

    // Getters y Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getTokensUsed() {
        return tokensUsed;
    }

    public void setTokensUsed(Integer tokensUsed) {
        this.tokensUsed = tokensUsed;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "ChatResponse{" +
                "success=" + success +
                ", message='" + (message != null && message.length() > 100
                ? message.substring(0, 100) + "..."
                : message) + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", timestamp=" + timestamp +
                ", tokensUsed=" + tokensUsed +
                ", error='" + error + '\'' +
                '}';
    }
}