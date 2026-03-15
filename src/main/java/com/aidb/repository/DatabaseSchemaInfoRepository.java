package com.aidb.repository;

import com.aidb.model.DatabaseSchemaInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DatabaseSchemaInfoRepository extends JpaRepository<DatabaseSchemaInfo, Long> {
    List<DatabaseSchemaInfo> findByActiveTrue();
}
