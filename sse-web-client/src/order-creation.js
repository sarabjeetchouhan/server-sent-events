const orderForm = document.querySelector("#order-form");
const submitButton = document.querySelector("#submit-order");
const result = document.querySelector("#result");

/**
 * Sends the order form to Spring Boot and navigates to its live status page.
 *
 * The create endpoint responds with an order ID as plain text. That ID is passed
 * in the status page URL so its EventSource can subscribe to the matching order.
 *
 * @param {SubmitEvent} event form submission event
 */
async function handleOrderSubmission(event) {
  event.preventDefault();
  submitButton.disabled = true;
  result.textContent = "Creating order...";

  const order = {
    itemName: document.querySelector("#item-name").value.trim(),
    quantity: document.querySelector("#quantity").value,
    address: document.querySelector("#address").value.trim(),
  };

  console.info("[orders] Sending order creation request", {
    itemName: order.itemName,
    quantity: order.quantity,
  });

  try {
    const response = await fetch("http://localhost:8080/api/v1/create-order", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(order),
    });
    const responseMessage = await response.text();

    if (!response.ok) {
      console.error("[orders] Order creation failed", {
        status: response.status,
        message: responseMessage,
      });
      throw new Error(responseMessage || `Request failed with status ${response.status}`);
    }

    console.info("[orders] Order created", {
      status: response.status,
      message: responseMessage,
    });
    const orderId = responseMessage.trim();
    window.location.assign(`/orders/status?orderId=${encodeURIComponent(orderId)}`);
    orderForm.reset();
  } catch (error) {
    console.error("[orders] Could not send order creation request", error);
    result.textContent = `Could not create order: ${error.message}`;
  } finally {
    submitButton.disabled = false;
  }
}

orderForm.addEventListener("submit", handleOrderSubmission);
