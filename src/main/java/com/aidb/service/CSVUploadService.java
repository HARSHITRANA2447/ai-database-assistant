package com.aidb.service;

import com.aidb.dto.CSVUploadResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.*;
import java.sql.*;
import java.util.*;

@Service
public class CSVUploadService {

    private static final Logger log = LoggerFactory.getLogger(CSVUploadService.class);

    @Value("${csv.upload.dir:./uploads}")
    private String uploadDir;

    // ✅ Inject DataSource directly — works with Hibernate 6 / Spring Boot 3
    private final DataSource dataSource;

    public CSVUploadService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // ── Preview (no DB write) ─────────────────────────────────────
    public CSVUploadResult preview(org.springframework.web.multipart.MultipartFile file) {
        try {
            List<String[]> rows = parseCSV(file.getInputStream());
            if (rows.size() < 2) return CSVUploadResult.fail("CSV file has no data rows.");

            String[] headers = rows.get(0);
            List<Map<String, String>> preview = new ArrayList<>();
            for (int i = 1; i < Math.min(11, rows.size()); i++) {
                Map<String, String> row = new LinkedHashMap<>();
                for (int j = 0; j < headers.length; j++) {
                    row.put(headers[j], j < rows.get(i).length ? rows.get(i)[j] : "");
                }
                preview.add(row);
            }

            String suggestedName = sanitizeTableName(
                    file.getOriginalFilename().replaceAll("(?i)\\.csv$", ""));
            List<String> columnTypes = inferColumnTypes(rows, headers,
                    range(headers.length));

            return CSVUploadResult.builder()
                    .success(true)
                    .tableName(suggestedName)
                    .headers(List.of(headers))
                    .columnTypes(columnTypes)
                    .previewRows(preview)
                    .totalRows(rows.size() - 1)
                    .message("Preview ready. " + (rows.size() - 1) + " data rows found.")
                    .build();

        } catch (Exception e) {
            log.error("CSV preview failed", e);
            return CSVUploadResult.fail("Failed to read CSV: " + e.getMessage());
        }
    }

    // ── Import CSV → H2 ──────────────────────────────────────────
    public CSVUploadResult importToDatabase(
            org.springframework.web.multipart.MultipartFile file,
            String tableName, boolean dropIfExists, String[] selectedColumns) {

        tableName = sanitizeTableName(tableName);
        try {
            List<String[]> rows = parseCSV(file.getInputStream());
            if (rows.size() < 2) return CSVUploadResult.fail("CSV has no data rows.");

            String[] headers = rows.get(0);

            // Resolve selected column indexes
            int[] colIndexes;
            String[] finalHeaders;
            if (selectedColumns != null && selectedColumns.length > 0) {
                List<Integer> idxList = new ArrayList<>();
                List<String>  hList   = new ArrayList<>();
                for (int i = 0; i < headers.length; i++) {
                    for (String sc : selectedColumns) {
                        if (headers[i].trim().equalsIgnoreCase(sc.trim())) {
                            idxList.add(i); hList.add(headers[i]); break;
                        }
                    }
                }
                colIndexes   = idxList.stream().mapToInt(Integer::intValue).toArray();
                finalHeaders = hList.toArray(new String[0]);
            } else {
                colIndexes   = range(headers.length);
                finalHeaders = headers;
            }

            List<String> columnTypes = inferColumnTypes(rows, finalHeaders, colIndexes);

            // ✅ Get Connection from DataSource — always works
            try (Connection conn = dataSource.getConnection()) {
                if (dropIfExists) {
                    try (Statement st = conn.createStatement()) {
                        st.execute("DROP TABLE IF EXISTS " + tableName);
                    }
                }
                createTable(conn, tableName, finalHeaders, columnTypes);
                int inserted = batchInsert(conn, tableName, finalHeaders, rows, colIndexes);

                return CSVUploadResult.builder()
                        .success(true)
                        .tableName(tableName)
                        .headers(List.of(finalHeaders))
                        .columnTypes(columnTypes)
                        .totalRows(inserted)
                        .message("✅ Successfully imported " + inserted
                                + " rows into table '" + tableName + "'")
                        .build();
            }

        } catch (Exception e) {
            log.error("CSV import failed", e);
            return CSVUploadResult.fail("Import failed: " + e.getMessage());
        }
    }

    // ── List uploaded tables ──────────────────────────────────────
    public List<Map<String, Object>> listUploadedTables() {
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> internalTables = Set.of(
                "QUERY_HISTORY", "SAVED_QUERIES", "DATABASE_SCHEMA_INFO",
                "EMPLOYEES", "DEPARTMENTS", "PRODUCTS", "SALES");
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(null, "PUBLIC", null, new String[]{"TABLE"})) {
                while (rs.next()) {
                    String tname = rs.getString("TABLE_NAME");
                    if (internalTables.contains(tname.toUpperCase())) continue;
                    long count = 0;
                    try (Statement st = conn.createStatement();
                         ResultSet cr = st.executeQuery("SELECT COUNT(*) FROM " + tname)) {
                        if (cr.next()) count = cr.getLong(1);
                    }
                    Map<String, Object> t = new LinkedHashMap<>();
                    t.put("tableName", tname);
                    t.put("rowCount", count);
                    result.add(t);
                }
            }
        } catch (Exception e) {
            log.error("Failed to list tables", e);
        }
        return result;
    }

    // ── Drop table ────────────────────────────────────────────────
    public boolean dropTable(String tableName) {
        tableName = sanitizeTableName(tableName);
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            st.execute("DROP TABLE IF EXISTS " + tableName);
            return true;
        } catch (Exception e) {
            log.error("Drop table failed", e);
            return false;
        }
    }

    // ── CSV Parsing ───────────────────────────────────────────────
    private List<String[]> parseCSV(InputStream is) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                if (first && line.startsWith("\uFEFF")) line = line.substring(1);
                first = false;
                rows.add(parseLine(line));
            }
        }
        return rows;
    }

    private String[] parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"'); i++;
                } else { inQuotes = !inQuotes; }
            } else if (c == ',' && !inQuotes) {
                fields.add(sb.toString().trim()); sb.setLength(0);
            } else { sb.append(c); }
        }
        fields.add(sb.toString().trim());
        return fields.toArray(new String[0]);
    }

    // ── Type Inference ────────────────────────────────────────────
    private List<String> inferColumnTypes(List<String[]> rows, String[] headers, int[] colIndexes) {
        List<String> types = new ArrayList<>();
        for (int ci = 0; ci < colIndexes.length; ci++) {
            int colIdx = colIndexes[ci];
            boolean isInt = true, isDouble = true, isDate = true;
            int nonEmpty = 0;
            for (int r = 1; r < Math.min(rows.size(), 100); r++) {
                String[] row = rows.get(r);
                String val = colIdx < row.length ? row[colIdx].trim() : "";
                if (val.isEmpty()) continue;
                nonEmpty++;
                if (isInt)    isInt    = val.matches("-?\\d{1,18}");
                if (isDouble) isDouble = val.matches("-?\\d+(\\.\\d+)?");
                if (isDate)   isDate   = val.matches("\\d{4}-\\d{2}-\\d{2}") ||
                        val.matches("\\d{2}/\\d{2}/\\d{4}");
            }
            if (nonEmpty == 0)   types.add("VARCHAR(255)");
            else if (isInt)      types.add("BIGINT");
            else if (isDouble)   types.add("DECIMAL(18,4)");
            else if (isDate)     types.add("DATE");
            else {
                int maxLen = 50;
                for (int r = 1; r < rows.size(); r++) {
                    String[] row = rows.get(r);
                    String val = colIdx < row.length ? row[colIdx] : "";
                    maxLen = Math.max(maxLen, val.length());
                }
                types.add(maxLen > 500 ? "TEXT" : "VARCHAR(" + Math.max(maxLen + 50, 255) + ")");
            }
        }
        return types;
    }

    // ── DDL + DML ─────────────────────────────────────────────────
    private void createTable(Connection conn, String tableName,
                             String[] headers, List<String> types) throws SQLException {
        StringBuilder ddl = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
                .append(tableName).append(" (")
                .append("id BIGINT AUTO_INCREMENT PRIMARY KEY, ");
        for (int i = 0; i < headers.length; i++) {
            ddl.append(sanitizeColumnName(headers[i])).append(" ").append(types.get(i));
            if (i < headers.length - 1) ddl.append(", ");
        }
        ddl.append(")");
        try (Statement st = conn.createStatement()) { st.execute(ddl.toString()); }
    }

    private int batchInsert(Connection conn, String tableName,
                            String[] headers, List<String[]> rows,
                            int[] colIndexes) throws SQLException {
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
        for (int i = 0; i < headers.length; i++) {
            sql.append(sanitizeColumnName(headers[i]));
            if (i < headers.length - 1) sql.append(", ");
        }
        sql.append(") VALUES (").append("?,".repeat(headers.length));
        sql.deleteCharAt(sql.length() - 1).append(")");

        int inserted = 0;
        boolean autoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int r = 1; r < rows.size(); r++) {
                String[] row = rows.get(r);
                boolean allEmpty = true;
                for (int ci = 0; ci < colIndexes.length; ci++) {
                    String val = colIndexes[ci] < row.length ? row[colIndexes[ci]].trim() : "";
                    if (!val.isEmpty()) allEmpty = false;
                    ps.setString(ci + 1, val.isEmpty() ? null : val);
                }
                if (!allEmpty) { ps.addBatch(); inserted++; }
                if (r % 500 == 0) ps.executeBatch();
            }
            ps.executeBatch();
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(autoCommit);
        }
        return inserted;
    }

    // ── Helpers ───────────────────────────────────────────────────
    private String sanitizeTableName(String name) {
        if (name == null || name.isBlank()) return "uploaded_data";
        return name.trim().replaceAll("[^a-zA-Z0-9_]", "_")
                .replaceAll("^(\\d)", "_$1")
                .toLowerCase();
    }

    private String sanitizeColumnName(String name) {
        if (name == null || name.isBlank()) return "col_unknown";
        String sanitized = name.trim().replaceAll("[^a-zA-Z0-9_]", "_")
                .replaceAll("^(\\d)", "col_$1")
                .toLowerCase();
        // H2 reserved words that need quoting
        Set<String> reserved = Set.of("group", "order", "select", "from", "where", "join", "table", "index", "key", "primary", "foreign", "unique", "check", "default", "null", "not", "and", "or", "like", "in", "exists", "between", "case", "when", "then", "else", "end", "limit", "offset", "union", "all", "distinct", "as", "on", "using", "natural", "left", "right", "inner", "outer", "full", "cross");
        if (reserved.contains(sanitized)) {
            return "\"" + sanitized + "\"";
        }
        return sanitized;
    }

    private int[] range(int n) {
        int[] r = new int[n];
        for (int i = 0; i < n; i++) r[i] = i;
        return r;
    }
}
