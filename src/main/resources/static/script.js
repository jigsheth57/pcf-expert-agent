// URL of your Spring Boot endpoint
const RAG_ENDPOINT = '/api/expert-rag'; 

/**
 * Handles the logic for sending the question and receiving the expert's response.
 */
async function askExpert() {
    const inputElement = document.getElementById('questionInput');
    const responseArea = document.getElementById('responseArea');
    const submitButton = document.getElementById('submitButton');
    
    const question = inputElement.value.trim();

    if (!question) {
        alert('Please enter a question for the expert.');
        return;
    }

    // 1. Disable input and show loading state
    submitButton.disabled = true;
    submitButton.textContent = 'Analyzing Documentation...';
    responseArea.innerHTML = '<p>Querying Ollama and PGVector...</p>';
    
    try {
        // 2. Make the API call
        const response = await fetch(`${RAG_ENDPOINT}?message=${encodeURIComponent(question)}`);

        if (!response.ok) {
            // Handle HTTP errors (e.g., 500 server error)
            throw new Error(`HTTP Error: ${response.status} ${response.statusText}`);
        }

        // 3. Get the response text
        const expertAnswer = await response.text();

        // 4. Display the answer
        responseArea.innerHTML = expertAnswer;
        responseArea.classList.remove('error');

    } catch (error) {
        // 5. Display error message
        console.error('RAG Query Failed:', error);
        responseArea.innerHTML = `<p class="error">An error occurred: ${error.message}. Please check your Spring Boot logs for details.</p>`;
        responseArea.classList.add('error');

    } finally {
        // 6. Re-enable the button
        submitButton.disabled = false;
        submitButton.textContent = 'Ask Expert';
    }
}