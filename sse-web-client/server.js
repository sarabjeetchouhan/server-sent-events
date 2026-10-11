import { readFile } from "node:fs";
import { createServer } from "node:http";
import { extname, resolve, sep } from "node:path";
import { fileURLToPath } from "node:url";

/**
 * Serves the static learning pages and browser JavaScript for the SSE demos.
 *
 * <p>This Node.js process only hosts the frontend files. SSE connections and
 * order API requests are made by the browser directly to the Spring Boot app.</p>
 */
const webRoot = resolve(fileURLToPath(new URL(".", import.meta.url)));
const server = createServer(handleRequest);

/**
 * Resolves an incoming page or asset request to an allowed file under the web root.
 *
 * @param {import("node:http").IncomingMessage} request incoming HTTP request
 * @param {import("node:http").ServerResponse} response response to write to
 */
function handleRequest(request, response) {
  if (request.method !== "GET") {
    response.writeHead(405, { Allow: "GET" });
    response.end("Method not allowed");
    return;
  }

  const pathname = new URL(request.url, "http://localhost").pathname;
  const routes = {
    "/": "index.html",
    "/orders": "pages/order-creation.html",
    "/orders/status": "pages/order-status.html",
    "/robots.txt": "robots.txt",
  };
  const requestedFile = routes[pathname] || pathname.slice(1);
  const isWebPage = requestedFile === "index.html"
    || requestedFile === "robots.txt"
    || requestedFile.startsWith("pages/");
  const isClientAsset = requestedFile.startsWith("src/");
  if (!isWebPage && !isClientAsset) {
    response.writeHead(404);
    response.end("Page not found");
    return;
  }

  const filePath = resolve(webRoot, requestedFile);

  if (filePath !== webRoot && !filePath.startsWith(`${webRoot}${sep}`)) {
    response.writeHead(403);
    response.end("Forbidden");
    return;
  }

  readFile(filePath, (error, content) => {
    if (error) {
      response.writeHead(404, { "Content-Type": "text/plain; charset=utf-8" });
      response.end("Page not found");
      return;
    }

    const contentTypes = {
      ".css": "text/css",
      ".html": "text/html",
      ".js": "text/javascript",
      ".txt": "text/plain",
    };
    const contentType = contentTypes[extname(filePath)] || "application/octet-stream";
    response.writeHead(200, { "Content-Type": `${contentType}; charset=utf-8` });
    response.end(content);
  });
}

const host = process.env.HOST || "0.0.0.0";
const port = Number(process.env.PORT) || 5500;
server.listen(port, host, () => {
  console.log(`SSE guide is available at http://localhost:${port}`);
});
