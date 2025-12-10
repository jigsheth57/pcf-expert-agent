package com.broadcom.demo.ragdemo.controller;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.broadcom.demo.ragdemo.component.LargeDocumentIngestion;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api")
public class ExpertRagController {

    private final ChatClient gChatClient;
    private final ChatClient lChatClient;
    private final VectorStore vectorStore;
    private final LargeDocumentIngestion docLoad;

    public ExpertRagController(
        @Qualifier("flashModel") org.springframework.ai.chat.model.ChatModel glchatModel,
        @Qualifier("gemmaChatModel") org.springframework.ai.chat.model.ChatModel olchatModel,
        VectorStore vectorStore,
        LargeDocumentIngestion docLoad,
        @Value("classpath:/expert-system-message-gemini.st") Resource cloudSystemMessage,
        @Value("classpath:/expert-system-message.st") Resource systemMessage) {

        this.vectorStore = vectorStore;
        this.docLoad = docLoad;

        this.gChatClient = ChatClient.builder(glchatModel)
                .defaultSystem(cloudSystemMessage)
                .build();
        this.lChatClient = ChatClient.builder(olchatModel)
                .defaultSystem(systemMessage)
                .build();
    }

    // Wrapped in Mono to make the blocking PDF ingestion async
    @GetMapping("/loaddata")
    public Mono<String> loadPDF() {
        return Mono.fromCallable(() -> docLoad.ingestPdf())
                .subscribeOn(Schedulers.boundedElastic());
    }

    // Returns Flux<String> for streaming response
    @GetMapping("/assistant")
    public Flux<String> expertRagChat(@RequestParam(value = "message") String message) {
        return performSearchAndChat(gChatClient, message);
    }

    // Returns Flux<String> for streaming response
    @GetMapping("/lassistant")
    public Flux<String> expertRagLocalChat(@RequestParam(value = "message") String message) {
        return performSearchAndChat(lChatClient, message);
    }

    /**
     * Helper method to handle the logging (blocking) and the chat stream (reactive).
     */
    private Flux<String> performSearchAndChat(ChatClient client, String message) {
        // 1. Run the similarity search for logging on a background thread (Elastic Scheduler)
        return Mono.fromRunnable(() -> {
            List<Document> documents = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query("Represent this sentence for searching relevant passages: " + message)
                            .similarityThreshold(0.65)
                            .topK(5)
                            .build()
            );

            System.out.println("Found: " + documents.size());
            for (int i = 0; i < documents.size(); i++) {
                System.out.println("Element at index " + i + ": " + documents.get(i).getScore());
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        // 2. Once logging is done, switch to the ChatClient stream
        .thenMany(
            client.prompt()
                .user(message)
                .advisors(searchDB(message))
                .stream() // Use stream() for WebFlux/SSE
                .content()
        );
    }

    QuestionAnswerAdvisor searchDB(String message) {
        String customAdvisorText = """
            <context>
            {question_answer_context}
            </context>
            """;
        PromptTemplate customPromptTemplate = new PromptTemplate(customAdvisorText);
        
        return QuestionAnswerAdvisor.builder(vectorStore)
            .searchRequest(SearchRequest.builder()
                    .query("Represent this sentence for searching relevant passages: " + message)
                    .similarityThreshold(0.65)
                    .topK(5)
                    .build())
            .promptTemplate(customPromptTemplate)
            .build();
    }
}