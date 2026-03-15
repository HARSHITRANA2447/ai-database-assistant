package com.aidb.controller;

import com.aidb.dto.QueryRequest;
import com.aidb.dto.QueryResponse;
import com.aidb.model.QueryHistory;
import com.aidb.model.SavedQuery;
import com.aidb.repository.QueryHistoryRepository;
import com.aidb.repository.SavedQueryRepository;
import com.aidb.service.AIService;
import com.aidb.service.ExportService;
import com.aidb.service.QueryService;
import com.aidb.service.SchemaService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final QueryService queryService;
    private final QueryHistoryRepository historyRepo;
    private final SavedQueryRepository savedQueryRepo;
    private final AIService aiService;
    private final SchemaService schemaService;
    private final ExportService exportService;

    public ChatController(QueryService queryService, QueryHistoryRepository historyRepo,
                          SavedQueryRepository savedQueryRepo, AIService aiService,
                          SchemaService schemaService, ExportService exportService) {
        this.queryService = queryService;
        this.historyRepo = historyRepo;
        this.savedQueryRepo = savedQueryRepo;
        this.aiService = aiService;
        this.schemaService = schemaService;
        this.exportService = exportService;
    }

    @PostMapping("/query")
    public ResponseEntity<QueryResponse> processQuery(@RequestBody QueryRequest request) {
        QueryResponse response = queryService.processQuery(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<QueryHistory>> getHistory(Authentication auth) {
        String username = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        List<QueryHistory> history = isAdmin
            ? historyRepo.findAllByOrderByTimestampDesc()
            : historyRepo.findTop10ByUsernameOrderByTimestampDesc(username);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/schema")
    public ResponseEntity<?> getSchema() {
        return ResponseEntity.ok(Map.of(
            "schema", schemaService.getSchemaContext(),
            "tables", schemaService.getDetailedSchema()
        ));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> getSuggestions(@RequestParam(defaultValue = "") String context) {
        return ResponseEntity.ok(aiService.generateSuggestions(context));
    }

    @PostMapping("/save-query")
    public ResponseEntity<?> saveQuery(@RequestBody Map<String, String> body, Authentication auth) {
        SavedQuery sq = new SavedQuery();
        sq.setUsername(auth.getName());
        sq.setQueryName(body.get("name"));
        sq.setNaturalLanguageQuery(body.get("nlQuery"));
        sq.setSqlQuery(body.get("sql"));
        savedQueryRepo.save(sq);
        return ResponseEntity.ok(Map.of("message", "Query saved successfully"));
    }

    @GetMapping("/saved-queries")
    public ResponseEntity<List<SavedQuery>> getSavedQueries(Authentication auth) {
        return ResponseEntity.ok(savedQueryRepo.findByUsernameOrderByCreatedAtDesc(auth.getName()));
    }

    @DeleteMapping("/saved-queries/{id}")
    public ResponseEntity<?> deleteSavedQuery(@PathVariable Long id) {
        savedQueryRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Deleted"));
    }

    @PostMapping("/export/csv")
    public ResponseEntity<byte[]> exportCSV(@RequestBody Map<String, Object> body) throws Exception {
        List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");
        List<String> columns = (List<String>) body.get("columns");
        byte[] csv = exportService.exportToCSV(data, columns);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=query-results.csv")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(csv);
    }

    @PostMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(@RequestBody Map<String, Object> body) throws Exception {
        List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");
        List<String> columns = (List<String>) body.get("columns");
        byte[] excel = exportService.exportToExcel(data, columns);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=query-results.xlsx")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(excel);
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats(Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) return ResponseEntity.status(403).build();
        List<Object[]> typeStats = historyRepo.getQueryTypeStats();
        long total = historyRepo.count();
        return ResponseEntity.ok(Map.of("totalQueries", total, "byType", typeStats));
    }
}
