package com.aidb.dto;

import lombok.Data;

@Data
public class QueryRequest {
    private String query;
    private boolean explain;
    private String exportFormat;
}
