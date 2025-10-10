package com.riwi.chat.client.exception;

/* Excepción personalizada para errores en la comunicación con AI21 Studio API
 * Encapsula información detallada sobre errores HTTP y de red
 */
public class AI21ClientException extends RuntimeException {

    private final Integer httpStatusCode;
    private final String errorType;

    /**
     * Constructor básico
     *
     * @param message mensaje descriptivo del error
     * @param cause   causa original del error
     */
    public AI21ClientException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatusCode = null;
        this.errorType = "UNKNOWN";
    }

    /**
     * Constructor con código HTTP
     *
     * @param message        mensaje descriptivo del error
     * @param cause          causa original del error
     * @param httpStatusCode código de status HTTP
     */
    public AI21ClientException(String message, Throwable cause, Integer httpStatusCode) {
        super(message, cause);
        this.httpStatusCode = httpStatusCode;
        this.errorType = determineErrorType(httpStatusCode);
    }

    /**
     * Constructor completo
     *
     * @param message        mensaje descriptivo del error
     * @param cause          causa original del error
     * @param httpStatusCode código de status HTTP
     * @param errorType      tipo de error personalizado
     */
    public AI21ClientException(String message, Throwable cause, Integer httpStatusCode, String errorType) {
        super(message, cause);
        this.httpStatusCode = httpStatusCode;
        this.errorType = errorType;
    }

    /**
     * Obtiene el código HTTP del error
     *
     * @return código HTTP o null si no aplica
     */
    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    /**
     * Obtiene el tipo de error
     *
     * @return tipo de error clasificado
     */
    public String getErrorType() {
        return errorType;
    }

    /**
     * Determina el tipo de error basado en el código HTTP
     *
     * @param statusCode código de status HTTP
     * @return categoría del error
     */
    private static String determineErrorType(Integer statusCode) {
        if (statusCode == null) {
            return "UNKNOWN";
        }

        if (statusCode == 400) {
            return "BAD_REQUEST";
        } else if (statusCode == 401) {
            return "UNAUTHORIZED";
        } else if (statusCode == 403) {
            return "FORBIDDEN";
        } else if (statusCode == 404) {
            return "NOT_FOUND";
        } else if (statusCode == 408 || statusCode == 504) {
            return "TIMEOUT";
        } else if (statusCode == 429) {
            return "RATE_LIMIT";
        } else if (statusCode >= 400 && statusCode < 500) {
            return "CLIENT_ERROR";
        } else if (statusCode >= 500 && statusCode < 600) {
            return "SERVER_ERROR";
        } else {
            return "UNKNOWN";
        }
    }

    /**
     * Verifica si el error es recuperable (puede reintentarse)
     *
     * @return true si el error puede ser temporal y vale la pena reintentar
     */
    public boolean isRetryable() {
        if (httpStatusCode == null) {
            return false;
        }

        // Errores recuperables: timeouts, rate limits, errores 5xx
        return httpStatusCode == 408 ||
                httpStatusCode == 429 ||
                httpStatusCode == 503 ||
                httpStatusCode == 504 ||
                (httpStatusCode >= 500 && httpStatusCode < 600);
    }

    /**
     * Verifica si el error es de autenticación
     *
     * @return true si el error está relacionado con autenticación
     */
    public boolean isAuthenticationError() {
        return httpStatusCode != null && (httpStatusCode == 401 || httpStatusCode == 403);
    }

    /**
     * Verifica si el error es de rate limit
     *
     * @return true si se excedió el límite de peticiones
     */
    public boolean isRateLimitError() {
        return httpStatusCode != null && httpStatusCode == 429;
    }

    /**
     * Verifica si el error es de timeout
     *
     * @return true si el error fue por timeout
     */
    public boolean isTimeoutError() {
        return "TIMEOUT".equals(errorType) ||
                (httpStatusCode != null && (httpStatusCode == 408 || httpStatusCode == 504));
    }

    /**
     * Obtiene un mensaje de error amigable para el usuario
     *
     * @return mensaje descriptivo en español
     */
    public String getUserFriendlyMessage() {
        if (isAuthenticationError()) {
            return "Error de autenticación con el servicio de IA. Contacta al administrador.";
        }

        if (isRateLimitError()) {
            return "Se ha excedido el límite de solicitudes. Por favor, intenta más tarde.";
        }

        if (isTimeoutError()) {
            return "El servicio está tardando más de lo esperado. Por favor, intenta nuevamente.";
        }

        if (httpStatusCode != null && httpStatusCode >= 500) {
            return "El servicio de IA está temporalmente no disponible. Intenta en unos momentos.";
        }

        if (httpStatusCode != null && httpStatusCode >= 400) {
            return "Hubo un problema con tu solicitud. Verifica los datos e intenta nuevamente.";
        }

        return "Ocurrió un error inesperado. Por favor, intenta más tarde.";
    }

    @Override
    public String toString() {
        return String.format("AI21ClientException{message='%s', httpStatus=%d, errorType='%s', retryable=%s}",
                getMessage(),
                httpStatusCode != null ? httpStatusCode : 0,
                errorType,
                isRetryable());
    }
}