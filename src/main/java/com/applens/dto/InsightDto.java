package com.applens.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record InsightDto(
        Long id,
        String insightKey,
        String title,
        String insightText,
        String recommendation,
        BigDecimal confidence,
        Map<String, Object> supportingData,
        LocalDateTime computedAt
) {}