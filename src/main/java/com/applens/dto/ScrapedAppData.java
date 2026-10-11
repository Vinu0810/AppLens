package com.applens.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ScrapedAppData(
        String appId,
        String name,
        String store,
        String category,
        BigDecimal rating,
        Long reviewCount,
        Long installCount,
        BigDecimal price,
        String appType,
        String contentRating,
        String iconUrl,
        LocalDate lastUpdated
) {}