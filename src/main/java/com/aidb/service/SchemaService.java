package com.aidb.service;

import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class SchemaService {

    private static final Logger log = LoggerFactory.getLogger(SchemaService.class);
    private final EntityManager entityManager;

    public SchemaService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public String getSchemaContext() {
        try {
            // H2 returns plain String for single-column SELECT — NOT Object[]
            List<?> tables = entityManager.createNativeQuery(
                    "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES " +
                            "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_TYPE='BASE TABLE'"
            ).getResultList();

            Set<String> skipTables = Set.of(
                    "QUERY_HISTORY", "SAVED_QUERIES", "DATABASE_SCHEMA_INFO");

            StringBuilder schema = new StringBuilder();
            for (Object tableObj : tables) {
                String tableName = tableObj.toString();
                if (skipTables.contains(tableName.toUpperCase())) continue;

                schema.append("Table: ").append(tableName.toLowerCase()).append("(");

                // Two-column SELECT → H2 returns Object[]
                List<?> cols = entityManager.createNativeQuery(
                        "SELECT COLUMN_NAME, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                                "WHERE TABLE_NAME = '" + tableName + "' AND TABLE_SCHEMA = 'PUBLIC' " +
                                "ORDER BY ORDINAL_POSITION"
                ).getResultList();

                StringJoiner colJoiner = new StringJoiner(", ");
                for (Object colObj : cols) {
                    Object[] col = (Object[]) colObj;
                    colJoiner.add(col[0].toString().toLowerCase() + " " + col[1].toString().toLowerCase());
                }
                schema.append(colJoiner).append(")\n");
            }
            return schema.length() > 0 ? schema.toString()
                    : "No user tables found.";
        } catch (Exception e) {
            log.error("Failed to read schema", e);
            return "Schema unavailable.";
        }
    }

    public List<Map<String, Object>> getDetailedSchema() {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            List<?> tables = entityManager.createNativeQuery(
                    "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES " +
                            "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_TYPE='BASE TABLE'"
            ).getResultList();

            for (Object tableObj : tables) {
                String tableName = tableObj.toString();
                Map<String, Object> tableInfo = new LinkedHashMap<>();
                tableInfo.put("tableName", tableName);

                List<?> cols = entityManager.createNativeQuery(
                        "SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS " +
                                "WHERE TABLE_NAME = '" + tableName + "' AND TABLE_SCHEMA = 'PUBLIC' " +
                                "ORDER BY ORDINAL_POSITION"
                ).getResultList();

                List<Map<String, String>> columns = new ArrayList<>();
                for (Object colObj : cols) {
                    Object[] col = (Object[]) colObj;
                    Map<String, String> colInfo = new LinkedHashMap<>();
                    colInfo.put("name", col[0].toString());
                    colInfo.put("type", col[1].toString());
                    colInfo.put("nullable", col[2].toString());
                    columns.add(colInfo);
                }
                tableInfo.put("columns", columns);
                result.add(tableInfo);
            }
        } catch (Exception e) {
            log.error("Schema detail read failed", e);
        }
        return result;
    }
}
