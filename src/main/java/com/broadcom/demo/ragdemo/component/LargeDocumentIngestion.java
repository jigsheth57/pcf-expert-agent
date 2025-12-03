package com.broadcom.demo.ragdemo.component;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class LargeDocumentIngestion {

    private final VectorStore vectorStore;

    // Inject the PDF resource from the resources folder
    @Value("classpath:/tas-for-vms-6.pdf")
    private Resource taspdfResource;

    @Value("classpath:/tanzu-ops-manager-3-2.pdf")
    private Resource opsmanpdfResource;

    public LargeDocumentIngestion(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public String ingestPdf() {
        if (!taspdfResource.exists()) {
            return "FATAL ERROR: PDF file not found at " + taspdfResource.getFilename();
        }

        if (!opsmanpdfResource.exists()) {
            return "FATAL ERROR: PDF file not found at " + opsmanpdfResource.getFilename();
        }

        System.out.println("⏳ Starting ingestion of large PDF document...");

        // 1. Read the PDF
        // Use PagePdfDocumentReader to read the document, with each page as a Document
        PagePdfDocumentReader[] pdfReader = {new PagePdfDocumentReader(taspdfResource),new PagePdfDocumentReader(opsmanpdfResource)};

        // 2. Chunk the Documents
        // TokenTextSplitter is essential for large docs.
        // We use a small overlap (100 tokens) to preserve context across splits.
        // Use the builder to set the chunk size and overlap
        TokenTextSplitter textSplitter = TokenTextSplitter.builder().withChunkSize(550).withMinChunkSizeChars(150).withKeepSeparator(false).build();

        for (PagePdfDocumentReader pagePdfDocumentReader : pdfReader) {
            // Read, split, and get the final list of chunks
            List<Document> documents = pagePdfDocumentReader.get()
                                                .stream()
                                                .flatMap(doc -> textSplitter.split(doc).stream())
                                                .map(this::sanitizeDocumentContent)
                                                .toList();

            System.out.printf("📄 PDF Read. Total documents/chunks created: %d%n", documents.size());

            // 3. Store the Embeddings
            // The add() method automatically embeds the documents using the configured OllamaEmbeddingModel
            vectorStore.add(documents);            
        }

        return "✅ Ingestion complete. Documents embedded and stored in PGVector.";
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
