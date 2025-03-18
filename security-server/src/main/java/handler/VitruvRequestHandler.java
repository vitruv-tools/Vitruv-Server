package handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class VitruvRequestHandler implements HttpHandler {

    private final int forwardPort;
    private static final Logger logger = LoggerFactory.getLogger(VitruvRequestHandler.class);

    public VitruvRequestHandler(int forwardPort) {
        this.forwardPort = forwardPort;
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            String requestURI = exchange.getRequestURI().toString();

            // check for root request
            if (requestURI.equals("/")) {
                handleRootRequest(exchange);
                return;
            }

            logger.info("Redirect request '{}' to VitruvServer at port {}", requestURI, this.forwardPort);

            HttpURLConnection connection = createConnectionToInternVitruvServer(exchange);
            forwardRequestAndResponse(exchange, connection);

        } catch (Exception e) {
            logger.error("An error occurred while handling the HTTP request: {}", e.getMessage());
        }
    }

    private void handleRootRequest(HttpExchange exchange) throws IOException {
        String response = "Welcome to Vitruv-Server :)\n\n"
                + "Important information can be found here: https://github.com/vitruv-tools/Vitruv";
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(200, response.getBytes().length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }

    }

    private HttpURLConnection createConnectionToInternVitruvServer(HttpExchange exchange) throws IOException {
        final String vitruvHost = "http://localhost:" + this.forwardPort;
        final String fullUri = vitruvHost + exchange.getRequestURI().toString();

        // redirect HTTP request to Vitruv
        final HttpURLConnection connection = (HttpURLConnection) new URL(fullUri).openConnection();
        connection.setRequestMethod(exchange.getRequestMethod());
        exchange.getRequestHeaders().forEach((key, values) -> {
            for (String value : values) {
                connection.setRequestProperty(key, value);
            }
        });

        return connection;
    }

    private void forwardRequestAndResponse(HttpExchange exchange, HttpURLConnection connection) throws IOException {
        // redirect body
        if (exchange.getRequestBody().available() > 0) {
            connection.setDoOutput(true);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(exchange.getRequestBody().readAllBytes());
            }
        }

        // read answer
        final int responseCode = connection.getResponseCode();
        InputStream responseStream = responseCode >= 400
                ? connection.getErrorStream()
                : connection.getInputStream();

        // return answer to client
        exchange.sendResponseHeaders(responseCode, responseStream.available());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseStream.readAllBytes());
        }

        logger.debug("Response code: {}", responseCode);
    }
}
