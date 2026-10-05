package com.kidsanim.api.health;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Hace un GET simple a un endpoint HTTP y reporta si respondió 2xx.
 * Se aísla en su propia clase para poder sustituirlo en tests.
 */
@Component
public class HttpProbe {

    private final HttpClient httpClient;

    public HttpProbe() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
    }

    HttpProbe(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public ComponentHealth probe(String url, int timeoutMs) {
        long start = System.nanoTime();
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .GET()
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            long latency = (System.nanoTime() - start) / 1_000_000;
            int code = response.statusCode();
            if (code >= 200 && code < 300) {
                return ComponentHealth.up(url, latency, "HTTP " + code);
            }
            return ComponentHealth.down(url, "HTTP " + code);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ComponentHealth.down(url, "Interrumpido");
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ComponentHealth.down(url, "No responde: " + msg);
        }
    }
}
