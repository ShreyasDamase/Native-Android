# Backend Engineering with Spring Boot & Kotlin

## The HireStory Builder's Guide

---

# Chapter 9 — Spring AI: Extracting Structured Data From Raw Text

### _Turning a Reddit post into a structured interview experience — automatically_

---

## 9.1 The Problem This Chapter Solves

Your crawler (Chapter 8) finds this Reddit post:

```
Title: Google L3 Interview Experience - Bangalore 2024

So I finally cracked Google after 3 attempts. Here's what happened:

Applied through a referral in January. Got a call from HR in 2 weeks.

Round 1 - Online Assessment (90 mins):
Two coding problems. First was a medium graph problem - BFS traversal.
Second was a hard DP problem on intervals. I solved both but the DP
took me 85 minutes. Not sure how I passed honestly.

Round 2 - Technical Phone Screen:
Talked to an SWE for 45 mins. She asked me about my projects first,
then gave me a medium array problem. Also asked about time complexity.
She was super helpful and gave hints when I was stuck.

Round 3, 4, 5 - Onsite (Virtual):
Three back-to-back interviews. One system design, two coding.
System design was design a URL shortener. Coding was one hard tree
problem and one medium string problem.

Round 6 - Hiring Manager:
30 mins, mostly behavioral. STAR format questions. Why Google, what
are your strengths, a conflict situation.

Got the offer 2 weeks later. 28 LPA base + 15% bonus + RSUs.
Role: SWE L3. Location: Bangalore.
```

From this raw text you need to extract:

```json
{
  "company": "Google",
  "role": "Software Engineer L3",
  "location": "Bangalore",
  "experienceYears": 2,
  "difficulty": "HARD",
  "outcome": "OFFER",
  "salaryLpa": 28.0,
  "roundsCount": 6,
  "rounds": [
    { "roundNumber": 1, "title": "Online Assessment", "questions": "BFS traversal, DP on intervals" },
    { "roundNumber": 2, "title": "Technical Phone Screen", "questions": "Array problem, time complexity" },
    ...
  ],
  "tags": ["DSA", "System Design", "Behavioral"],
  "confidenceScore": 95
}
```

Doing this with regex or rule-based code would take months and still fail on edge cases. GPT-4o Mini does it in 2 seconds for less than $0.001 per extraction.

This chapter teaches you how to wire Spring AI into HireStory, write prompts that extract reliably, and build the auto-publish pipeline that turns crawled content into live interviews.

---

## 9.2 Spring AI — What It Is

Spring AI is Spring's official library for integrating AI models into Spring Boot applications. It provides:

- A unified `ChatClient` API that works with OpenAI, Anthropic, Google, and others
- **Structured output** — ask the AI to return a specific Kotlin data class
- Token counting, retry logic, and observability built in
- Prompt templates — reusable prompts with variable substitution

You chose GPT-4o Mini. It is:

- Fast — 2-3 second response for extraction tasks
- Cheap — ~$0.15 per million input tokens, ~$0.60 per million output tokens
- Good enough — structured data extraction does not need GPT-4o's reasoning power
- Available via OpenAI's Batch API for 50% discount on high-volume processing

---

## 9.3 Dependencies

```kotlin
// build.gradle.kts

// Spring AI BOM — manages all Spring AI dependency versions together
dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:1.0.0")
    }
}

dependencies {
    // Spring AI OpenAI integration
    implementation("org.springframework.ai:spring-ai-openai-spring-boot-starter")

    // Already have: jackson, spring-boot-starter-web, etc.
}
```

---

## 9.4 Configuration

```yaml
# application.yml

spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o-mini
          temperature: 0.1      # Low temperature = more deterministic output
                                # We want consistent extraction, not creativity
          max-tokens: 2000      # Enough for a full extraction response

hirestory:
  ai:
    extraction:
      min-confidence-score: 50   # Below this: mark as FAILED, do not create interview
      auto-publish-threshold: 80 # Above this: publish automatically, no human review
      max-retries: 3             # Retry failed AI calls this many times
```

Update `HireStoryProperties`:

```kotlin
data class HireStoryProperties(
    val clerk: ClerkProperties,
    val freeTier: FreeTierProperties,
    val crawler: CrawlerProperties,
    val ai: AiProperties        // Add this
) {
    data class AiProperties(
        val extraction: ExtractionProperties
    )

    data class ExtractionProperties(
        @DefaultValue("50") val minConfidenceScore: Int,
        @DefaultValue("80") val autoPublishThreshold: Int,
        @DefaultValue("3") val maxRetries: Int
    )
}
```

---

## 9.5 The Extraction Response — What You Ask the AI to Return

Define the exact Kotlin data class you want the AI to populate. Spring AI uses this as the schema for structured output:

```kotlin
// src/main/kotlin/com/example/hirestory/ai/ExtractionModels.kt

package com.example.hirestory.ai

import com.fasterxml.jackson.annotation.JsonProperty

// The complete extraction result from the AI
// Every field is nullable — the AI might not find all information
data class InterviewExtractionResult(

    @JsonProperty("company")
    val company: String?,          // "Google", "Amazon", "Microsoft"

    @JsonProperty("role")
    val role: String?,             // "Software Engineer L3", "SDE-1"

    @JsonProperty("location")
    val location: String?,         // "Bangalore", "Hyderabad", "Remote"

    @JsonProperty("experience_years")
    val experienceYears: Int?,     // Years of experience the candidate had

    @JsonProperty("difficulty")
    val difficulty: String?,       // "EASY", "MEDIUM", or "HARD"

    @JsonProperty("outcome")
    val outcome: String?,          // "OFFER", "REJECTED", or "GHOSTED"

    @JsonProperty("salary_lpa")
    val salaryLpa: Double?,        // Annual salary in LPA — null if not mentioned

    @JsonProperty("rounds_count")
    val roundsCount: Int?,         // Total number of interview rounds

    @JsonProperty("rounds")
    val rounds: List<RoundExtraction>,

    @JsonProperty("tags")
    val tags: List<String>,        // ["DSA", "System Design", "HR", "SQL", "Behavioral"]

    @JsonProperty("headline")
    val headline: String?,         // 1-2 sentence summary of the experience

    @JsonProperty("confidence_score")
    val confidenceScore: Int,      // 0-100: how confident AI is this is a real interview post

    @JsonProperty("rejection_reason")
    val rejectionReason: String?   // Why confidence is low — if confidenceScore < 50
)

data class RoundExtraction(
    @JsonProperty("round_number")
    val roundNumber: Int,

    @JsonProperty("title")
    val title: String,             // "Online Assessment", "System Design", "HR Round"

    @JsonProperty("questions")
    val questions: String,         // Description of what was asked

    @JsonProperty("difficulty")
    val difficulty: String?,       // "EASY", "MEDIUM", "HARD" for this round

    @JsonProperty("notes")
    val notes: String?             // Any tips or observations about this round
)
```

---

## 9.6 The Extraction Prompt — The Most Important Thing in This Chapter

The quality of your extracted data depends entirely on your prompt. A bad prompt gives you garbage data that silently passes into your database. A good prompt gives you reliable, structured, accurate data.

Spend more time on your prompt than on any other part of this chapter.

```kotlin
// src/main/kotlin/com/example/hirestory/ai/ExtractionPrompt.kt

package com.example.hirestory.ai

object ExtractionPrompt {

    val SYSTEM_PROMPT = """
        You are a data extraction specialist for HireStory, a platform that aggregates
        software engineering interview experiences in India.
        
        Your job is to extract structured information from raw text that may contain
        interview experiences. You must be accurate, conservative, and honest.
        
        CRITICAL RULES:
        1. Only extract information that is explicitly stated in the text.
           Never infer, guess, or hallucinate data that is not there.
        2. If a field cannot be determined from the text, return null for that field.
           Do NOT make up values.
        3. The confidence_score is your most important output.
           Be honest about how confident you are.
        4. You MUST return valid JSON matching the exact schema provided.
           No markdown, no explanation text, just the JSON object.
        
        CONFIDENCE SCORE GUIDE:
        90-100: Clearly a real interview experience. Company named, rounds described,
                specific questions mentioned, outcome stated.
        70-89:  Probably an interview experience. Some details missing but overall
                structure matches an interview post.
        50-69:  Possibly an interview experience. Vague, missing key details,
                or could be something else.
        20-49:  Probably NOT an interview experience. Asking for advice, meme,
                off-topic discussion, news article.
        0-19:   Definitely NOT an interview experience. Spam, unrelated content,
                or too little text to determine anything.
        
        VALID VALUES:
        - difficulty: must be exactly "EASY", "MEDIUM", or "HARD"
        - outcome: must be exactly "OFFER", "REJECTED", or "GHOSTED"
        - tags: only from this list: ["DSA", "System Design", "HR", "Behavioral",
                "SQL", "LLD", "HLD", "OS", "DBMS", "Networking", "Frontend",
                "Backend", "DevOps", "ML", "Puzzle"]
        
        EXPERIENCE YEARS:
        Look for phrases like "2 YOE", "fresher", "3 years experience",
        "final year student". Fresher/campus = 0.
        
        SALARY:
        Convert to LPA (Lakhs Per Annum). If stated in CTC, use that.
        If monthly, multiply by 12. If USD, do not convert — return null.
    """.trimIndent()

    fun buildUserPrompt(rawText: String, sourceUrl: String): String = """
        Extract interview experience data from the following text.
        
        Source URL: $sourceUrl
        
        --- BEGIN TEXT ---
        ${rawText.take(8000)}
        --- END TEXT ---
        
        Return ONLY a JSON object with this exact structure. No other text:
        {
          "company": string or null,
          "role": string or null,
          "location": string or null,
          "experience_years": integer or null,
          "difficulty": "EASY" | "MEDIUM" | "HARD" or null,
          "outcome": "OFFER" | "REJECTED" | "GHOSTED" or null,
          "salary_lpa": number or null,
          "rounds_count": integer or null,
          "rounds": [
            {
              "round_number": integer,
              "title": string,
              "questions": string,
              "difficulty": string or null,
              "notes": string or null
            }
          ],
          "tags": string[],
          "headline": string or null,
          "confidence_score": integer (0-100),
          "rejection_reason": string or null
        }
    """.trimIndent()
}
```

### Why These Specific Rules in the Prompt

**"Only extract information explicitly stated"** — Without this, GPT will make educated guesses. A post mentioning "I work at a tech company in Bangalore" might get `"company": "Infosys"` hallucinated. That is real data corruption.

**"Return null, not a guess"** — Reinforces the above. Null is honest. A wrong value is dangerous.

**"No markdown, just JSON"** — GPT loves wrapping JSON in code blocks: ` ```json {...} ``` `. Your parser will fail silently if you do not tell it explicitly to skip the markdown wrapper.

**The confidence score guide** — Without specific guidance, GPT gives everything 85-95. The guide calibrates it to be genuinely useful.

**Valid values list** — Prevents GPT from returning `"MEDIUM-HARD"` or `"got offer"` instead of `"OFFER"`. Enums need to be explicitly constrained.

---

## 9.7 The AI Extraction Service

```kotlin
// src/main/kotlin/com/example/hirestory/ai/AiExtractionService.kt

package com.example.hirestory.ai

import com.example.hirestory.config.HireStoryProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.stereotype.Service

@Service
class AiExtractionService(
    private val chatClient: ChatClient,
    private val objectMapper: ObjectMapper,
    private val properties: HireStoryProperties
) {
    private val log = LoggerFactory.getLogger(AiExtractionService::class.java)

    // Main extraction method — called from CrawlConsumer
    fun extract(rawText: String, sourceUrl: String): InterviewExtractionResult {
        if (rawText.isBlank()) {
            return failedExtraction("Empty raw text", 0)
        }

        var lastException: Exception? = null
        val maxRetries = properties.ai.extraction.maxRetries

        // Retry loop — AI calls can fail transiently
        repeat(maxRetries) { attempt ->
            try {
                val result = callAi(rawText, sourceUrl)
                log.info("Extraction successful for {}: confidence={}, company={}",
                    sourceUrl.take(60), result.confidenceScore, result.company)
                return result
            } catch (e: Exception) {
                lastException = e
                log.warn("AI extraction attempt {}/{} failed for {}: {}",
                    attempt + 1, maxRetries, sourceUrl.take(60), e.message)

                if (attempt < maxRetries - 1) {
                    // Exponential backoff between retries: 1s, 2s, 4s
                    Thread.sleep(1000L * Math.pow(2.0, attempt.toDouble()).toLong())
                }
            }
        }

        log.error("AI extraction failed after {} attempts for {}: {}",
            maxRetries, sourceUrl.take(60), lastException?.message)
        return failedExtraction("AI call failed after $maxRetries attempts: ${lastException?.message}", 0)
    }

    private fun callAi(rawText: String, sourceUrl: String): InterviewExtractionResult {
        // Build the prompt with system instructions and user content
        val prompt = Prompt(listOf(
            SystemMessage(ExtractionPrompt.SYSTEM_PROMPT),
            UserMessage(ExtractionPrompt.buildUserPrompt(rawText, sourceUrl))
        ))

        // Call the AI — this is where the OpenAI API is hit
        val response = chatClient.prompt(prompt).call().content()
            ?: throw IllegalStateException("AI returned empty response")

        // Parse the JSON response into our data class
        return parseResponse(response)
    }

    private fun parseResponse(rawResponse: String): InterviewExtractionResult {
        // Clean the response — remove markdown code blocks if AI ignored our instructions
        val cleaned = rawResponse
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return try {
            objectMapper.readValue<InterviewExtractionResult>(cleaned)
        } catch (e: Exception) {
            log.error("Failed to parse AI response as JSON: {}", cleaned.take(200))
            throw IllegalStateException("AI returned invalid JSON: ${e.message}")
        }
    }

    private fun failedExtraction(reason: String, confidence: Int) = InterviewExtractionResult(
        company = null,
        role = null,
        location = null,
        experienceYears = null,
        difficulty = null,
        outcome = null,
        salaryLpa = null,
        roundsCount = null,
        rounds = emptyList(),
        tags = emptyList(),
        headline = null,
        confidenceScore = confidence,
        rejectionReason = reason
    )
}
```

---

## 9.8 The ChatClient Bean

Spring AI's `ChatClient` needs to be configured as a bean. Spring Boot auto-configures a `ChatClient.Builder` — use it:

```kotlin
// src/main/kotlin/com/example/hirestory/config/AiConfig.kt

package com.example.hirestory.config

import org.springframework.ai.chat.client.ChatClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AiConfig {

    @Bean
    fun chatClient(builder: ChatClient.Builder): ChatClient {
        return builder
            // Default system prompt — can be overridden per call
            .defaultSystem("You are a helpful data extraction assistant.")
            .build()
    }
}
```

---

## 9.9 Processing the Extraction Result

After the AI returns a result, you need to decide what to do with it: create an interview, send to review, or discard it.

```kotlin
// src/main/kotlin/com/example/hirestory/ai/ExtractionProcessor.kt

package com.example.hirestory.ai

import com.example.hirestory.config.HireStoryProperties
import com.example.hirestory.entity.*
import com.example.hirestory.repository.*
import com.example.hirestory.service.toSlug
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ExtractionProcessor(
    private val interviewRepository: InterviewRepository,
    private val companyRepository: CompanyRepository,
    private val tagRepository: TagRepository,
    private val crawlJobRepository: CrawlJobRepository,
    private val properties: HireStoryProperties
) {
    private val log = LoggerFactory.getLogger(ExtractionProcessor::class.java)

    @Transactional
    fun process(crawlJobId: Long, result: InterviewExtractionResult, sourceUrl: String) {
        val crawlJob = crawlJobRepository.findById(crawlJobId).orElseThrow()
        val threshold = properties.ai.extraction
        val score = result.confidenceScore

        when {
            // Below minimum — not an interview post at all
            score < threshold.minConfidenceScore -> {
                log.info("Crawl job {} rejected: confidence={}, reason={}",
                    crawlJobId, score, result.rejectionReason)
                crawlJob.status = CrawlStatus.FAILED
                crawlJob.confidenceScore = score
                crawlJob.errorMessage = "Low confidence: ${result.rejectionReason}"
                crawlJobRepository.save(crawlJob)
            }

            // Above auto-publish threshold — create and publish immediately
            score >= threshold.autoPublishThreshold -> {
                log.info("Auto-publishing crawl job {}: confidence={}", crawlJobId, score)
                val interview = createInterview(result, sourceUrl, InterviewStatus.PUBLISHED)

                crawlJob.status = CrawlStatus.DONE
                crawlJob.confidenceScore = score
                crawlJob.interview = interview
                crawlJob.processedAt = LocalDateTime.now()
                crawlJobRepository.save(crawlJob)

                // Update company's interview count
                companyRepository.incrementInterviewCount(interview.company.id!!)
            }

            // Between thresholds — create as PENDING for human review
            else -> {
                log.info("Crawl job {} requires review: confidence={}", crawlJobId, score)
                val interview = createInterview(result, sourceUrl, InterviewStatus.PENDING)

                crawlJob.status = CrawlStatus.DONE
                crawlJob.confidenceScore = score
                crawlJob.interview = interview
                crawlJob.processedAt = LocalDateTime.now()
                crawlJobRepository.save(crawlJob)
            }
        }
    }

    private fun createInterview(
        result: InterviewExtractionResult,
        sourceUrl: String,
        status: InterviewStatus
    ): Interview {
        // Validate required fields — if missing, we cannot create a useful interview
        val companyName = result.company?.trim()
            ?: throw IllegalStateException("AI extraction missing company name")
        val role = result.role?.trim()
            ?: "Software Engineer"   // Default if role not found
        val difficulty = parseDifficulty(result.difficulty) ?: Difficulty.MEDIUM
        val outcome = parseOutcome(result.outcome) ?: Outcome.OFFER

        // Find or create company
        val company = companyRepository.findByName(companyName)
            ?: companyRepository.save(
                Company(name = companyName, slug = companyName.toSlug())
            )

        // Find or create tags
        val tags = result.tags
            .filter { it.isNotBlank() }
            .map { tagName ->
                tagRepository.findByName(tagName)
                    ?: tagRepository.save(Tag(name = tagName))
            }

        // Generate unique slug
        val baseSlug = "${companyName.toSlug()}-${role.toSlug()}"
        val slug = ensureUniqueSlug(baseSlug)

        val headline = result.headline
            ?: generateDefaultHeadline(companyName, role, outcome)

        // Build and save the interview
        val interview = Interview(
            title = buildTitle(companyName, role, result.location),
            headline = headline.take(500),
            content = buildContent(result),
            role = role,
            location = result.location,
            experienceYears = result.experienceYears ?: 0,
            difficulty = difficulty,
            outcome = outcome,
            salaryLpa = result.salaryLpa,
            slug = slug,
            status = status,
            sourceType = SourceType.CRAWLED,
            sourceUrl = sourceUrl,
            confidenceScore = result.confidenceScore,
            company = company,
            user = null   // Crawled interviews have no user
        )
        interview.tags.addAll(tags)

        val saved = interviewRepository.save(interview)

        // Save the rounds
        if (result.rounds.isNotEmpty()) {
            val rounds = result.rounds.map { roundData ->
                InterviewRound(
                    interview = saved,
                    roundNumber = roundData.roundNumber,
                    title = roundData.title,
                    questions = roundData.questions,
                    difficulty = roundData.difficulty,
                    notes = roundData.notes
                )
            }
            saved.rounds.addAll(interviewRoundRepository.saveAll(rounds))
            saved.roundsCount = rounds.size
            interviewRepository.save(saved)
        }

        log.info("Created interview {} from crawl: {} at {} — {}",
            saved.id, companyName, result.location, outcome)

        return saved
    }

    // Build a readable title from extracted data
    private fun buildTitle(company: String, role: String, location: String?): String {
        val year = java.time.LocalDate.now().year
        return if (location != null) {
            "$company $role Interview Experience — $location $year"
        } else {
            "$company $role Interview Experience — $year"
        }
    }

    // Build structured content from extraction result
    // This is what the app displays to the user
    private fun buildContent(result: InterviewExtractionResult): String {
        val sb = StringBuilder()

        if (result.experienceYears != null) {
            sb.appendLine("**Experience:** ${result.experienceYears} years")
        }
        if (result.salaryLpa != null) {
            sb.appendLine("**Offered CTC:** ${result.salaryLpa} LPA")
        }
        sb.appendLine()

        if (result.rounds.isNotEmpty()) {
            result.rounds.forEach { round ->
                sb.appendLine("### Round ${round.roundNumber}: ${round.title}")
                sb.appendLine(round.questions)
                if (round.notes != null) {
                    sb.appendLine("*${round.notes}*")
                }
                sb.appendLine()
            }
        }

        return sb.toString().trim()
    }

    private fun generateDefaultHeadline(
        company: String,
        role: String,
        outcome: Outcome
    ): String {
        val outcomeText = when (outcome) {
            Outcome.OFFER -> "received an offer"
            Outcome.REJECTED -> "was rejected"
            Outcome.GHOSTED -> "was ghosted"
        }
        return "A candidate $outcomeText after interviewing for $role at $company."
    }

    private fun parseDifficulty(value: String?): Difficulty? = when (value?.uppercase()) {
        "EASY" -> Difficulty.EASY
        "MEDIUM" -> Difficulty.MEDIUM
        "HARD" -> Difficulty.HARD
        else -> null
    }

    private fun parseOutcome(value: String?): Outcome? = when (value?.uppercase()) {
        "OFFER" -> Outcome.OFFER
        "REJECTED" -> Outcome.REJECTED
        "GHOSTED" -> Outcome.GHOSTED
        else -> null
    }

    private fun ensureUniqueSlug(baseSlug: String): String {
        if (!interviewRepository.existsBySlug(baseSlug)) return baseSlug
        var counter = 2
        while (interviewRepository.existsBySlug("$baseSlug-$counter")) counter++
        return "$baseSlug-$counter"
    }
}
```

---

## 9.10 Updating the Crawl Consumer

Now update the `CrawlConsumer` from Chapter 7 to use real AI extraction:

```kotlin
// Update CrawlConsumer.kt

@Component
class CrawlConsumer(
    private val crawlJobRepository: CrawlJobRepository,
    private val aiExtractionService: AiExtractionService,
    private val extractionProcessor: ExtractionProcessor   // New dependency
) {
    private val log = LoggerFactory.getLogger(CrawlConsumer::class.java)

    @RabbitListener(queues = [RabbitMQConfig.CRAWL_QUEUE], ackMode = "MANUAL")
    fun processCrawlJob(
        message: CrawlJobMessage,
        channel: Channel,
        @Header(AmqpHeaders.DELIVERY_TAG) deliveryTag: Long
    ) {
        val jobId = message.crawlJobId
        log.info("Processing crawl job {}: {}", jobId, message.sourceUrl.take(60))

        val crawlJob = crawlJobRepository.findByIdOrNull(jobId)
        if (crawlJob == null) {
            channel.basicAck(deliveryTag, false)
            return
        }

        try {
            // Mark as processing
            crawlJob.status = CrawlStatus.PROCESSING
            crawlJobRepository.save(crawlJob)

            // Step 1: Call AI for extraction
            val extractionResult = aiExtractionService.extract(
                rawText = message.rawText,
                sourceUrl = message.sourceUrl
            )

            // Step 2: Process the result — create interview or discard
            extractionProcessor.process(
                crawlJobId = jobId,
                result = extractionResult,
                sourceUrl = message.sourceUrl
            )

            // Step 3: Acknowledge — processing complete
            channel.basicAck(deliveryTag, false)

        } catch (e: Exception) {
            log.error("Crawl job {} failed: {}", jobId, e.message, e)

            crawlJob.status = CrawlStatus.FAILED
            crawlJob.errorMessage = e.message?.take(500)
            crawlJob.processedAt = LocalDateTime.now()
            crawlJobRepository.save(crawlJob)

            channel.basicNack(deliveryTag, false, false)
        }
    }
}
```

---

## 9.11 Cost Control — OpenAI API Usage

GPT-4o Mini is cheap but you are calling it for every crawled URL. At scale this adds up. Here is how to control costs:

### Estimate Before You Run

```kotlin
// Rough calculation for one crawler run
val avgTokensPerPost = 1500      // Input (post content)
val avgResponseTokens = 500      // Output (extraction JSON)
val postsPerRun = 50
val runsPerDay = 4               // Every 6 hours

val dailyInputTokens = avgTokensPerPost * postsPerRun * runsPerDay    // 300,000
val dailyOutputTokens = avgResponseTokens * postsPerRun * runsPerDay  // 100,000

// GPT-4o Mini pricing (approximate):
// Input:  $0.15 per 1M tokens  → $0.045 per day
// Output: $0.60 per 1M tokens  → $0.06 per day
// Total:  ~$0.10 per day = $3/month

// Totally affordable at this scale
```

### Truncate Input

```kotlin
// In ExtractionPrompt.buildUserPrompt():
${rawText.take(8000)}   // Cap at 8000 chars ≈ 2000 tokens
// Full Reddit post is usually 500-2000 chars — this cap rarely triggers
// But protects you from accidentally sending a 50KB document
```

### Pre-filter Before AI

```kotlin
// CrawlerOrchestrator — filter before spending tokens
val filteredResults = allResults.filter { result ->
    // Basic checks before sending to AI
    result.rawContent.length >= 200 &&           // Too short = not useful
    result.rawContent.length <= 50_000 &&         // Too long = truncate or skip
    isLikelyInterviewContent(result.rawContent)  // Keyword pre-check
}
```

```kotlin
private fun isLikelyInterviewContent(text: String): Boolean {
    val lower = text.lowercase()
    val interviewKeywords = listOf(
        "interview", "round", "offer", "rejected", "leetcode",
        "coding", "technical", "hr round", "system design",
        "placed", "selection", "cracked", "cleared"
    )
    return interviewKeywords.count { it in lower } >= 2
}
```

This pre-filter rejects obvious non-interview content before spending a single token. If a post does not contain at least two interview-related keywords, the AI would give it a confidence score of 0-20 anyway — save the cost.

---

## 9.12 Prompt Testing — Before Going to Production

Before you run this on real crawled data, test your prompt manually. This is not optional. You will discover edge cases that break your extraction — better to find them in testing than in production.

### Test Cases You Must Verify

```kotlin
// src/test/kotlin/com/example/hirestory/ai/ExtractionPromptTest.kt

// Test Case 1: Clear interview experience — should be confidence 90+
val clearExperience = """
    I just cracked Google SDE-1. 5 rounds total.
    Round 1: OA - 2 medium problems in 90 mins. BFS and DP.
    Round 2: Phone screen - array and string questions.
    Round 3-5: Onsite - System design + 2 coding rounds.
    Got offer: 28 LPA. Location: Bangalore.
    YOE: 2 years.
"""

// Test Case 2: "Asking for advice" post — should be confidence 0-30
val askingForAdvice = """
    Hey everyone, I have a Google interview coming up next week.
    What should I prepare? Any tips for system design rounds?
    I'm targeting SDE-1 position. Been practicing LeetCode mediums.
"""

// Test Case 3: Incomplete experience — should be confidence 50-70
val incompleteExperience = """
    Had my Amazon interview yesterday. It went okay I think.
    First round was a coding problem, second was behavioral.
    Still waiting for results. The interviewer seemed positive.
"""

// Test Case 4: Off-topic post — should be confidence 0-20
val offTopic = """
    What's the best laptop for programming?
    I'm thinking MacBook Pro M3 or a ThinkPad.
    Budget around 1.5 lakhs.
"""

// Test Case 5: Missing key fields — should extract what exists, null the rest
val partialData = """
    My Microsoft interview went well! 3 coding rounds, all medium difficulty.
    Got through to the hiring manager round. Waiting for decision.
    The questions were mostly on arrays and graphs.
"""
```

Run each test case through your actual `AiExtractionService.extract()` in a unit test and assert:

- Confidence scores are in the expected range
- Extracted fields match what is actually in the text
- Null fields are null (not hallucinated)
- Tags are from the valid list

---

## 9.13 The Deduplication Problem — Same Experience, Multiple Sources

The same interview experience might appear on Reddit AND GeeksForGeeks. After AI extraction, both create identical interviews from different URLs.

Handle this with a post-extraction duplicate check:

```kotlin
// In ExtractionProcessor.createInterview() — check before saving

private fun isDuplicate(
    company: String,
    role: String,
    experienceYears: Int?,
    outcome: Outcome
): Boolean {
    // A duplicate is: same company + same role + same outcome + similar experience years
    // Published in the last 30 days
    val cutoff = LocalDateTime.now().minusDays(30)

    return interviewRepository.existsSimilar(
        companyName = company,
        role = role,
        outcome = outcome,
        addedAfter = cutoff
    )
}
```

Add to `InterviewRepository`:

```kotlin
@Query("""
    SELECT COUNT(i) > 0 FROM Interview i
    JOIN i.company c
    WHERE LOWER(c.name) = LOWER(:companyName)
    AND LOWER(i.role) LIKE LOWER(CONCAT('%', :role, '%'))
    AND i.outcome = :outcome
    AND i.addedAt > :addedAfter
    AND i.sourceType = 'CRAWLED'
""")
fun existsSimilar(
    @Param("companyName") companyName: String,
    @Param("role") role: String,
    @Param("outcome") outcome: Outcome,
    @Param("addedAfter") addedAfter: LocalDateTime
): Boolean
```

---

## 9.14 Monitoring AI Extraction Quality

Add an admin endpoint to see how well extraction is performing:

```kotlin
// In AdminService

fun getExtractionStats(): ExtractionStatsDto {
    val done = crawlJobRepository.countByStatus(CrawlStatus.DONE)
    val failed = crawlJobRepository.countByStatus(CrawlStatus.FAILED)
    val autoPublished = interviewRepository.countByStatusAndSourceType(
        InterviewStatus.PUBLISHED, SourceType.CRAWLED
    )
    val pendingReview = interviewRepository.countByStatusAndSourceType(
        InterviewStatus.PENDING, SourceType.CRAWLED
    )
    val avgConfidence = crawlJobRepository.findAverageConfidenceScore() ?: 0.0

    return ExtractionStatsDto(
        totalProcessed = done + failed,
        totalFailed = failed,
        autoPublished = autoPublished,
        pendingReview = pendingReview,
        successRate = if (done > 0) (autoPublished + pendingReview).toDouble() / done else 0.0,
        averageConfidenceScore = avgConfidence
    )
}

data class ExtractionStatsDto(
    val totalProcessed: Long,
    val totalFailed: Long,
    val autoPublished: Long,
    val pendingReview: Long,
    val successRate: Double,
    val averageConfidenceScore: Double
)
```

Add to `CrawlJobRepository`:

```kotlin
@Query("SELECT AVG(c.confidenceScore) FROM CrawlJob c WHERE c.confidenceScore IS NOT NULL")
fun findAverageConfidenceScore(): Double?
```

---

## 9.15 Re-queuing Stuck PENDING Jobs

Remember Chapter 8 checkpoint question 4 — a crawl job saved to the database but the RabbitMQ publish failed. The job sits as `PENDING` forever with no message in the queue.

Fix it with a recovery scheduler:

```kotlin
// src/main/kotlin/com/example/hirestory/service/CrawlRecoveryScheduler.kt

package com.example.hirestory.service

import com.example.hirestory.entity.CrawlStatus
import com.example.hirestory.messaging.CrawlPublisher
import com.example.hirestory.repository.CrawlJobRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class CrawlRecoveryScheduler(
    private val crawlJobRepository: CrawlJobRepository,
    private val crawlPublisher: CrawlPublisher
) {
    private val log = LoggerFactory.getLogger(CrawlRecoveryScheduler::class.java)

    // Run every 15 minutes — find stuck PENDING jobs and re-queue them
    @Scheduled(fixedDelay = 900_000)
    fun recoverStuckJobs() {
        // Jobs that have been PENDING for more than 10 minutes
        // are likely stuck (the queue message was lost)
        val cutoff = LocalDateTime.now().minusMinutes(10)

        val stuckJobs = crawlJobRepository.findByStatusAndCreatedAtBefore(
            CrawlStatus.PENDING,
            cutoff
        )

        if (stuckJobs.isEmpty()) return

        log.info("Recovery: found {} stuck PENDING crawl jobs — re-queuing", stuckJobs.size)

        stuckJobs.forEach { job ->
            try {
                crawlPublisher.publishCrawlJob(
                    crawlJobId = job.id!!,
                    sourceUrl = job.sourceUrl,
                    rawText = job.rawText ?: ""
                )
                log.info("Re-queued stuck job {}", job.id)
            } catch (e: Exception) {
                log.error("Failed to re-queue stuck job {}: {}", job.id, e.message)
            }
        }
    }
}
```

Add to `CrawlJobRepository`:

```kotlin
fun findByStatusAndCreatedAtBefore(
    status: CrawlStatus,
    cutoff: LocalDateTime
): List<CrawlJob>
```

---

## 9.16 Common Mistakes in Chapter 9

### Mistake 1 — Not testing the prompt before production

```kotlin
// ❌ Write prompt, deploy, discover it hallucinates data on 30% of posts
// Weeks of bad data in your database before you notice

// ✅ Test with 20 real examples before deploying
// Check: confidence score calibration, null vs hallucination, valid enum values
```

### Mistake 2 — Not stripping markdown from AI response

```kotlin
// ❌ AI returns ```json {...} ``` — JSON parse fails
val result = objectMapper.readValue<InterviewExtractionResult>(response)
// Throws: Unrecognized token '`'

// ✅ Strip markdown wrapper first
val cleaned = response.trim()
    .removePrefix("```json")
    .removePrefix("```")
    .removeSuffix("```")
    .trim()
val result = objectMapper.readValue<InterviewExtractionResult>(cleaned)
```

### Mistake 3 — High temperature for extraction tasks

```kotlin
// ❌ Temperature 0.7 — creative, inconsistent output
// Same post gives different fields on different calls
spring:
  ai:
    openai:
      chat:
        options:
          temperature: 0.7

// ✅ Temperature 0.1 — deterministic, consistent output
// Extraction is not a creative task — you want the same answer every time
          temperature: 0.1
```

### Mistake 4 — Creating interviews from low-confidence extractions

```kotlin
// ❌ Trust everything the AI returns — even spam and advice posts
extractionProcessor.process(crawlJobId, result, sourceUrl)
// Creates an interview from "What laptop should I buy?" post

// ✅ Check confidence before creating anything
if (result.confidenceScore < properties.ai.extraction.minConfidenceScore) {
    markAsFailed(crawlJob, "Low confidence: ${result.confidenceScore}")
    return
}
```

### Mistake 5 — Not handling null required fields after extraction

```kotlin
// ❌ NPE when company is null
val company = companyRepository.findByName(result.company!!)
//                                                        ^ NullPointerException

// ✅ Validate required fields, provide defaults or throw cleanly
val companyName = result.company?.trim()
    ?: throw IllegalStateException("Company name missing from extraction")
```

### Mistake 6 — Sending full raw text without truncation

```kotlin
// ❌ A 100KB document costs 25,000 tokens — $0.004 per call
val prompt = ExtractionPrompt.buildUserPrompt(rawText, sourceUrl)  // full rawText

// ✅ Cap input — most interview posts are under 5000 chars anyway
val prompt = ExtractionPrompt.buildUserPrompt(rawText.take(8000), sourceUrl)
```

---

## 9.17 HireStory Connection — What You Built in Chapter 9

By the end of Chapter 9, HireStory has a complete automated content pipeline from URL discovery to published interview:

**The complete flow end to end:**

```
Scheduler fires every 6 hours (Chapter 8)
        ↓
RedditCrawler, GfgCrawler, DevToCrawler discover URLs (Chapter 8)
        ↓
CrawlJobService deduplicates and saves to DB (Chapter 8)
        ↓
CrawlPublisher sends CrawlJobMessage to RabbitMQ (Chapter 7)
        ↓
CrawlConsumer picks up message (Chapter 7)
        ↓
AiExtractionService calls GPT-4o Mini with structured prompt (Chapter 9)
        ↓
ExtractionProcessor decides:
  confidenceScore >= 80  → Create Interview (PUBLISHED) + notify target users
  confidenceScore 50-79  → Create Interview (PENDING) → Admin reviews
  confidenceScore < 50   → Mark CrawlJob FAILED → discard
        ↓
CrawlRecoveryScheduler re-queues any stuck PENDING jobs (Chapter 9)
```

**Supporting infrastructure:**

- `ExtractionPrompt` — system prompt with calibrated confidence guide, valid value constraints, anti-hallucination rules
- `AiExtractionService` — calls OpenAI, retries with exponential backoff, cleans markdown from response, parses JSON safely
- `ExtractionProcessor` — validates required fields, finds/creates company and tags, generates slug, saves interview and rounds, updates crawl job status
- `CrawlRecoveryScheduler` — re-queues jobs stuck in PENDING state
- Admin extraction stats endpoint — monitors success rate and average confidence

**Cost:** approximately $3-5 per month at 50 crawls per 6-hour run.

---

## 9.18 Chapter Project — Build It Before You Move On

### What to build

A working AI extraction pipeline that turns raw text into structured interviews.

**Step 1 — Get an OpenAI API key**

Sign up at platform.openai.com. Add credit ($5 is enough for testing). Set `OPENAI_API_KEY` in your environment.

**Step 2 — Add the Spring AI dependency**

Add `spring-ai-openai-spring-boot-starter` to Gradle. Configure the model and temperature in `application.yml`. Verify the app starts without errors.

**Step 3 — Test the ChatClient directly**

Before writing extraction logic, verify the API works:

```kotlin
@Component
class AiTest(private val chatClient: ChatClient) {
    fun test() {
        val response = chatClient.prompt()
            .user("Say hello in one word.")
            .call()
            .content()
        println(response)   // Should print "Hello"
    }
}
```

**Step 4 — Write and test the extraction prompt**

Run your 5 test cases from Section 9.12 against the actual API. Check confidence scores. Adjust the prompt until calibration is right.

**Step 5 — Wire into the CrawlConsumer**

Update `CrawlConsumer` to use `AiExtractionService`. Manually create a `CrawlJob` in the database with real Reddit content. Publish its ID to the crawl queue via the admin endpoint. Watch it get processed — verify an interview is created.

**Step 6 — Verify the full pipeline**

```
1. POST /api/admin/crawler/trigger  → discovers real Reddit posts
2. Check DB: crawl_jobs table has PENDING entries
3. Watch logs: CrawlConsumer picks up, AI processes
4. Check DB: crawl_jobs are now DONE or FAILED
5. Check DB: interviews table has new PUBLISHED or PENDING entries
6. GET /api/interviews → new interview appears in the feed
```

### Checkpoint questions — answer before moving on

1. Your prompt says `"Return ONLY a JSON object, no markdown."` The AI returns `\```json {...} ````. You strip the markdown and parse successfully. Why does the AI ignore explicit instructions sometimes, and how do you make your code robust against it?
    
2. The AI extracts `"company": "Google India"` from a post that mentions "Google". Your database has `"Google"`. Two company records are created. How do you fix this in `ExtractionProcessor`?
    
3. Your `confidenceScore` threshold for auto-publish is 80. A post about "Google interview tips" (not an experience) scores 72 because it mentions rounds and questions in hypothetical form. It gets created as PENDING and your admin publishes it manually. How do you prevent this — what change to the prompt helps most?
    
4. The `CrawlRecoveryScheduler` runs every 15 minutes and finds 3 jobs stuck as PENDING for over 10 minutes. It re-queues all 3. But actually, 2 of them are currently being processed — the AI call is just taking longer than 10 minutes. What happens? How do you prevent this?
    
5. You run the crawler on 1000 URLs over one week. Average confidence score is 45. What does this tell you about your crawler's content quality, and what two changes would you make?
    

---

_Chapter 10 → Testing — Unit Tests, Integration Tests, and Confidence_

---

> **Book Progress:** Chapter 9 of 15 complete. Chapters ahead: Testing · Deployment · KMP · Android UI · Next.js · Final Integration