package com.applens.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AppDto(
        Long id,
        String appId,
        String name,
        String store,
        String category,
        BigDecimal rating,
        Long reviewCount,
        Long installCount,
        BigDecimal price,
        String appType,
        BigDecimal sizeMb,
        String contentRating,
        LocalDate lastUpdated,
        String iconUrl,
        LocalDateTime scrapedAt
) {}