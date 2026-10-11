# SSE Web Client

This is a small Node.js app that serves HTML pages. The browser page uses
`EventSource` to connect to Spring Boot. Node.js does not open the SSE
connection; it serves the pages.

## Run

Start the Spring Boot backend in a separate terminal:

```bash
cd ../sse-backend
./gradlew bootRun
```

Start this Node.js app:

```bash
cd sse-web-client
npm start
```

Open <http://localhost:5500> for an overview of Server-Sent Events, Spring Boot,
and the interactive examples. The order creation page is available at
<http://localhost:5500/orders>. Add more HTML pages under `pages` and link them
from `index.html`.

After creating an order, the client opens `/orders/status?orderId=...`, subscribes
to that order's SSE update endpoint, and displays each live update.

The simple connection logic is in `src/app.js`. Change the URL or event name
there to experiment. The Spring Boot backend permits the local client origin
`http://localhost:5500` for SSE. The order API also needs to allow this origin.

For hosting, the Node server listens on `0.0.0.0` and uses the platform's
`PORT` environment variable when provided. Add the deployed domain to the
backend's CORS origins before using the browser examples from that host.
