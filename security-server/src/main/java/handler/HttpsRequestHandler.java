package handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpsRequestHandler implements HttpHandler {

    private final int forwardPort;
    private static final Logger logger = LoggerFactory.getLogger(HttpsRequestHandler.class);


    public HttpsRequestHandler(int forwardPort) {
        this.forwardPort = forwardPort;
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            String requestURI = exchange.getRequestURI().toString();

            // handle root request
            if (requestURI.equals("/")) {
                String response = "Welcome to Vitruv-Server :)\n\n"
                        + "Important information can be found here: https://github.com/vitruv-tools/Vitruv";
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.getBytes().length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }

            logger.info("Redirect request '{}' to VitruvServer at port {}", requestURI, this.forwardPort);

            // connect to intern http VitruvServer
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
        catch (Exception e) {
            logger.error("An error occurred while handling the HTTP request: {}", e.getMessage());
        }
    }
}