package com.aidb.controller;

import com.aidb.dto.CSVUploadResult;
import com.aidb.service.CSVUploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/csv")
public class CSVController {

    private final CSVUploadService csvUploadService;

    public CSVController(CSVUploadService csvUploadService) {
        this.csvUploadService = csvUploadService;
    }

    /** Step 1: Upload and preview — no DB write yet */
    @PostMapping("/preview")
    public ResponseEntity<CSVUploadResult> preview(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body(CSVUploadResult.fail("No file selected."));
        if (!file.getOriginalFilename().toLowerCase().endsWith(".csv"))
            return ResponseEntity.badRequest().body(CSVUploadResult.fail("Only .csv files are supported."));
        return ResponseEntity.ok(csvUploadService.preview(file));
    }

    /** Step 2: Confirm and import into H2 */
    @PostMapping("/import")
    public ResponseEntity<CSVUploadResult> importCSV(
            @RequestParam("file") MultipartFile file,
            @RequestParam("tableName") String tableName,
            @RequestParam(value = "dropIfExists", defaultValue = "false") boolean dropIfExists,
            @RequestParam(value = "selectedColumns", required = false) String[] selectedColumns) {

        if (file.isEmpty()) return ResponseEntity.badRequest().body(CSVUploadResult.fail("No file."));
        return ResponseEntity.ok(
            csvUploadService.importToDatabase(file, tableName, dropIfExists, selectedColumns));
    }

    /** List all user-uploaded tables */
    @GetMapping("/tables")
    public ResponseEntity<List<Map<String, Object>>> listTables() {
        return ResponseEntity.ok(csvUploadService.listUploadedTables());
    }

    /** Drop a table */
    @DeleteMapping("/tables/{tableName}")
    public ResponseEntity<?> dropTable(@PathVariable String tableName) {
        boolean ok = csvUploadService.dropTable(tableName);
        return ok ? ResponseEntity.ok(Map.of("message", "Table dropped: " + tableName))
                  : ResponseEntity.status(500).body(Map.of("error", "Failed to drop table."));
    }
}
