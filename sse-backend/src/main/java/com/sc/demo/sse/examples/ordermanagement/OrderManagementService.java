package com.sc.demo.sse.examples.ordermanagement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Validates incoming orders and simulates their asynchronous lifecycle.
 *
 * <p>Each accepted order advances through {@link OrderStage} values on the
 * order-update scheduler. When a client has subscribed, stage changes are
 * published through the notification manager.</p>
 */
@Service
public class OrderManagementService {

    private static final Logger log = LoggerFactory.getLogger(OrderManagementService.class);

    private final ScheduledExecutorService orderUpdateScheduler;

    private final OrderUpdateEventNotificationManager orderUpdateEventNotificationManager;

    /**
     * Creates the service with the scheduler and order event publisher.
     *
     * @param orderUpdateScheduler executor for scheduled order stage changes
     * @param orderUpdateEventNotificationManager sends stage updates to
     *                                            subscribers for each order
     */
    public OrderManagementService(@Qualifier("orderUpdateScheduler") ScheduledExecutorService orderUpdateScheduler, OrderUpdateEventNotificationManager orderUpdateEventNotificationManager) {
        this.orderUpdateScheduler = orderUpdateScheduler;
        this.orderUpdateEventNotificationManager = orderUpdateEventNotificationManager;
    }

    /**
     * Validates an order, assigns it an ID, and schedules its lifecycle.
     *
     * <p>The first stage runs after ten seconds, followed by one stage every ten
     * seconds. The repeating task stops after {@link OrderStage#DELIVERED}. If
     * processing throws a runtime exception, it is rethrown so the scheduler
     * suppresses future executions of that order's task.</p>
     *
     * @param orderRequest order data received from the client
     * @return generated ID used by clients to subscribe to this order
     * @throws ResponseStatusException with HTTP 400 if required fields are
     *                                 missing or quantity is not positive
     */
    public String createOrder(OrderRequest orderRequest) {
        validateOrderRequest(orderRequest);

        String orderId = UUID.randomUUID().toString();
        AtomicInteger counter = new AtomicInteger(1);
        AtomicReference<ScheduledFuture<?>> scheduledTask = new AtomicReference<>();
        Runnable task = () -> {
            try {
                OrderStage stage = OrderStage.fromId(counter.getAndIncrement());
                if (stage == null) {
                    log.error("Order Creation Failed");
                    throw new IllegalStateException("No order stage remains for order " + orderId);
                }
                processOrderAndSendNotificationEvent(stage, orderId, orderRequest);
                if (stage == OrderStage.DELIVERED) {
                    ScheduledFuture<?> future = scheduledTask.get();
                    if (future != null) {
                        future.cancel(false);
                    }
                }
            } catch (RuntimeException exception) {
                log.error("Stopping order update schedule after failure for order {}", orderId, exception);
                throw exception;
            }
        };
        ScheduledFuture<?> future = orderUpdateScheduler.scheduleAtFixedRate(task, 10, 10, TimeUnit.SECONDS);
        scheduledTask.set(future);

        return orderId;
    }

    /**
     * Checks required order fields before the asynchronous workflow is started.
     *
     * @param orderRequest request to validate
     * @throws ResponseStatusException with HTTP 400 if the request is missing,
     *                                 item name/address is blank, or quantity is
     *                                 null or not positive
     */
    private void validateOrderRequest(OrderRequest orderRequest) {
        if (orderRequest == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order details are required");
        }
        if (orderRequest.itemName() == null || orderRequest.itemName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item name is required");
        }
        if (orderRequest.address() == null || orderRequest.address().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Delivery address is required");
        }
        if (orderRequest.quantity() == null || orderRequest.quantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be greater than zero");
        }
    }

    /**
     * Builds and publishes the human-readable update for a single order stage.
     *
     * <p>If there are no active subscribers for the order, the notification
     * manager logs that fact and the order workflow continues.</p>
     *
     * @param stage current lifecycle stage
     * @param orderId ID of the order being processed
     * @param orderRequest original order data, used in the creation update
     */
    public void processOrderAndSendNotificationEvent(OrderStage stage, String orderId, OrderRequest orderRequest) {
        log.info("Processing order update event for order id {}", orderId);
        String message = "";
        switch (stage) {
            case CREATED:
                log.info("Creating order with id {}", orderId);
                message = String.format("%s for item: %s with quantity: %s. Here is order id: %s", stage.description(), orderRequest.itemName(), orderRequest.quantity(), orderId);
                orderUpdateEventNotificationManager.sendUpdate(orderId, OrderStage.CREATED, message);
                break;
            case PROCESSING:
                log.info("Processing order with id {}", orderId);
                message = String.format("%s for order id: %s", stage.description(), orderId);
                orderUpdateEventNotificationManager.sendUpdate(orderId, OrderStage.PROCESSING, message);
                break;
            case READY_TO_SHIP:
                log.info("Shipping order with id {}", orderId);
                message = String.format("%s for order id: %s", stage.description(), orderId);
                orderUpdateEventNotificationManager.sendUpdate(orderId, OrderStage.READY_TO_SHIP, message);
                break;
            case SHIPPED:
                log.info("Order with id {} is shipped", orderId);
                message = String.format("%s for order id: %s", stage.description(), orderId);
                orderUpdateEventNotificationManager.sendUpdate(orderId, OrderStage.SHIPPED, message);
                break;
            case OUT_FOR_DELIVERY:
                log.info("Order with id {} is out for delivery", orderId);
                message = String.format("%s for order id: %s", stage.description(), orderId);
                orderUpdateEventNotificationManager.sendUpdate(orderId, OrderStage.OUT_FOR_DELIVERY, message);
                break;
            case DELIVERED:
                log.info("Order with id {} is delivered", orderId);
                message = String.format("%s for order id: %s", stage.description(), orderId);
                orderUpdateEventNotificationManager.sendUpdate(orderId, OrderStage.DELIVERED, message);
                break;
            default:
                log.info("Unknown stage {}", stage);
        }
    }
}
