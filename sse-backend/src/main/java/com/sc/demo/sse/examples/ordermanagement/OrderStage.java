package com.sc.demo.sse.examples.ordermanagement;

/**
 * Ordered stages in the example order fulfillment lifecycle.
 *
 * <p>The numeric IDs determine the simulated processing order and are used by
 * {@link #fromId(int)} to select the next stage.</p>
 */
public enum OrderStage {

    /** Order has been accepted by the system. */
    CREATED(1, "Order created"),
    /** Order processing has started. */
    PROCESSING(2, "Order is processing."),
    /** Order is prepared and waiting for dispatch. */
    READY_TO_SHIP(3, "Order is ready to ship."),
    /** Order has been dispatched to the carrier. */
    SHIPPED(4, "Order is shipped."),
    /** Carrier is taking the order to its destination. */
    OUT_FOR_DELIVERY(5, "Order is out for delivery."),
    /** Order has reached its destination and its update stream can close. */
    DELIVERED(6, "Order is delivered.");

    private int id;
    private String description;

    /**
     * Defines a stage's sequence number and display text.
     *
     * @param id stage sequence number
     * @param description human-readable stage message
     */
    OrderStage(int id, String description) {
        this.id = id;
        this.description = description;
    }

    /**
     * Returns the stage sequence number.
     *
     * @return numeric order of this stage in the workflow
     */
    public int id() {
        return id;
    }

    /**
     * Returns the text shown to clients for this stage.
     *
     * @return human-readable stage description
     */
    public String description() {
        return description;
    }

    /**
     * Finds a stage by its numeric sequence number.
     *
     * @param id sequence number to look up
     * @return matching stage, or {@code null} if no stage has that ID
     */
    public static OrderStage fromId(int id) {
        for (OrderStage stage : OrderStage.values()) {
            if (stage.id() == id) {
                return stage;
            }
        }
        return null;
    }
}
