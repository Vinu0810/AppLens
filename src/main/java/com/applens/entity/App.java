package com.applens.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "apps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class App {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @Column(name = "app_id", unique = true, nullable = false)
    private String appId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String store;

    private String category;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    @Column(name = "review_count")
    private Long reviewCount;

    @Column(name = "install_count")
    private Long installCount;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "app_type")
    private String appType;

    @Column(name = "size_mb", precision = 10, scale = 2)
    private BigDecimal sizeMb;

    @Column(name = "content_rating")
    private String contentRating;

    @Column(name = "min_age")
    private Integer minAge;

    @Column(name = "last_updated")
    private LocalDate lastUpdated;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon_url")
    private String iconUrl;

    @Column(name = "scraped_at")
    private LocalDateTime scrapedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.scrapedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
