package com.applens.repository;

import com.applens.entity.Insight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InsightRepository extends JpaRepository<Insight, Long> {
    
    List<Insight> findAllByOrderByComputedAtDesc();

    Optional<Insight> findByInsightKey(String insightKey);

    void deleteByInsightKey(String insightKey);
}
