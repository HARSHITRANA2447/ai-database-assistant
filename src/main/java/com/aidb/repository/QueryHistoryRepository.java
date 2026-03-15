package com.aidb.repository;

import com.aidb.model.QueryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QueryHistoryRepository extends JpaRepository<QueryHistory, Long> {
    List<QueryHistory> findByUsernameOrderByTimestampDesc(String username);
    List<QueryHistory> findTop10ByUsernameOrderByTimestampDesc(String username);
    List<QueryHistory> findAllByOrderByTimestampDesc();

    @Query("SELECT q FROM QueryHistory q WHERE q.executionStatus = 'SUCCESS' ORDER BY q.timestamp DESC")
    List<QueryHistory> findAllSuccessfulQueries();

    @Query("SELECT q.queryType, COUNT(q) FROM QueryHistory q GROUP BY q.queryType")
    List<Object[]> getQueryTypeStats();

    @Query("SELECT COUNT(q) FROM QueryHistory q WHERE q.username = :username")
    long countByUsername(String username);
}
