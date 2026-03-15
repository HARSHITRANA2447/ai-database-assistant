package com.aidb.service;

import com.aidb.dto.QueryRequest;
import com.aidb.dto.QueryResponse;
import com.aidb.model.QueryHistory;
import com.aidb.repository.QueryHistoryRepository;
import com.aidb.utils.SQLValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class QueryService {

    private static final Logger log = LoggerFactory.getLogger(QueryService.class);

    private final AIService aiService;
    private final SQLValidator sqlValidator;
    private final QueryHistoryRepository historyRepo;
    private final DataSource dataSource;          // ✅ DataSource — no Hibernate unwrap needed
    private final SchemaService schemaService;

    public QueryService(AIService aiService, SQLValidator sqlValidator,
                        QueryHistoryRepository historyRepo,
                        DataSource dataSource,
                        SchemaService schemaService) {
        this.aiService     = aiService;
        this.sqlValidator  = sqlValidator;
        this.historyRepo   = historyRepo;
        this.dataSource    = dataSource;
        this.schemaService = schemaService;
    }

    public QueryResponse processQuery(QueryRequest request) {
        long startTime = System.currentTimeMillis();
        String username = getCurrentUsername();

        QueryHistory history = new QueryHistory();
        history.setUsername(username);
        history.setUserQuery(request.getQuery());
        history.setTimestamp(LocalDateTime.now());

        try {
            // Step 1: NL → SQL
            String sql = aiService.convertToSQL(request.getQuery());
            if (sql == null || sql.contains("INVALID_REQUEST")) {
                history.setExecutionStatus("FAILED");
                history.setErrorMessage("AI could not convert query to SQL.");
                historyRepo.save(history);
                return QueryResponse.builder()
                        .success(false)
                        .userQuery(request.getQuery())
                        .error("I couldn't understand your query. Please try rephrasing it.")
                        .suggestions(aiService.generateSuggestions(request.getQuery()))
                        .build();
            }

            sql = sqlValidator.sanitizeSQL(sql);
            history.setGeneratedSql(sql);

            // Step 2: Security check
            SQLValidator.ValidationResult validation = sqlValidator.validate(sql);
            if (!validation.valid()) {
                history.setExecutionStatus("BLOCKED");
                history.setErrorMessage(validation.message());
                historyRepo.save(history);
                return QueryResponse.builder()
                        .success(false).userQuery(request.getQuery())
                        .generatedSql(sql)
                        .error("Query blocked: " + validation.message())
                        .build();
            }

            // Step 3: Execute
            String queryType = sqlValidator.detectQueryType(sql);
            history.setQueryType(queryType);
            List<Map<String, Object>> results = executeQuery(sql);
            long execTime = System.currentTimeMillis() - startTime;
            history.setRowCount(results.size());
            history.setExecutionTimeMs(execTime);
            history.setExecutionStatus("SUCCESS");

            // Step 4: Optional explanation
            String explanation = null;
            if (request.isExplain()) {
                explanation = aiService.explainSQL(sql);
                history.setAiExplanation(explanation);
            }

            // Step 5: Insight for aggregations
            String insight = null;
            if ("AGGREGATION".equals(queryType) || "GROUP BY".equals(queryType)) {
                insight = aiService.generateInsight(request.getQuery(), results);
            }

            historyRepo.save(history);

            List<String> columns = results.isEmpty()
                    ? List.of() : new ArrayList<>(results.get(0).keySet());
            boolean hasNumeric = hasNumericData(results);
            String chartType   = determineChartType(queryType, columns);

            return QueryResponse.builder()
                    .success(true)
                    .userQuery(request.getQuery())
                    .generatedSql(sql)
                    .explanation(explanation)
                    .queryType(queryType)
                    .results(results)
                    .columns(columns)
                    .rowCount(results.size())
                    .executionTimeMs(execTime)
                    .chartType(chartType)
                    .hasNumericData(hasNumeric)
                    .insight(insight)
                    .build();

        } catch (Exception e) {
            log.error("Query execution failed", e);
            long execTime = System.currentTimeMillis() - startTime;

            // Auto-correction attempt
            if (history.getGeneratedSql() != null) {
                try {
                    String correctedSql = aiService.correctSQL(
                            history.getGeneratedSql(), e.getMessage(),
                            schemaService.getSchemaContext());
                    if (correctedSql != null && sqlValidator.validate(correctedSql).valid()) {
                        List<Map<String, Object>> results = executeQuery(correctedSql);
                        history.setGeneratedSql(correctedSql + " [AUTO-CORRECTED]");
                        history.setExecutionStatus("SUCCESS");
                        history.setRowCount(results.size());
                        historyRepo.save(history);
                        List<String> cols = results.isEmpty()
                                ? List.of() : new ArrayList<>(results.get(0).keySet());
                        return QueryResponse.builder()
                                .success(true).userQuery(request.getQuery())
                                .generatedSql(correctedSql + " [AUTO-CORRECTED]")
                                .results(results).columns(cols)
                                .rowCount(results.size()).executionTimeMs(execTime)
                                .insight("⚡ Query was automatically corrected.")
                                .build();
                    }
                } catch (Exception ignored) {}
            }

            history.setExecutionStatus("FAILED");
            history.setErrorMessage(e.getMessage());
            historyRepo.save(history);
            return QueryResponse.builder()
                    .success(false).userQuery(request.getQuery())
                    .generatedSql(history.getGeneratedSql())
                    .error("Execution failed: " + e.getMessage())
                    .build();
        }
    }

    // ✅ Uses DataSource directly — works with Hibernate 6 / Spring Boot 3
    private List<Map<String, Object>> executeQuery(String sql) {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            String[] colNames = new String[cols];
            for (int i = 1; i <= cols; i++) colNames[i - 1] = meta.getColumnLabel(i);

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= cols; i++) row.put(colNames[i - 1], rs.getObject(i));
                result.add(row);
            }
        } catch (Exception e) {
            throw new RuntimeException("SQL execution error: " + e.getMessage(), e);
        }
        return result;
    }

    private boolean hasNumericData(List<Map<String, Object>> results) {
        if (results.isEmpty()) return false;
        return results.get(0).values().stream().anyMatch(v -> v instanceof Number);
    }

    private String determineChartType(String queryType, List<String> columns) {
        if ("AGGREGATION".equals(queryType)) return "bar";
        if ("GROUP BY".equals(queryType))    return "pie";
        if (columns.stream().anyMatch(c -> {
            String cl = c.toLowerCase();
            return cl.contains("date") || cl.contains("month") || cl.contains("year");
        })) return "line";
        return "table";
    }

    private String getCurrentUsername() {
        try { return SecurityContextHolder.getContext().getAuthentication().getName(); }
        catch (Exception e) { return "anonymous"; }
    }
}
