package com.applens.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "statistical_tests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatisticalTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "test_name", nullable = false)
    private String testName;

    @Column(name = "group_a_label")
    private String groupALabel;

    @Column(name = "group_b_label")
    private String groupBLabel;

    @Column(name = "group_a_mean", precision = 10, scale = 4)
    private BigDecimal groupAMean;

    @Column(name = "group_b_mean", precision = 10, scale = 4)
    private BigDecimal groupBMean;

    @Column(name = "group_a_count")
    private Integer groupACount;

    @Column(name = "group_b_count")
    private Integer groupBCount;

    @Column(name = "t_statistic", precision = 10, scale = 4)
    private BigDecimal tStatistic;

    @Column(name = "p_value", precision = 10, scale = 4)
    private BigDecimal pValue;

    @Column(name = "is_Significant")
    private Boolean isSignificant;

    @Column(precision = 4, scale = 3)
    private BigDecimal confidence;

    @Column(name = "computed_at")
    private LocalDateTime computedAt;

    @PrePersist
    protected void onCreate() {
        this.computedAt = LocalDateTime.now();
    }
}
