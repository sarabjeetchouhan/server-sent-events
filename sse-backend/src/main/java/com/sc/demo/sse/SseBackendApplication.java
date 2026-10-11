package com.sc.demo.sse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Bootstraps the Spring Boot application that exposes the server-sent events API.
 *
 * <p>The application uses Spring MVC's {@code SseEmitter} to keep an HTTP
 * response open and stream notifications from the server to connected clients.
 * The endpoint is available at {@code GET /api/stream}.</p>
 */
@SpringBootApplication
public class SseBackendApplication {

    /** Creates the Spring Boot application entry point. */
    public SseBackendApplication() {}

    /**
     * Starts the embedded web server and initializes the Spring application context.
     *
     * @param args command-line arguments passed to Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(SseBackendApplication.class, args);
    }
}
