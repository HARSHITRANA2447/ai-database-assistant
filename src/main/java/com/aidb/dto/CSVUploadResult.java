package com.aidb.dto;

import lombok.*;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CSVUploadResult {
    private boolean success;
    private String message;
    private String tableName;
    private List<String> headers;
    private List<String> columnTypes;
    private List<Map<String, String>> previewRows;
    private int totalRows;

    public static CSVUploadResult fail(String message) {
        return CSVUploadResult.builder().success(false).message(message).build();
    }
}
