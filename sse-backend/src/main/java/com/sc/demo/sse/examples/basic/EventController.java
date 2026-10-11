package com.sc.demo.sse.examples.basic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Exposes the server-sent events stream.
 *
 * <p>Each {@code GET /api/stream} request creates an independent stream that
 * sends ten JSON notifications, two seconds apart, before completing. Events
 * use the name {@code notification} and have incrementing IDs for clients to
 * inspect. This example does not retain events or replay them based on
 * {@code Last-Event-ID}; reconnecting clients start a new stream.</p>
 *
 * <p>SSE is a unidirectional protocol over a long-lived HTTP response. A browser
 * can consume this endpoint with the native {@code EventSource} API, which
 * handles reconnection at the transport level.</p>
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"})
public class EventController {
    private static final Logger logger = LoggerFactory.getLogger(EventController.class);

    /**
     * Number of notifications sent before a stream is completed.
     */
    private static final int EVENT_LIMIT = 10;

    /**
     * Delay between notifications for each connected client.
     */
    private static final long EVENT_INTERVAL_SECONDS = 2;

    private final ScheduledExecutorService scheduler;

    /**
     * Creates the controller using the application-managed event scheduler.
     *
     * @param scheduler executor used to send notifications without blocking
     *                  servlet request threads
     */
    public EventController(@Qualifier("sseScheduler") ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * Opens a stream and sends a finite sequence of notifications.
     *
     * <p>The response has the {@code text/event-stream} media type. Each event
     * has a unique-in-this-stream ID, the {@code notification} event name, and a
     * JSON-serialized {@link Notification} payload. The emitter times out after
     * 60 seconds. The scheduled task is cancelled when the client disconnects,
     * the stream completes, or the emitter times out.</p>
     *
     * @return the asynchronous SSE response
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        SseEmitter emitter = new SseEmitter(60_000L);
        AtomicInteger eventNumber = new AtomicInteger();
        logger.info("Opening SSE stream");

        Runnable sendEvent = () -> {
            int id = eventNumber.incrementAndGet();
            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(id))
                        .name("notification")
                        .data(new Notification(id, "Message " + id, Instant.now())));
                logger.debug("Sent SSE notification {}", id);
                if (id >= EVENT_LIMIT) {
                    logger.info("Completed SSE stream after {} notifications", id);
                    emitter.complete();
                }
            } catch (IOException | IllegalStateException exception) {
                logger.warn("Unable to send SSE notification {}; completing stream with error", id, exception);
                emitter.completeWithError(exception);
            }
        };

        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(
                sendEvent, 0, EVENT_INTERVAL_SECONDS, TimeUnit.SECONDS);
        emitter.onCompletion(() -> {
            task.cancel(false);
            logger.debug("SSE stream completed");
        });
        emitter.onTimeout(() -> {
            task.cancel(false);
            logger.warn("SSE stream timed out");
        });
        emitter.onError(error -> {
            task.cancel(false);
            logger.debug("SSE stream terminated with an error", error);
        });
        return emitter;
    }

    /**
     * JSON payload delivered in each {@code notification} SSE event.
     *
     * @param id        sequence number within this stream
     * @param message   human-readable notification text
     * @param timestamp UTC instant at which the notification was created
     */
    public record Notification(int id, String message, Instant timestamp) {
    }
}
