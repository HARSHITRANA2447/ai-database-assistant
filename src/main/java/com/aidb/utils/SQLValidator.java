package com.aidb.utils;

import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class SQLValidator {

    private static final List<String> BLOCKED_KEYWORDS = Arrays.asList(
        "DROP", "DELETE", "UPDATE", "INSERT", "ALTER", "CREATE", "TRUNCATE",
        "EXEC", "EXECUTE", "GRANT", "REVOKE", "MERGE", "REPLACE", "LOAD",
        "INTO OUTFILE", "DUMPFILE", "SLEEP", "BENCHMARK", "INFORMATION_SCHEMA"
    );

    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "('|--|;|/\\*|\\*/|xp_|0x|CHAR\\(|NCHAR\\(|VARCHAR\\()",
        Pattern.CASE_INSENSITIVE
    );

    public ValidationResult validate(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return ValidationResult.fail("SQL query is empty.");
        }

        String upperSql = sql.trim().toUpperCase();

        // Must start with SELECT
        if (!upperSql.startsWith("SELECT")) {
            return ValidationResult.fail("Only SELECT queries are allowed for security reasons.");
        }

        // Check for blocked keywords
        for (String keyword : BLOCKED_KEYWORDS) {
            if (upperSql.contains(keyword)) {
                return ValidationResult.fail("Query contains forbidden keyword: " + keyword);
            }
        }

        // Check for SQL injection patterns
        if (SQL_INJECTION_PATTERN.matcher(sql).find()) {
            return ValidationResult.fail("Query contains potentially unsafe characters.");
        }

        // Check for multiple statements
        if (upperSql.contains(";")) {
            return ValidationResult.fail("Multiple SQL statements are not allowed.");
        }

        return ValidationResult.success();
    }

    public String sanitizeSQL(String sql) {
        if (sql == null) return null;
        // Remove any markdown code blocks that AI might return
        sql = sql.replaceAll("```sql", "").replaceAll("```", "").trim();
        // Remove trailing semicolons
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).trim();
        }
        return sql;
    }

    public String detectQueryType(String sql) {
        String upper = sql.toUpperCase();
        if (upper.contains("COUNT(") || upper.contains("SUM(") ||
            upper.contains("AVG(") || upper.contains("MAX(") || upper.contains("MIN(")) {
            return "AGGREGATION";
        }
        if (upper.contains("JOIN")) return "JOIN";
        if (upper.contains("GROUP BY")) return "GROUP BY";
        if (upper.contains("ORDER BY")) return "ORDER BY";
        if (upper.contains("WHERE")) return "FILTERED SELECT";
        return "SELECT";
    }

    public record ValidationResult(boolean valid, String message) {
        public static ValidationResult success() { return new ValidationResult(true, null); }
        public static ValidationResult fail(String message) { return new ValidationResult(false, message); }
    }
}
