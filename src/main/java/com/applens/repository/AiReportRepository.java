package com.applens.repository;

import com.applens.entity.AiReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiReportRepository extends JpaRepository<AiReport, Long> {
    
    Optional<AiReport> findByDataHash(String dataHash);
}
