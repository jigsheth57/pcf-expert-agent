package com.broadcom.demo.ragdemo.component;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class OllamaHealthIndicator implements ReactiveHealthIndicator {

    private final WebClient webClient;

    // We assume Ollama runs on default port 11434
    public OllamaHealthIndicator(WebClient.Builder webClientBuilder, @Value("${ollama.url}") String ollamaAPI) {
        this.webClient = webClientBuilder
                .baseUrl(ollamaAPI)
                .build();
    }

    @Override
    public Mono<Health> health() {
        return webClient.get()
                .uri("/api/version") // Fast endpoint, returns {"version": "x.x.x"}
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(1)) // Fail fast if Ollama is hanging
                .map(response -> Health.up()
                        .withDetail("ollama_version", parseVersion(response))
                        .build())
                .onErrorResume(ex -> Mono.just(Health.down()
                        .withDetail("error", "Ollama unreachable: " + ex.getMessage())
                        .build()));
    }

    // Simple helper to clean up the JSON response for the health report
    private String parseVersion(String json) {
        // Keeps the health check robust even if JSON parsing fails
        try {
            return json.replaceAll("[{}\"]", "").split(":")[1];
        } catch (Exception e) {
            return "unknown";
        }
    }
}