package com.broadcom.demo.firstdemo.config;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import jakarta.annotation.PostConstruct;

@Configuration
public class LargeDocumentIngestion {

    private final VectorStore vectorStore;

    // Inject the PDF resource from the resources folder
    @Value("classpath:/tas-for-vms.pdf")
    private Resource pdfResource; 

    public LargeDocumentIngestion(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @PostConstruct
    public void ingestPdf() {
        if (!pdfResource.exists()) {
            System.err.println("FATAL ERROR: PDF file not found at " + pdfResource.getFilename());
            return;
        }

        System.out.println("⏳ Starting ingestion of large PDF document...");

        // 1. Read the PDF
        // Use PagePdfDocumentReader to read the document, with each page as a Document
        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(pdfResource);

        // 2. Chunk the Documents
        // TokenTextSplitter is essential for large docs. 
        // We use a small overlap (100 tokens) to preserve context across splits.
        TokenTextSplitter textSplitter = new TokenTextSplitter().builder().withChunkSize(550).withMinChunkSizeChars(150).withKeepSeparator(false).build(); 

        // Read, split, and get the final list of chunks
        List<Document> documents = pdfReader.get()
                                            .stream()
                                            .flatMap(doc -> textSplitter.split(doc).stream())
                                            .map(this::sanitizeDocumentContent)
                                            .toList();

        System.out.printf("📄 PDF Read. Total documents/chunks created: %d%n", documents.size());
        
        // 3. Store the Embeddings
        // The add() method automatically embeds the documents using the configured OllamaEmbeddingModel
        vectorStore.add(documents);

        System.out.println("✅ Ingestion complete. Documents embedded and stored in PGVector.");
    }

/**
     * Removes the illegal null character (0x00 or \u0000) from the document content.
     */
    private Document sanitizeDocumentContent(Document document) {
        String cleanedContent = document.getFormattedContent().replace('\u0000', ' ');
        // Optionally, you can replace it with an empty string:
        // String cleanedContent = document.getContent().replace("\u0000", "");
        
        // Create a new Document with the cleaned content
        return new Document(cleanedContent, document.getMetadata());
    }
}