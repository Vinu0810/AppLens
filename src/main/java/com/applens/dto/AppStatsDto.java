package com.applens.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record AppStatsDto(
        long totalApps,
        BigDecimal avgRating,
        long totalCategories,
        Map<String, Long> platformSplit,
        Map<String, Long> appTypeSplit,
        LocalDateTime lastScrapedAt
) {}