package com.applens.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StatTestDto(
        Long id,
        String testName,
        String groupALabel,
        String groupBLabel,
        BigDecimal groupAMean,
        BigDecimal groupBMean,
        Integer groupACount,
        Integer groupBCount,
        BigDecimal tStatistic,
        BigDecimal pValue,
        Boolean isSignificant,
        BigDecimal confidence,
        LocalDateTime computedAt
) {}