package com.applens.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "scrape_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScrapeRun {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    private String status;

    @Column(name = "apps_scraped")
    private Integer appsScraped;

    @Column(name = "apps_created")
    private Integer appsCreated;

    @Column(name = "apps_updated")
    private Integer appsUpdated;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    private String store;

    public static ScrapeRun started(String store) {
        return ScrapeRun.builder()
                .startedAt(LocalDateTime.now())
                .status("RUNNING")
                .store(store)
                .appsScraped(0)
                .appsCreated(0)
                .appsUpdated(0)
                .build();
    }

    public ScrapeRun completed(int created, int updated) {
        this.completedAt = LocalDateTime.now();
        this.status = "SUCCESS";
        this.appsCreated = created;
        this.appsUpdated = updated;
        this.appsScraped = created + updated;
        return this;
    }

    public ScrapeRun failed(String error) {
        this.completedAt = LocalDateTime.now();
        this.status = "FAILED";
        this.errorMessage = error;
        return this;
    }
}
