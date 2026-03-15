package com.aidb.repository;

import com.aidb.model.SavedQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SavedQueryRepository extends JpaRepository<SavedQuery, Long> {
    List<SavedQuery> findByUsernameOrderByCreatedAtDesc(String username);
}
