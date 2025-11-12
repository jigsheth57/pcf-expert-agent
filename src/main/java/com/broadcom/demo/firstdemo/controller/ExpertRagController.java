package com.broadcom.demo.firstdemo.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ExpertRagController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final Resource systemMessage;


    public ExpertRagController(
        ChatClient.Builder chatClientBuilder, 
        VectorStore vectorStore,
        @Value("classpath:/expert-system-message.st") Resource systemMessage) {

        this.vectorStore = vectorStore;
        this.systemMessage = systemMessage;
        
        // Build the ChatClient with the system message template
        this.chatClient = chatClientBuilder
                .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore))
                .defaultSystem(systemMessage)
                .build();
    }

@GetMapping("/assistant")
    public String expertRagChat(@PathVariable String user, @RequestParam(value = "message") String message) {
        
        // Use QuestionAnswerAdvisor to perform RAG:
        // 1. Search the VectorStore (PGVector) for relevant documents.
        // 2. Insert the retrieved documents into the {documents} placeholder in the system message.
        // 3. Send the augmented prompt to the LLM (Ollama).
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }
}