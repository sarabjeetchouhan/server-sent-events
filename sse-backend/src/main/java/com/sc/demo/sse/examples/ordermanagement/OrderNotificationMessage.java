package com.sc.demo.sse.examples.ordermanagement;

import java.time.Instant;

/**
 * JSON payload sent with each named {@code order-update} server-sent event.
 *
 * @param id unique identifier assigned to this event
 * @param message human-readable description of the order stage
 * @param timestamp time when the notification was created
 */
public record OrderNotificationMessage(String id, String message, Instant timestamp) {
}
