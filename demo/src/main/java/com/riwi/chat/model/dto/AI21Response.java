package com.riwi.chat.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTO para respuestas de la API de AI21 Studio
 * Mapea el formato de respuesta de AI21 Chat Completions API
 */
public class AI21Response {
    private String id;

    private String object;

    private Long created;

    private String model;

    private List<Choice> choices;

    private Usage usage;

    // Constructores
    public AI21Response() {}

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Choice> getChoices() {
        return choices;
    }

    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }

    public Usage getUsage() {
        return usage;
    }

    public void setUsage(Usage usage) {
        this.usage = usage;
    }

    /**
     * Clase interna para representar las opciones de respuesta
     */
    public static class Choice {
        private Integer index;

        private AI21Request.Message message;

        @JsonProperty("finish_reason")
        private String finishReason;

        // Constructores
        public Choice() {}

        // Getters y Setters
        public Integer getIndex() {
            return index;
        }

        public void setIndex(Integer index) {
            this.index = index;
        }

        public AI21Request.Message getMessage() {
            return message;
        }

        public void setMessage(AI21Request.Message message) {
            this.message = message;
        }

        public String getFinishReason() {
            return finishReason;
        }

        public void setFinishReason(String finishReason) {
            this.finishReason = finishReason;
        }

        @Override
        public String toString() {
            return "Choice{" +
                    "index=" + index +
                    ", message=" + message +
                    ", finishReason='" + finishReason + '\'' +
                    '}';
        }
    }

    /**
     * Clase interna para información de uso de tokens
     */
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;

        @JsonProperty("completion_tokens")
        private Integer completionTokens;

        @JsonProperty("total_tokens")
        private Integer totalTokens;

        // Constructores
        public Usage() {}

        // Getters y Setters
        public Integer getPromptTokens() {
            return promptTokens;
        }

        public void setPromptTokens(Integer promptTokens) {
            this.promptTokens = promptTokens;
        }

        public Integer getCompletionTokens() {
            return completionTokens;
        }

        public void setCompletionTokens(Integer completionTokens) {
            this.completionTokens = completionTokens;
        }

        public Integer getTotalTokens() {
            return totalTokens;
        }

        public void setTotalTokens(Integer totalTokens) {
            this.totalTokens = totalTokens;
        }

        @Override
        public String toString() {
            return "Usage{" +
                    "promptTokens=" + promptTokens +
                    ", completionTokens=" + completionTokens +
                    ", totalTokens=" + totalTokens +
                    '}';
        }
    }

    // Métodos de utilidad

    /**
     * Obtiene el contenido del primer mensaje de respuesta
     * @return contenido del mensaje o null si no hay respuesta
     */
    public String getFirstMessageContent() {
        if (choices != null && !choices.isEmpty()) {
            Choice firstChoice = choices.get(0);
            if (firstChoice.getMessage() != null) {
                return firstChoice.getMessage().getContent();
            }
        }
        return null;
    }

    /**
     * Verifica si la respuesta fue completada exitosamente
     * @return true si finish_reason es "stop"
     */
    public boolean isCompletedSuccessfully() {
        if (choices != null && !choices.isEmpty()) {
            Choice firstChoice = choices.get(0);
            return "stop".equals(firstChoice.getFinishReason());
        }
        return false;
    }

    /**
     * Obtiene el total de tokens usados
     * @return total de tokens o 0 si no está disponible
     */
    public int getTotalTokensUsed() {
        return usage != null && usage.getTotalTokens() != null
                ? usage.getTotalTokens()
                : 0;
    }

    @Override
    public String toString() {
        return "AI21Response{" +
                "id='" + id + '\'' +
                ", object='" + object + '\'' +
                ", created=" + created +
                ", model='" + model + '\'' +
                ", choices=" + choices +
                ", usage=" + usage +
                '}';
    }
}
