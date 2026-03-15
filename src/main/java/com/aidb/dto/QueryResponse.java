package com.aidb.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryResponse {
    private boolean success;
    private String userQuery;
    private String generatedSql;
    private String explanation;
    private String queryType;
    private List<Map<String, Object>> results;
    private List<String> columns;
    private int rowCount;
    private long executionTimeMs;
    private String error;
    private String chartType;
    private boolean hasNumericData;
    private List<String> suggestions;
    private String insight;
}
