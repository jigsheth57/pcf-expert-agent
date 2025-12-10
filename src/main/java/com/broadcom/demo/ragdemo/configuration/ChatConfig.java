package com.broadcom.demo.ragdemo.configuration;

import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.genai.Client;

@Configuration
public class ChatConfig {

    private final Client googleGenAiClient;
    private final String FLASH_MODEL;
    private final String PRO_MODEL;
    private final String LOCAL_MODEL;
    private final String OLLAMA_API;

    public ChatConfig(Client googleGenAiClient, 
                        @Value("${google.flash.model}") String flashModel, 
                        @Value("${google.pro.model}") String proModel, 
                        @Value("${ollama.chat.model}") String localModel, 
                        @Value("${ollama.url}") String ollamaAPI) {
            this.googleGenAiClient = googleGenAiClient;
            this.FLASH_MODEL = flashModel;
            this.PRO_MODEL = proModel;
            this.LOCAL_MODEL = localModel;
            this.OLLAMA_API = ollamaAPI;
    }

    @Bean
    @Qualifier("flashModel")
    public GoogleGenAiChatModel flashModel() {
        GoogleGenAiChatOptions options = GoogleGenAiChatOptions.builder()
                .model(FLASH_MODEL) // Use the specific model name
                .temperature(0.7)
                .maxOutputTokens(2000)
                .thinkingBudget(512)
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
                .model(PRO_MODEL)
                .temperature(0.5)
                .maxOutputTokens(4000)
                .thinkingBudget(1024)
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
        OllamaApi ollamaApi = OllamaApi.builder().baseUrl(OLLAMA_API).build();

        // Configure options like model name, temperature, etc.
        OllamaChatOptions options = OllamaChatOptions.builder()
                .model(LOCAL_MODEL) // Specify the Ollama model to use
                .temperature((double) 0.7f)
                .build();

        return OllamaChatModel.builder()
        .ollamaApi(ollamaApi)
        .defaultOptions(options)
        .build();
    }    

}
