package com.aidb.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "saved_queries")
@Data
@NoArgsConstructor
public class SavedQuery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String queryName;
    @Column(columnDefinition = "TEXT")
    private String naturalLanguageQuery;
    @Column(columnDefinition = "TEXT")
    private String sqlQuery;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
