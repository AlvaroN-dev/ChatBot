package com.riwi.chat.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Collections;

/**
 * Configuración para RestTemplate y CORS
 * Configura el cliente HTTP que consumirá APIS externas (AI21 Studio)
 */
@Configuration
public class RestTemplateConfig implements WebMvcConfigurer {

    @Autowired
    private ApiConfig apiConfig;

    /**
     * Bean de RestTemplate configurado con timeouts personalizados
     * @return RestTemplate configurado
     */
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // Configurar factory con timeouts
        restTemplate.setRequestFactory(clientHttpRequestFactory());

        // Configurar converters JSON
        restTemplate.setMessageConverters(
                Collections.singletonList(new MappingJackson2HttpMessageConverter())
        );

        return restTemplate;
    }

    /**
     * Configura los timeouts del cliente HTTP
     * @return ClientHttpRequestFactory configurado
     */
    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        // Configurar timeouts desde ApiConfig con valores por defecto seguros
        Integer connectTimeout = apiConfig.getHttp().getClient().getConnectTimeout();
        Integer readTimeout = apiConfig.getHttp().getClient().getReadTimeout();

        factory.setConnectTimeout(connectTimeout != null ? connectTimeout : 10000);
        factory.setReadTimeout(readTimeout != null ? readTimeout : 30000);

        return factory;
    }

    /**
     * Configuración global de CORS
     * Permite requests desde el frontend
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = apiConfig.getCors().getAllowedOriginsAsArray();
        String[] methods = apiConfig.getCors().getAllowedMethodsAsArray();

        // Solo configurar CORS si hay orígenes definidos
        if (origins != null && origins.length > 0) {
            registry.addMapping("/**")
                    .allowedOrigins(origins)
                    .allowedMethods(methods)
                    .allowedHeaders("*")
                    .allowCredentials(true)
                    .maxAge(3600); // Cache preflight por 1 hora
        }
    }

    /**
     * Bean de RestTemplate específico para AI21 Studio
     * Con configuraciones optimizadas para llamadas a IA
     * @return RestTemplate optimizado para AI21
     */
    @Bean("ai21RestTemplate")
    public RestTemplate ai21RestTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // Factory con timeouts más largos para IA (las respuestas pueden tardar más)
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        Integer connectTimeout = apiConfig.getHttp().getClient().getConnectTimeout();
        Integer readTimeout = apiConfig.getHttp().getClient().getReadTimeout();

        factory.setConnectTimeout(connectTimeout != null ? connectTimeout : 10000);
        // Doble timeout de lectura para IA, ya que puede tardar en generar respuestas
        factory.setReadTimeout(readTimeout != null ? readTimeout * 2 : 60000);

        restTemplate.setRequestFactory(factory);
        restTemplate.setMessageConverters(
                Collections.singletonList(new MappingJackson2HttpMessageConverter())
        );

        return restTemplate;
    }
}