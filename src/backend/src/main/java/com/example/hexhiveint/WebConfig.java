package com.example.hexhiveint;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Global web configuration for the Hexive backend.
 *
 * <p>Configures static resource serving from the classpath and
 * enables Cross-Origin Resource Sharing (CORS) for all API endpoints,
 * allowing the frontend dashboard to communicate with the backend
 * from any origin during development and deployment.</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Registers static resource handlers, serving files from {@code classpath:/static/}.
     *
     * @param registry the resource handler registry provided by Spring MVC
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }

    /**
     * Configures CORS mappings for all {@code /api/**} endpoints.
     *
     * <p>Permits GET, POST, PUT, DELETE, and OPTIONS methods from any origin.
     * Credentials are disabled to simplify token-free development flows.</p>
     *
     * @param registry the CORS registry provided by Spring MVC
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false);
    }
}
