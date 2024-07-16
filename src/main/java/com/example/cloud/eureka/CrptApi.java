package com.example.cloud.eureka;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private final Semaphore semaphore;
    private final ScheduledExecutorService scheduler;
    private final ObjectMapper objectMapper;
    private final String apiUrl = "https://ismp.crpt.ru/api/v3/lk/documents/create";

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.semaphore = new Semaphore(requestLimit);
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.objectMapper = new ObjectMapper();

        scheduler.scheduleAtFixedRate(semaphore::release, 0, 1, timeUnit);
    }

    public void createDocumentForIntroduction(Document document, String signature) throws InterruptedException, IOException {
        semaphore.acquire();
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Signature", signature);

            String jsonDocument = objectMapper.writeValueAsString(document);
            System.out.println("Sending JSON: " + jsonDocument);

            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(jsonDocument.getBytes());
                outputStream.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.out.println("Error response code: " + responseCode);
                throw new IOException("Failed to create document: " + responseCode);
            }
        } finally {
            semaphore.release();
        }
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
