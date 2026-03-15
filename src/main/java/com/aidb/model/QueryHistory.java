package com.aidb.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "query_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "user_query", columnDefinition = "TEXT", nullable = false)
    private String userQuery;

    @Column(name = "generated_sql", columnDefinition = "TEXT")
    private String generatedSql;

    @Column(name = "execution_status")
    private String executionStatus;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "row_count")
    private Integer rowCount;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "ai_explanation", columnDefinition = "TEXT")
    private String aiExplanation;

    @Column(name = "query_type")
    private String queryType;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) timestamp = LocalDateTime.now();
    }
}
