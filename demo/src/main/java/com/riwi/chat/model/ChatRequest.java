package com.riwi.chat.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para recibir requests del frontend
 * Representa la petición que envía el cliente al backend
 */
public class ChatRequest {

    @NotBlank(message = "El mensaje no puede estar vacío")
    @Size(min = 1, max = 5000, message = "El mensaje debe tener entre 1 y 5000 caracteres")
    private String message;

    private String sessionId;  // Opcional: para mantener contexto de conversación

    // Constructores
    public ChatRequest() {
    }

    public ChatRequest(String message) {
        this.message = message;
    }

    public ChatRequest(String message, String sessionId) {
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
        return "ChatRequest{" +
                "message='" + (message != null && message.length() > 100
                ? message.substring(0, 100) + "..."
                : message) + '\'' +
                ", sessionId='" + sessionId + '\'' +
                '}';
    }
}