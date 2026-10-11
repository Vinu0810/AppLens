package com.applens.dto;

import java.time.LocalDateTime;

public record ScrapeRunDto(
        Long id,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String status,
        Integer appsScraped,
        Integer appsCreated,
        Integer appsUpdated,
        String errorMessage,
        String store
) {}