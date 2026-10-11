package com.sc.demo.sse.examples.ordermanagement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains order-to-client SSE subscriptions and fans order updates out to them.
 *
 * <p>Each {@link SseEmitter} represents one HTTP client connection. The map
 * stores a set per order so multiple clients can subscribe to the same order.
 * This registry is held in memory and is local to this application instance.</p>
 */
@Component
public class OrderUpdateEventNotificationManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderUpdateEventNotificationManager.class);

    private final Map<String, Set<SseEmitter>> orderSubscriberMap = new ConcurrentHashMap<>();
    private static final String ORDER_UPDATE_EVENT = "order-update";

    /**
     * Creates an empty, in-memory registry of order subscribers.
     */
    public OrderUpdateEventNotificationManager() {
    }

    /**
     * Registers a new client connection for the requested order.
     *
     * <p>Completion, timeout, and error callbacks remove the emitter from the
     * registry so stale connections are not retained.</p>
     *
     * @param orderId order whose events this HTTP client wants to receive
     * @return a new emitter for the controller to return as the SSE response
     */
    public SseEmitter subscriber(String orderId) {
        LOGGER.info("Subscribing to order update event for order Id: {}", orderId);
        SseEmitter emitter = assignSseEmitter();
        orderSubscriberMap.computeIfAbsent(orderId, key -> ConcurrentHashMap.newKeySet()).add(emitter);
        emitter.onCompletion(() -> removeSubscriber(orderId, emitter));
        emitter.onTimeout(() -> removeSubscriber(orderId, emitter));
        emitter.onError(error -> removeSubscriber(orderId, emitter));
        LOGGER.info("Subscribed to order update event for order Id: {}", orderId);
        return emitter;
    }

    /**
     * Sends a named event to every current subscriber of the order.
     *
     * <p>Each event carries a generated event ID and an
     * {@link OrderNotificationMessage} JSON payload. Failed emitters are removed
     * independently. After the delivered stage is sent, all remaining
     * subscribers are completed and removed.</p>
     *
     * @param orderId order whose subscribers receive the event
     * @param stage current stage; {@link OrderStage#DELIVERED} ends the streams
     * @param message human-readable description of the stage
     */
    public void sendUpdate(String orderId, OrderStage stage, String message) {
        Set<SseEmitter> subscribers = orderSubscriberMap.get(orderId);
        if (subscribers != null && !subscribers.isEmpty()) {
            LOGGER.info("Sending update event for order id {}", orderId);
            for (SseEmitter emitter : subscribers) {
                String eventId = UUID.randomUUID().toString();
                try {
                    emitter.send(SseEmitter.event()
                            .id(eventId)
                            .name(ORDER_UPDATE_EVENT)
                            .data(new OrderNotificationMessage(eventId, message, Instant.now()))
                            .build()
                    );
                } catch (IOException | RuntimeException e) {
                    LOGGER.error("Error while sending update event for order id {}", orderId, e);
                    removeSubscriber(orderId, emitter);
                    emitter.completeWithError(e);
                }
            }
            if (OrderStage.DELIVERED == stage) {
                LOGGER.info("Delivered update event for order id {} now unsubscribing the event", orderId);
                subscribers.forEach(SseEmitter::complete);
                orderSubscriberMap.remove(orderId, subscribers);
            }
        } else {
            LOGGER.info("No subscription found with order id: {}", orderId);
        }
    }

    /**
     * Creates an emitter and logs its lifecycle callbacks.
     *
     * <p>Membership cleanup is registered separately by {@link #subscriber(String)}.
     * The timeout bounds how long an idle client connection can remain open.</p>
     *
     * @return a newly created emitter with a two-minute timeout
     */
    private SseEmitter assignSseEmitter() {
        SseEmitter sseEmitter = new SseEmitter(120_000L);
        sseEmitter.onCompletion(() -> {
            LOGGER.info("SSE emitter has been closed");
        });
        sseEmitter.onTimeout(() -> {
            LOGGER.info("SSE emitter has been timed out");
        });
        sseEmitter.onError((error) -> {
            LOGGER.error("SSE emitter has been closed", error);
        });
        return sseEmitter;
    }

    /**
     * Removes one emitter and removes its order key if no subscribers remain.
     *
     * @param orderId order associated with the emitter
     * @param emitter client connection to remove
     */
    private void removeSubscriber(String orderId, SseEmitter emitter) {
        Set<SseEmitter> subscribers = orderSubscriberMap.get(orderId);
        if (subscribers != null) {
            subscribers.remove(emitter);
            if (subscribers.isEmpty()) {
                orderSubscriberMap.remove(orderId, subscribers);
            }
        }
    }
}
