package com.sc.demo.sse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Configures infrastructure shared by the SSE endpoints.
 *
 * <p>A dedicated scheduler sends each client's notifications independently of
 * servlet request threads. Spring shuts this executor down when the application
 * context closes.</p>
 */
@Configuration
public class SseConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(SseConfiguration.class);

    /**
     * Creates the SSE infrastructure configuration.
     */
    public SseConfiguration() {
    }

    /**
     * Creates the executor used to schedule periodic SSE notifications.
     *
     * <p>The pool has four daemon threads. Scheduled callbacks should remain
     * short because they share this pool across all connected clients.</p>
     *
     * @return the application-managed SSE scheduler
     */
    @Bean(destroyMethod = "shutdown", name = "sseScheduler")
    ScheduledExecutorService sseScheduler() {
        logger.info("Creating SSE scheduler with four worker threads");
        return Executors.newScheduledThreadPool(4, task -> {
            Thread thread = new Thread(task, "sse-scheduler");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Creates the legacy order notification emitter bean.
     *
     * <p>The order management flow currently creates one emitter per HTTP
     * subscription in {@code OrderUpdateEventNotificationManager}; it does not
     * use this singleton bean. It remains here as an example of a Spring-managed
     * emitter bean.</p>
     *
     * @return an emitter with a ten-minute timeout
     */
    @Bean(name = "orderNotificationEvent")
    SseEmitter sseEmitter() {
        logger.info("Creating SSE emitter for order notification");
        return new SseEmitter(600000L);
    }

    /**
     * Creates the worker pool used to advance order stages asynchronously.
     *
     * <p>Spring shuts the executor down with the application context. A separate
     * pool prevents order progression tasks from sharing the basic SSE example's
     * scheduler.</p>
     *
     * @return the application-managed order update scheduler
     */
    @Bean(destroyMethod = "shutdown", name = "orderUpdateScheduler")
    ScheduledExecutorService orderUpdateScheduler() {
        logger.info("Creating order update scheduler with four worker threads");
        return Executors.newScheduledThreadPool(4, task -> {
            Thread thread = new Thread(task, "order-update-scheduler");
            thread.setDaemon(true);
            return thread;
        });
    }
}
