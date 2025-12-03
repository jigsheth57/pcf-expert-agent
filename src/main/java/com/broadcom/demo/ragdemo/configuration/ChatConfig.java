package com.broadcom.demo.ragdemo.configuration;

import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.genai.Client;

@Configuration
public class ChatConfig {

    private final Client googleGenAiClient;

    public ChatConfig(Client googleGenAiClient) {
        this.googleGenAiClient = googleGenAiClient;
    }

    @Bean
    @Qualifier("flashModel")
    public GoogleGenAiChatModel flashModel() {
        GoogleGenAiChatOptions options = GoogleGenAiChatOptions.builder()
                .model("gemini-2.5-flash") // Use the specific model name
                .temperature(0.7)
                .maxOutputTokens(2000)
                .build();
        
        return GoogleGenAiChatModel.builder()
 		.genAiClient(googleGenAiClient)
 		.defaultOptions(options)
 		.build();
    }

    @Bean
    @Qualifier("proModel")
    public GoogleGenAiChatModel proModel() {
        GoogleGenAiChatOptions options = GoogleGenAiChatOptions.builder()
                .model("gemini-2.5-pro")
                .temperature(0.5)
                .maxOutputTokens(4000)
                .build();

        return GoogleGenAiChatModel.builder()
 		.genAiClient(googleGenAiClient)
 		.defaultOptions(options)
 		.build();
    }

    @Bean
    @Qualifier("gemmaChatModel")
    public OllamaChatModel ollamaChatModel() {
        // You can customize the Ollama API host and port if needed
        OllamaApi ollamaApi = OllamaApi.builder().baseUrl("http://localhost:11434").build();

        // Configure options like model name, temperature, etc.
        OllamaChatOptions options = OllamaChatOptions.builder()
                .model("gemma3n:e2b-it-q8_0") // Specify the Ollama model to use
                .temperature((double) 0.7f)
                .build();

        return OllamaChatModel.builder()
        .ollamaApi(ollamaApi)
        .defaultOptions(options)
        .build();
    }    

}
