package com.riwi.chat.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


@Configuration
@ConfigurationProperties(prefix = "chatbot")
@Validated
public class ApiConfig {


    // AI21 Studio API Configuration
    private AI21Config ai21 = new AI21Config();

    // HTTP Client Configuration
    private HttpClientConfig http = new HttpClientConfig();

    // CORS Configuration
    private CorsConfig cors = new CorsConfig();

    // Getters y Setters principales
    public AI21Config getAi21() {
        return ai21;
    }

    public void setAi21(AI21Config ai21) {
        this.ai21 = ai21;
    }

    public HttpClientConfig getHttp() {
        return http;
    }

    public void setHttp(HttpClientConfig http) {
        this.http = http;
    }

    public CorsConfig getCors() {
        return cors;
    }

    public void setCors(CorsConfig cors) {
        this.cors = cors;
    }

    // Clase interna para configuración de AI21
    public static class AI21Config {
        private ApiSettings api = new ApiSettings();

        public ApiSettings getApi() {
            return api;
        }

        public void setApi(ApiSettings api) {
            this.api = api;
        }

        public static class ApiSettings {
            @NotBlank(message = "AI21 API Key es requerido")
            private String key;

            @NotBlank(message = "AI21 API URL es requerido")
            private String url;

            @NotBlank(message = "AI21 Model es requerido")
            private String model;

            private Integer maxTokens = 300;
            private Double temperature = 0.7;

            // Getters y Setters
            public String getKey() {
                return key;
            }

            public void setKey(String key) {
                this.key = key;
            }

            public String getUrl() {
                return url;
            }

            public void setUrl(String url) {
                this.url = url;
            }

            public String getModel() {
                return model;
            }

            public void setModel(String model) {
                this.model = model;
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
        }
    }

    // Clase interna para configuración HTTP
    public static class HttpClientConfig {
        private ClientSettings client = new ClientSettings();

        public ClientSettings getClient() {
            return client;
        }

        public void setClient(ClientSettings client) {
            this.client = client;
        }

        public static class ClientSettings {
            @NotNull(message = "HTTP Connect Timeout es requerido")
            private Integer connectTimeout = 10000;

            @NotNull(message = "HTTP Read Timeout es requerido")
            private Integer readTimeout = 30000;

            private Integer maxConnections = 20;

            // Getters y Setters
            public Integer getConnectTimeout() {
                return connectTimeout;
            }

            public void setConnectTimeout(Integer connectTimeout) {
                this.connectTimeout = connectTimeout;
            }

            public Integer getReadTimeout() {
                return readTimeout;
            }

            public void setReadTimeout(Integer readTimeout) {
                this.readTimeout = readTimeout;
            }

            public Integer getMaxConnections() {
                return maxConnections;
            }

            public void setMaxConnections(Integer maxConnections) {
                this.maxConnections = maxConnections;
            }
        }
    }

    // Clase interna para configuración CORS
    public static class CorsConfig {
        private String allowedOrigins;
        private String allowedMethods;
        private String allowedHeaders;

        // Getters y Setters
        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public String getAllowedMethods() {
            return allowedMethods;
        }

        public void setAllowedMethods(String allowedMethods) {
            this.allowedMethods = allowedMethods;
        }

        public String getAllowedHeaders() {
            return allowedHeaders;
        }

        public void setAllowedHeaders(String allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
        }

        // Método utilitario
        public String[] getAllowedOriginsAsArray() {
            if (allowedOrigins == null || allowedOrigins.trim().isEmpty()) {
                return new String[0];
            }
            return allowedOrigins.split(",");
        }

        public String[] getAllowedMethodsAsArray() {
            if (allowedMethods == null || allowedMethods.trim().isEmpty()) {
                return new String[]{"GET", "POST", "PUT", "DELETE", "OPTIONS"};
            }
            return allowedMethods.split(",");
        }
    }

    /**
     * Valida que todas las configuraciones requeridas estén presentes
     */
    public void validateConfiguration() {
        if (ai21.getApi().getKey() == null || ai21.getApi().getKey().trim().isEmpty()) {
            throw new IllegalStateException("CHATBOT_AI21_API_KEY no está configurado");
        }
        if (ai21.getApi().getUrl() == null || ai21.getApi().getUrl().trim().isEmpty()) {
            throw new IllegalStateException("CHATBOT_AI21_API_URL no está configurado");
        }
        if (ai21.getApi().getModel() == null || ai21.getApi().getModel().trim().isEmpty()) {
            throw new IllegalStateException("CHATBOT_AI21_MODEL no está configurado");
        }
    }

    @Override
    public String toString() {
        return "ApiConfig{" +
                "ai21.api.url='" + ai21.getApi().getUrl() + '\'' +
                ", ai21.api.model='" + ai21.getApi().getModel() + '\'' +
                ", http.client.connectTimeout=" + http.getClient().getConnectTimeout() +
                ", http.client.readTimeout=" + http.getClient().getReadTimeout() +
                ", cors.allowedOrigins='" + cors.getAllowedOrigins() + '\'' +
                ", ai21.api.key='***HIDDEN***'" +
                '}';
    }

}
