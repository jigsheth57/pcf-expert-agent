package com.broadcom.demo.firstdemo.config;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
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
    private final EmbeddingModel embeddingModel;
// Key used by most VectorStore implementations to check for a pre-computed embedding
    private static final String EMBEDDING_KEY = "embedding";
    // Inject the PDF resource from the resources folder
    @Value("classpath:/tas-for-vms.pdf")
    private Resource pdfResource; 

    public LargeDocumentIngestion(EmbeddingModel embeddingModel, VectorStore vectorStore) {
        this.embeddingModel = embeddingModel;
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
        // Use the builder to set the chunk size and overlap
        TokenTextSplitter textSplitter = TokenTextSplitter.builder().withChunkSize(550).withMinChunkSizeChars(150).withKeepSeparator(false).build(); 

        // Read, split, and get the final list of chunks
        List<Document> documents = pdfReader.get()
                                            .stream()
                                            .flatMap(doc -> textSplitter.split(doc).stream())
                                            .map(this::sanitizeDocumentContent)
                                            .toList();

        System.out.printf("📄 PDF Read. Total documents/chunks created: %d%n", documents.size());
        
        // 3. Store the Embeddings
        // ingestDocumentsWithPrecomputedEmbeddings(documents);
        // The add() method automatically embeds the documents using the configured OllamaEmbeddingModel
        vectorStore.add(documents);

        System.out.println("✅ Ingestion complete. Documents embedded and stored in PGVector.");
    }


/**
     * Ingests a large list of documents by pre-calculating embeddings in parallel batches
     * and injecting them into the Document metadata to bypass the VectorStore's internal embedding step.
     *
     * @param rawDocuments The list of documents to process and ingest.
     */
    public void ingestDocumentsWithPrecomputedEmbeddings(List<Document> rawDocuments) {
        if (rawDocuments.isEmpty()) {
            System.out.println("No documents provided for ingestion.");
            return;
        }

        // --- 1. PREPARE TEXT CHUNKS FOR BATCHING ---
        // Extract the content from all documents into a single list of strings
        List<String> textChunks = rawDocuments.stream()
                .map(Document::getFormattedContent)
                .collect(Collectors.toList());

        System.out.printf("Starting batch embedding generation for %d documents (chunks).%n", textChunks.size());

        // --- 2. PARALLELIZED/BATCHED EMBEDDING GENERATION ---
        // Spring AI's EmbeddingModel.embed(List<String>) is generally optimized for batching.
        // We call it once for all content to minimize overhead and leverage the model's batch capabilities.
        // For extremely large lists, you might manually chunk this list further before calling embed().
        List<float[]> embeddings = embeddingModel.embed(textChunks);

        if (embeddings.size() != rawDocuments.size()) {
            throw new IllegalStateException("Embedding count does not match document count.");
        }

        System.out.printf("Successfully generated %d embeddings. Proceeding to injection.%n", embeddings.size());

        // --- 3. INJECT EMBEDDINGS INTO DOCUMENT METADATA ---
        List<Document> documentsWithEmbeddings = new ArrayList<>();
        for (int i = 0; i < rawDocuments.size(); i++) {
            Document doc = rawDocuments.get(i);
            float[] vector = embeddings.get(i);
            
            // CRITICAL STEP: Store the vector in the metadata under the 'embedding' key
            doc.getMetadata().put(EMBEDDING_KEY, vector);
            documentsWithEmbeddings.add(doc);
        }

        // --- 4. BYPASS EMBEDDING AND ADD TO VECTORSTORE ---
        // The VectorStore checks the metadata for the 'embedding' key.
        // Since it's present, it skips the slow embedding generation and just performs the database insert.
        System.out.printf("Injecting %d documents into the VectorStore (bypassing embedding step).%n", documentsWithEmbeddings.size());
        vectorStore.add(documentsWithEmbeddings);
        
        System.out.println("Ingestion complete.");
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