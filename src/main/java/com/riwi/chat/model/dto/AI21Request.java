package com.riwi.chat.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * DTO para requests a la API de AI21 Studio
 * Mapea el formato esperado por AI21 Chat Completions API
 */
public class AI21Request {

    @NotBlank(message = "El modelo es requerido")
    private String model;

    @NotEmpty(message = "Los mensajes son requeridos")
    private List<Message> messages;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    private Double temperature;

    @JsonProperty("top_p")
    private Double topP;

    private Integer n;

    @JsonProperty("frequency_penalty")
    private Double frequencyPenalty;

    @JsonProperty("presence_penalty")
    private Double presencePenalty;

    private List<String> stop;

    // Constructores
    public AI21Request() {}

    public AI21Request(String model, List<Message> messages) {
        this.model = model;
        this.messages = messages;
    }

    // Getters y Setters
    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getTopP() {
        return topP;
    }

    public void setTopP(Double topP) {
        this.topP = topP;
    }

    public Integer getN() {
        return n;
    }

    public void setN(Integer n) {
        this.n = n;
    }

    public Double getFrequencyPenalty() {
        return frequencyPenalty;
    }

    public void setFrequencyPenalty(Double frequencyPenalty) {
        this.frequencyPenalty = frequencyPenalty;
    }

    public Double getPresencePenalty() {
        return presencePenalty;
    }

    public void setPresencePenalty(Double presencePenalty) {
        this.presencePenalty = presencePenalty;
    }

    public List<String> getStop() {
        return stop;
    }

    public void setStop(List<String> stop) {
        this.stop = stop;
    }

    /**
     * Clase interna para representar mensajes en el chat
     * Compatible con formato AI21 Studio
     */
    public static class Message {
        @NotBlank(message = "El rol del mensaje es requerido")
        private String role; // "system", "user", "assistant"

        @NotBlank(message = "El contenido del mensaje es requerido")
        private String content;

        // Constructores
        public Message() {}

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        // Getters y Setters
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

        @Override
        public String toString() {
            return "Message{" +
                    "role='" + role + '\'' +
                    ", content='" + content + '\'' +
                    '}';
        }
    }

    // Builder pattern para construcción fluida
    public static class Builder {
        private String model;
        private List<Message> messages;
        private Integer maxTokens;
        private Double temperature;
        private Double topP;
        private int n;

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder messages(List<Message> messages) {
            this.messages = messages;
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder topP(Double topP) {
            this.topP = topP;
            return this;
        }

        public Builder n(int n){
            this.n = n;
            return this;
        }

        public AI21Request build() {
            AI21Request request = new AI21Request();
            request.setModel(model);
            request.setMessages(messages);
            request.setMaxTokens(maxTokens);
            request.setTemperature(temperature);
            request.setTopP(topP);
            request.setN(n);
            return request;
        }
    }

    @Override
    public String toString() {
        return "AI21Request{" +
                "model='" + model + '\'' +
                ", messages=" + messages +
                ", maxTokens=" + maxTokens +
                ", temperature=" + temperature +
                ", topP=" + topP +
                ", n=" + n +
                '}';
    }

}
