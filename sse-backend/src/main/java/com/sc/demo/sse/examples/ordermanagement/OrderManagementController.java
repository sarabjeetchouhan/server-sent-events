package com.sc.demo.sse.examples.ordermanagement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * HTTP API for creating orders and subscribing to their status updates.
 *
 * <p>The create endpoint returns the new order ID as plain text. A client then
 * uses that ID to open an {@code text/event-stream} subscription.</p>
 */
@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500"})
public class OrderManagementController {

    private static final Logger log = LoggerFactory.getLogger(OrderManagementController.class);

    private final OrderManagementService orderManagementService;
    private final OrderUpdateEventNotificationManager orderUpdateEventNotificationManager;

    /**
     * Creates the controller with the order workflow and SSE subscription manager.
     *
     * @param orderManagementService validates and starts order processing
     * @param orderUpdateEventNotificationManager registers SSE subscribers
     */
    public OrderManagementController(OrderManagementService orderManagementService, OrderUpdateEventNotificationManager orderUpdateEventNotificationManager) {
        this.orderManagementService = orderManagementService;
        this.orderUpdateEventNotificationManager = orderUpdateEventNotificationManager;
    }

    /**
     * Validates and accepts an order for asynchronous processing.
     *
     * <p>Requests with a quantity above 100 receive {@code 400 Bad Request}.
     * The service validates the remaining fields. On acceptance, the response
     * body contains the order ID that clients use to subscribe to updates.</p>
     *
     * @param orderRequest item, positive quantity, and delivery address
     * @return HTTP 202 and the generated order ID, or HTTP 400 for an excessive
     *         quantity
     */
    @PostMapping("/create-order")
    public ResponseEntity<String> createOrder(@RequestBody OrderRequest orderRequest) {
        log.info("Received request to create order {}", orderRequest);
        if (orderRequest.quantity() > 100) {
            log.error("Order request quantity exceeds 100");
            return ResponseEntity.badRequest().body("Order request quantity exceeds 100");
        }
        String orderId = orderManagementService.createOrder(orderRequest);
        return ResponseEntity.accepted().body(orderId);
    }

    /**
     * Opens an SSE connection for updates associated with one order.
     *
     * @param orderId identifier returned by {@link #createOrder(OrderRequest)}
     * @return the long-lived event stream for this client
     */
    @GetMapping(value = "/event/order-update", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter updateOrder(@RequestParam String orderId) {
        return orderUpdateEventNotificationManager.subscriber(orderId);
    }
}
