package com.applens.repository;

import com.applens.entity.ScrapeRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScrapeRunRepository extends JpaRepository<ScrapeRun, Long> {
    
    Page<ScrapeRun> findAllByOrderByStartedAtDesc(Pageable pageable);

    @Query("SELECT s FROM ScrapeRun s ORDER BY s.startedAt DESC LIMIT 1")
    Optional<ScrapeRun> findLatest();
}
