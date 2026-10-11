package com.sc.demo.sse.examples.ordermanagement;

/**
 * Client-provided data needed to create an order.
 *
 * @param itemName name of the item being ordered
 * @param quantity positive number of items requested
 * @param address delivery destination
 */
public record OrderRequest(String itemName, Integer quantity, String address) {
}
