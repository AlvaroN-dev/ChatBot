package com.riwi.chat.model;

import java.time.LocalDateTime;

/**
 * Modelo interno para representar un mensaje del chat
 * Este es el modelo de dominio de la aplicación
 */
public class ChatMessage {

    private String id;
    private String role;           // "system", "user", "assistant"
    private String content;
    private String sessionId;
    private LocalDateTime timestamp;
    private Integer tokenCount;    // Opcional: para tracking de tokens

    // Constructores
    public ChatMessage() {
        this.timestamp = LocalDateTime.now();
    }

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    public ChatMessage(String role, String content, String sessionId) {
        this.role = role;
        this.content = content;
        this.sessionId = sessionId;
        this.timestamp = LocalDateTime.now();
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public Integer getTokenCount() {
        return tokenCount;
    }

    public void setTokenCount(Integer tokenCount) {
        this.tokenCount = tokenCount;
    }

    // Métodos de utilidad

    /**
     * Verifica si el mensaje es del usuario
     */
    public boolean isUserMessage() {
        return "user".equalsIgnoreCase(this.role);
    }

    /**
     * Verifica si el mensaje es del asistente
     */
    public boolean isAssistantMessage() {
        return "assistant".equalsIgnoreCase(this.role);
    }

    /**
     * Verifica si el mensaje es del sistema
     */
    public boolean isSystemMessage() {
        return "system".equalsIgnoreCase(this.role);
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "id='" + id + '\'' +
                ", role='" + role + '\'' +
                ", content='" + (content != null && content.length() > 50
                ? content.substring(0, 50) + "..."
                : content) + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", timestamp=" + timestamp +
                ", tokenCount=" + tokenCount +
                '}';
    }
}