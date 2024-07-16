package com.example.cloud.eureka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final int PORT = 8000;
    private static CrptApi crptApi;

    public static void main(String[] args) throws IOException {
        crptApi = new CrptApi(TimeUnit.MINUTES, 100);

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/documents", new DocumentHandler());
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        System.out.println("Server started on port " + PORT);
    }

    static class DocumentHandler implements HttpHandler {
        private final ObjectMapper objectMapper = new ObjectMapper();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String signature = exchange.getRequestHeaders().getFirst("Signature");

                try (InputStream requestBody = exchange.getRequestBody()) {
                    Document document = objectMapper.readValue(requestBody, Document.class);

                    crptApi.createDocumentForIntroduction(document, signature);

                    String response = "Document created successfully";
                    exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                    os.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    String response = "Error: " + e.getMessage();
                    exchange.sendResponseHeaders(500, response.getBytes(StandardCharsets.UTF_8).length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                    os.close();
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }
}
