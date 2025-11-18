# spring-ai-examples
## Intelligent technical document querying

### Setup in Macos

#### Install Postgres

`brew install postgresql@18`

`brew install pgvector`

`export PATH="/opt/homebrew/opt/postgresql@18/bin:$PATH"`

`export LDFLAGS="-L/opt/homebrew/opt/postgresql@18/lib"`

`export CPPFLAGS="-I/opt/homebrew/opt/postgresql@18/include"`

`brew services start postgresql@18`

#### configure postgres for Vector extension and a user

`psql postgres`

`CREATE ROLE myappuser WITH LOGIN PASSWORD 'mypassword';`

`CREATE DATABASE techdocdb OWNER myappuser;`

`GRANT ALL PRIVILEGES ON DATABASE techdocdb TO myappuser;`

`GRANT USAGE ON SCHEMA public TO myappuser;`

`GRANT CREATE ON SCHEMA public TO myappuser;`

`\c techdocdb`

`CREATE EXTENSION vector;`

`\dx`

`CREATE TABLE vector_store (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    content TEXT, -- The actual text content of the document (your .txt file descriptions)
    metadata JSON, -- The associated metadata (like image_file_name)
    embedding VECTOR(1024)
);
CREATE INDEX ON vector_store USING HNSW (embedding vector_cosine_ops) WITH (M = 16, EF_CONSTRUCTION = 128);`

#### Install Ollama & download LLM models

`brew install ollama`

`brew services start ollama`

`ollama pull mxbai-embed-large`

`ollama pull llama3.2`

