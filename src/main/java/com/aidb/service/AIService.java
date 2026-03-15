package com.aidb.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class AIService {

    private static final Logger log = LoggerFactory.getLogger(AIService.class);

    @Value("${ai.provider:groq}")
    private String aiProvider;

    @Value("${ai.api.key:}")
    private String apiKey;

    // Groq
    @Value("${ai.groq.model:llama3-70b-8192}")
    private String groqModel;

    // Anthropic fallback
    @Value("${ai.model:claude-sonnet-4-20250514}")
    private String anthropicModel;

    // Ollama
    @Value("${ai.ollama.url:http://localhost:11434}")
    private String ollamaUrl;

    @Value("${ai.ollama.model:llama3}")
    private String ollamaModel;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SchemaService schemaService;

    public AIService(SchemaService schemaService) {
        this.schemaService = schemaService;
    }

    @PostConstruct
    private void init() {
        // Trim model names to avoid injection issues
        groqModel = groqModel.trim();
        anthropicModel = anthropicModel.trim();
        ollamaModel = ollamaModel.trim();
    }

    // ─── Public API ──────────────────────────────────────────────

    public String convertToSQL(String naturalLanguageQuery) {
        String schema = schemaService.getSchemaContext();
        String prompt = buildSQLPrompt(naturalLanguageQuery, schema);
        try {
            String response = callAI(prompt);
            return extractSQL(response);
        } catch (Exception e) {
            log.error("AI SQL conversion failed: {}", e.getMessage());
            return null;
        }
    }

    public String explainSQL(String sql) {
        String prompt = "Explain this SQL query in simple English in 2-3 sentences. No technical jargon:\n\n" + sql;
        try { return callAI(prompt); }
        catch (Exception e) { return "Could not generate explanation."; }
    }

    public String correctSQL(String failedSql, String errorMessage, String schema) {
        String prompt = String.format("""
            Fix this SQL query that failed. Return ONLY the corrected SQL, nothing else.

            Schema:
            %s

            Failed SQL:
            %s

            Error:
            %s
            """, schema, failedSql, errorMessage);
        try { return extractSQL(callAI(prompt)); }
        catch (Exception e) { return null; }
    }

    public String generateInsight(String query, List<Map<String, Object>> results) {
        if (results == null || results.isEmpty()) return null;
        try {
            String dataPreview = objectMapper.writeValueAsString(
                    results.subList(0, Math.min(10, results.size())));
            String prompt = String.format("""
                Analyze this data and give a 2-sentence business insight. Be specific with numbers.
                Query: %s
                Data: %s
                """, query, dataPreview);
            return callAI(prompt);
        } catch (Exception e) { return null; }
    }

    public List<String> generateSuggestions(String context) {
        String prompt = String.format("""
            Based on this database schema, suggest 5 useful queries a business user might ask.
            Return ONLY a valid JSON array of strings. Example: ["query1","query2"]
            Schema: %s
            """, schemaService.getSchemaContext());
        try {
            String response = callAI(prompt);
            // strip any wrapping text and find the JSON array
            int start = response.indexOf('[');
            int end   = response.lastIndexOf(']');
            if (start >= 0 && end > start) {
                JsonNode node = objectMapper.readTree(response.substring(start, end + 1));
                if (node.isArray()) return objectMapper.convertValue(node, List.class);
            }
        } catch (Exception e) {
            log.warn("Suggestions failed: {}", e.getMessage());
        }
        return List.of(
                "Show all employees",
                "Count total sales",
                "Show top 5 products by revenue",
                "Show employees by department",
                "Show monthly sales trend"
        );
    }

    // ─── Routing ─────────────────────────────────────────────────

    private String callAI(String prompt) throws Exception {
        return switch (aiProvider.toLowerCase()) {
            case "groq"      -> callGroq(prompt);
            case "gemini"    -> callGemini(prompt);
            case "ollama"    -> callOllama(prompt);
            case "anthropic" -> callAnthropic(prompt);
            default          -> callGroq(prompt);
        };
    }

    // ─── Groq (FREE — llama3-70b, mixtral, gemma) ────────────────
    private String callGroq(String prompt) throws Exception {
        Map<String, Object> body = Map.of(
                "model", groqModel,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "max_tokens", 1024,
                "temperature", 0.1   // low temp = more deterministic SQL
        );
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .timeout(Duration.ofSeconds(30))
                .build();
        HttpResponse<String> res = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) {
            log.error("Groq API error {}: {}", res.statusCode(), res.body());
            throw new RuntimeException("Groq API error: " + res.statusCode() + " — " + extractErrorMessage(res.body()));
        }
        return objectMapper.readTree(res.body()).at("/choices/0/message/content").asText();
    }

    // ─── Google Gemini (FREE tier) ────────────────────────────────
    private String callGemini(String prompt) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt))
                ))
        );
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .timeout(Duration.ofSeconds(30))
                .build();
        HttpResponse<String> res = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) throw new RuntimeException("Gemini error: " + res.statusCode());
        return objectMapper.readTree(res.body()).at("/candidates/0/content/parts/0/text").asText();
    }

    // ─── Ollama (100% FREE, local) ────────────────────────────────
    private String callOllama(String prompt) throws Exception {
        Map<String, Object> body = Map.of(
                "model", ollamaModel,
                "prompt", prompt,
                "stream", false
        );
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ollamaUrl + "/api/generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .timeout(Duration.ofSeconds(60))
                .build();
        HttpResponse<String> res = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return objectMapper.readTree(res.body()).at("/response").asText();
    }

    // ─── Anthropic Claude (paid) ──────────────────────────────────
    private String callAnthropic(String prompt) throws Exception {
        Map<String, Object> body = Map.of(
                "model", anthropicModel,
                "max_tokens", 1024,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.anthropic.com/v1/messages"))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .timeout(Duration.ofSeconds(30))
                .build();
        HttpResponse<String> res = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return objectMapper.readTree(res.body()).at("/content/0/text").asText();
    }

    // ─── Helpers ──────────────────────────────────────────────────
    private String buildSQLPrompt(String query, String schema) {
        return String.format("""
            You are an expert SQL assistant. Convert the user's question to a valid SQL SELECT query.

            Database Schema:
            %s

            Rules:
            - Generate ONLY a SELECT statement
            - Never use DROP, DELETE, UPDATE, INSERT, ALTER, TRUNCATE
            - Return ONLY the raw SQL — no explanation, no markdown, no backticks
            - If the request cannot be answered with SELECT, reply: INVALID_REQUEST

            User question: %s

            SQL:
            """, schema, query);
    }

    private String extractSQL(String response) {
        if (response == null) return null;
        response = response.trim()
                .replaceAll("(?i)```sql", "")
                .replaceAll("```", "")
                .trim();
        int idx = response.toUpperCase().indexOf("SELECT");
        if (idx > 0) response = response.substring(idx);
        if (response.endsWith(";")) response = response.substring(0, response.length() - 1).trim();
        return response;
    }

    private String extractErrorMessage(String body) {
        try { return objectMapper.readTree(body).at("/error/message").asText(); }
        catch (Exception e) { return body; }
    }
}
