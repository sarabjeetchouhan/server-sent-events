/**
 * Subscribes to order-specific SSE updates and renders them on the status page.
 *
 * The order ID comes from the query string set by the order creation page.
 * EventSource reconnects automatically; this example server does not replay
 * notifications missed while the browser is disconnected.
 */
const orderId = new URLSearchParams(window.location.search).get("orderId");
const orderIdElement = document.querySelector("#order-id");
const connectionStatus = document.querySelector("#connection-status");
const orderEvents = document.querySelector("#order-events");

if (!orderId) {
  connectionStatus.dataset.state = "error";
  connectionStatus.textContent = "No order ID was provided. Create an order first.";
} else {
  orderIdElement.textContent = orderId;

  const streamUrl = new URL("http://localhost:8080/api/v1/event/order-update");
  streamUrl.searchParams.set("orderId", orderId);
  const eventSource = new EventSource(streamUrl.toString());
  let connectionTimeout;

  /**
   * Starts the connection deadline while waiting for EventSource to open.
   *
   * @remarks The timer is started only once at a time and is restarted after
   * an interruption while EventSource attempts to reconnect.
   */
  function startConnectionTimeout() {
    if (connectionTimeout !== undefined) {
      return;
    }

    connectionTimeout = window.setTimeout(() => {
      connectionTimeout = undefined;
      eventSource.close();
      connectionStatus.dataset.state = "error";
      connectionStatus.textContent = "Could not connect to order updates within 20 seconds. Connection closed.";
    }, 20_000);
  }

  /**
   * Cancels the pending connection deadline after the stream opens or closes.
   */
  function clearConnectionTimeout() {
    window.clearTimeout(connectionTimeout);
    connectionTimeout = undefined;
  }

  /**
   * Parses and displays one order update event.
   *
   * Each event is rendered with an accessible button that toggles its JSON
   * payload. The demo identifies delivery from the displayed message and closes
   * the stream once the final update arrives.
   *
   * @param {MessageEvent<string>} event named SSE event from the order endpoint
   */
  function handleOrderUpdate(event) {
    document.querySelector("#waiting-message")?.remove();

    let update;
    try {
      update = JSON.parse(event.data);
    } catch (error) {
      console.error("[order-status] Could not parse order update", error);
      connectionStatus.dataset.state = "error";
      connectionStatus.textContent = "Received an update that could not be read.";
      return;
    }

    const item = document.createElement("li");
    item.className = "order-event";

    const heading = document.createElement("strong");
    heading.textContent = update.message;

    const time = document.createElement("time");
    time.dateTime = update.timestamp;
    time.textContent = new Date(update.timestamp).toLocaleString();

    const toggle = document.createElement("button");
    toggle.type = "button";
    toggle.className = "text-button";
    toggle.textContent = "Show event details";
    toggle.setAttribute("aria-expanded", "false");

    const details = document.createElement("pre");
    details.className = "event-details";
    details.id = `event-details-${orderEvents.children.length}`;
    toggle.setAttribute("aria-controls", details.id);
    details.hidden = true;
    details.textContent = JSON.stringify(update, null, 2);

    toggle.addEventListener("click", () => {
      details.hidden = !details.hidden;
      toggle.setAttribute("aria-expanded", String(!details.hidden));
      toggle.textContent = details.hidden ? "Show event details" : "Hide event details";
    });

    item.append(heading, time, toggle, details);
    orderEvents.append(item);
    connectionStatus.textContent = update.message;
    connectionStatus.dataset.state = "connected";
    console.info("[order-status] Received update", update);

    if (update.message?.toLowerCase().includes("delivered")) {
      connectionStatus.textContent = "Order delivered.";
      connectionStatus.dataset.state = "delivered";
      clearConnectionTimeout();
      eventSource.close();
    }
  }

  /**
   * Updates the connection message and restarts the deadline on reconnection.
   */
  function handleConnectionError() {
    if (eventSource.readyState === EventSource.CONNECTING) {
      startConnectionTimeout();
      connectionStatus.dataset.state = "connecting";
      connectionStatus.textContent = "Connection interrupted. Reconnecting...";
    } else if (eventSource.readyState === EventSource.CLOSED) {
      clearConnectionTimeout();
      connectionStatus.dataset.state = "error";
      connectionStatus.textContent = "Order update stream closed.";
    }
  }

  startConnectionTimeout();

  eventSource.onopen = () => {
    clearConnectionTimeout();
    connectionStatus.dataset.state = "connected";
    connectionStatus.textContent = "Connected. Listening for order updates.";
  };
  eventSource.addEventListener("order-update", handleOrderUpdate);
  eventSource.onerror = handleConnectionError;
}
