# Chapter 18 — AI, NLP, Vector Search and Spring AI

Book alignment: [[Book Alignment — Pro Spring Boot 3 with Kotlin]]

### _Semantic search, RAG, recommendations, moderation and language workflows in Kotlin Spring Boot_

---

## 18.1 What "NLP Features" Mean in Backend Apps

NLP is not only chatbots. In production backend systems, NLP/AI can power:

- Semantic search: "cheap family hotel near beach" finds relevant listings even without exact words.
- RAG: answer questions from your app's private documents/data.
- Classification: support ticket category, fraud signal, content type.
- Extraction: pull structured fields from invoices, resumes, menus, reviews.
- Moderation: detect abusive content or policy violations.
- Recommendations: similar products, restaurants, listings, articles.
- Summarization: trip summary, order issue summary, admin notes.
- Routing: assign support tickets or delivery issues to the right queue.

---

## 18.2 AI Architecture

```text
App DB / Files / Events
    |
Ingestion Job
    |
Chunking + Metadata
    |
Embedding Model
    |
Vector Store
    |
Retriever
    |
Prompt + LLM
    |
Answer with citations / structured output
```

Important: most AI quality comes from data preparation and retrieval, not from sprinkling an LLM call in a controller.

---

## 18.3 Spring AI Dependencies

Exact dependencies depend on model provider and vector store.

Example shape:

```kotlin
dependencies {
    implementation(platform("org.springframework.ai:spring-ai-bom:1.1.7"))
    implementation("org.springframework.ai:spring-ai-starter-model-openai")
    implementation("org.springframework.ai:spring-ai-advisors-vector-store")
    implementation("org.springframework.ai:spring-ai-starter-vector-store-pgvector")
}
```

Check the official Spring AI docs before creating a new app because model starters, vector-store starters and Boot compatibility evolve quickly. As of the checked docs, Spring AI 1.1.x is the stable line and the docs list 1.1.7 as the latest stable release.

---

## 18.4 Document Chunking

Bad chunk:

```text
Entire 80-page PDF as one embedding.
```

Better chunk:

```text
Small sections with title, source, page, tenantId, accessPolicy, timestamps.
```

Model:

```kotlin
data class KnowledgeChunk(
    val id: UUID,
    val tenantId: UUID,
    val sourceType: String,
    val sourceId: UUID,
    val title: String,
    val content: String,
    val metadata: Map<String, Any>
)
```

Metadata matters because retrieval needs filters:

- tenant id
- user/team permissions
- language
- source type
- freshness
- product/category/city

---

## 18.5 RAG Query Flow

```kotlin
@Service
class SupportAnswerService(
    private val chatClient: ChatClient,
    private val vectorStore: VectorStore
) {
    fun answer(question: String, tenantId: UUID): String {
        val advisor = QuestionAnswerAdvisor.builder(vectorStore)
            .searchRequest(
                SearchRequest.builder()
                    .query(question)
                    .topK(6)
                    .similarityThreshold(0.75)
                    .filterExpression("tenantId == '$tenantId'")
                    .build()
            )
            .build()

        return chatClient.prompt()
            .system("Answer using only the provided context. If context is missing, say you do not know.")
            .user(question)
            .advisors(advisor)
            .call()
            .content()
    }
}
```

Concept explanation:

- `VectorStore`: stores embeddings and metadata.
- `QuestionAnswerAdvisor`: retrieves relevant documents and adds them to the model context.
- `topK`: how many chunks to retrieve.
- `similarityThreshold`: minimum relevance score.
- `filterExpression`: protects tenant/security boundaries.

Security practice: never rely only on prompt text for authorization. Filter retrieval by tenant/user permissions before the LLM sees context.

---

## 18.6 Structured Extraction

For invoices, resumes, menus, booking emails or support messages, prefer structured outputs.

```kotlin
data class ExtractedMenuItem(
    val name: String,
    val description: String?,
    val priceCents: Long?,
    val currency: String?
)

data class ExtractedMenu(
    val restaurantName: String?,
    val items: List<ExtractedMenuItem>
)
```

Service:

```kotlin
fun extractMenu(rawText: String): ExtractedMenu {
    return chatClient.prompt()
        .system("Extract menu data. Return only valid structured data.")
        .user(rawText)
        .call()
        .entity(ExtractedMenu::class.java)
}
```

Good practice:

- Validate extracted output with Bean Validation/domain rules.
- Store raw input and model output for audit if legally allowed.
- Human-review high-risk changes before publishing.
- Never let model output directly change money, inventory or permissions.

---

## 18.7 Hybrid Search

For production search, combine:

- Lexical search: Elasticsearch keyword/full-text.
- Vector search: semantic meaning.
- Business ranking: availability, rating, distance, price, freshness.

Example:

```text
final_score =
    0.45 * text_score +
    0.35 * vector_score +
    0.10 * rating_score +
    0.10 * freshness_score
```

Use hybrid search for:

- Product discovery.
- Restaurant/dish search.
- Hotel/event discovery.
- Similar listings.
- Knowledge base search.

---

## 18.8 AI Safety Checklist

- Apply authorization before retrieval.
- Add tenant/user metadata to every vector.
- Rate-limit AI endpoints.
- Put strict token and cost budgets.
- Log prompt template versions.
- Evaluate answers with test datasets.
- Detect prompt injection in untrusted documents.
- Show citations when answering from private docs.
- Keep deterministic code for business decisions.
- Use human review for high-impact actions.

---

## 18.9 Source Links

- Spring AI RAG: https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html
- Spring AI vector stores: https://docs.spring.io/spring-ai/reference/api/vectordbs.html
- Spring AI getting started/version guidance: https://docs.spring.io/spring-ai/reference/getting-started.html
