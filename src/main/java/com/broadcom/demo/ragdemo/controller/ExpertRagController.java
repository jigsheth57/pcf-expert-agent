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

@RestController
@RequestMapping("/api")
public class ExpertRagController {

    private final ChatClient chatClient;
    private final ChatClient lclchatClient;
    private final VectorStore vectorStore;
    private final Resource systemMessage;
    private final LargeDocumentIngestion docLoad;

    public ExpertRagController(
        @Qualifier("proModel") org.springframework.ai.chat.model.ChatModel glchatModel,
        @Qualifier("gemmaChatModel") org.springframework.ai.chat.model.ChatModel olchatModel,
        VectorStore vectorStore,
        LargeDocumentIngestion docLoad,
        @Value("classpath:/expert-system-message.st") Resource systemMessage) {

        this.vectorStore = vectorStore;
        this.systemMessage = systemMessage;
        this.docLoad = docLoad;

        // Build the ChatClient with the system message template
        this.chatClient = ChatClient.builder(glchatModel)
                .defaultSystem(systemMessage)
                .build();
        this.lclchatClient = ChatClient.builder(olchatModel)
                .defaultSystem(systemMessage)
                .build();

    }

    @GetMapping("/loaddata")
    public String loadPDF() {
        return docLoad.ingestPdf();
    }

    @GetMapping("/assistant")
    public String expertRagChat(@RequestParam(value = "message") String message) {


        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder().query("Represent this sentence for searching relevant passages: "+message).similarityThreshold(0.65).topK(5).build());

        System.out.println("Found: "+documents.size());
        for (int i = 0; i < documents.size(); i++) {
            System.out.println("Element at index " + i + ": " + documents.get(i).getScore());
        }
        // Use QuestionAnswerAdvisor to perform RAG:
        // 1. Search the VectorStore (PGVector) for relevant documents.
        // 2. Insert the retrieved documents into the {documents} placeholder in the system message.
        // 3. Send the augmented prompt to the LLM (Ollama).
        return chatClient.prompt()
                .user(message)
                .advisors(searchDB(message))
                .call()
                .content();
    }

    @GetMapping("/lassistant")
    public String expertRagLocalChat(@RequestParam(value = "message") String message) {


        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder().query("Represent this sentence for searching relevant passages: "+message).similarityThreshold(0.65).topK(5).build());

        System.out.println("Found: "+documents.size());
        for (int i = 0; i < documents.size(); i++) {
            System.out.println("Element at index " + i + ": " + documents.get(i).getScore());
        }
        // Use QuestionAnswerAdvisor to perform RAG:
        // 1. Search the VectorStore (PGVector) for relevant documents.
        // 2. Insert the retrieved documents into the {documents} placeholder in the system message.
        // 3. Send the augmented prompt to the LLM (Ollama).
        return lclchatClient.prompt()
                .user(message)
                .advisors(searchDB(message))
                .call()
                .content();
    }

    QuestionAnswerAdvisor searchDB(String message) {
        // 1. Define the format you want the model to see
        // The advisor will append this to the user's query.
        String customAdvisorText = """
            <context>
            {question_answer_context}
            </context>
            """;
        PromptTemplate customPromptTemplate = new PromptTemplate(customAdvisorText);
        QuestionAnswerAdvisor ragAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
            .searchRequest(SearchRequest.builder().query("Represent this sentence for searching relevant passages: "+message).similarityThreshold(0.65).topK(5).build())
            .promptTemplate(customPromptTemplate)
            .build();
        return ragAdvisor;
    }

}
