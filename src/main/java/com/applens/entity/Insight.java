package com.applens.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "insights")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Insight {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "insight_key", nullable = false)
    private String insightKey;

    @Column(nullable = false)
    private String title;

    @Column(name = "insight_text", nullable = false, columnDefinition = "TEXT")
    private String insightText;

    @Column(columnDefinition = "TEXT")
    private String recommendation;

    @Column(precision = 4, scale = 3)
    private BigDecimal confidence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "supporting_data", columnDefinition = "JSON")
    private Map<String, Object> supportingData;

    @Column(name = "computed_at")
    private LocalDateTime computedAt;

    @PrePersist
    protected void onCreate() {
        this.computedAt = LocalDateTime.now();
    }
}
