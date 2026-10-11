/**
 * Demonstrates a basic browser SSE connection to the Spring Boot notification endpoint.
 *
 * <p>Node.js serves this script; the browser's {@link EventSource} API opens
 * and manages the long-lived SSE connection.</p>
 */
const streamUrl = document.querySelector("#stream-url");
const connectButton = document.querySelector("#connect-button");
const disconnectButton = document.querySelector("#disconnect-button");
const status = document.querySelector("#status");
const events = document.querySelector("#events");

let eventSource;

/**
 * Opens the configured stream and listens for named notification events.
 *
 * Repeated clicks close the old connection first so this page maintains only
 * one active EventSource.
 */
function connectToStream() {
  eventSource?.close();

  eventSource = new EventSource(streamUrl.value);
  status.textContent = "Connecting...";

  eventSource.onopen = () => {
    status.textContent = "Connected";
  };

  eventSource.addEventListener("notification", (event) => {
    const item = document.createElement("li");
    item.textContent = `Event ${event.lastEventId}: ${event.data}`;
    events.prepend(item);
  });

  eventSource.onerror = () => {
    status.textContent = "Connection lost. Reconnecting...";
  };
}

/**
 * Closes the current stream and updates the page to show it is disconnected.
 */
function disconnectFromStream() {
  eventSource?.close();
  eventSource = undefined;
  status.textContent = "Disconnected";
}

connectButton.addEventListener("click", connectToStream);
disconnectButton.addEventListener("click", disconnectFromStream);
