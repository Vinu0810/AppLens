package com.applens.service;

import com.applens.dto.AppDto;
import com.applens.dto.AppStatsDto;
import com.applens.entity.App;
import com.applens.exception.ResourceNotFoundException;
import com.applens.repository.AppRepository;
import com.applens.repository.ScrapeRunRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AppService {

    private final AppRepository appRepository;
    private final ScrapeRunRepository scrapeRunRepository;
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;

    private static final String STATS_CACHE_KEY = "applens:stats";
    private static final Duration STATS_TTL = Duration.ofHours(1);

    @Transactional(readOnly = true)
    public Page<AppDto> getApps(String category, String store, String appType,
                                String search, int page, int size, String sortBy) {
        Sort sort = buildSort(sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<App> apps;

        if (search != null && !search.isBlank()) {
            apps = appRepository.searchApps(search.trim(), pageable);
        } else if (category != null && store != null && appType != null) {
            apps = appRepository.findByCategoryAndStoreAndAppType(category, store, appType, pageable);
        } else if (category != null && store != null) {
            apps = appRepository.findByCategoryAndStore(category, store, pageable);
        } else if (category != null) {
            apps = appRepository.findByCategory(category, pageable);
        } else if (store != null) {
            apps = appRepository.findByStore(store, pageable);
        } else if (appType != null) {
            apps = appRepository.findByAppType(appType, pageable);
        } else {
            apps = appRepository.findAll(pageable);
        }

        return apps.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public AppDto getById(Long id) {
        return appRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("App not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public AppStatsDto getStats() {
        // Check Redis cache first
        Optional<AppStatsDto> cached = cacheService.get(STATS_CACHE_KEY, AppStatsDto.class);
        if (cached.isPresent()) {
            log.debug("Stats served from Redis cache");
            return cached.get();
        }

        // Cache miss — query PostgreSQL
        log.debug("Stats cache miss — querying database");
        AppStatsDto stats = computeStats();

        // Store in Redis for 1 hour
        cacheService.set(STATS_CACHE_KEY, stats, STATS_TTL);
        log.info("Stats cached in Redis (TTL: 1hr)");

        return stats;
    }

    private AppStatsDto computeStats() {
        long totalApps = appRepository.count();

        Double avgRatingRaw = appRepository.findAverageRating();
        BigDecimal avgRating = avgRatingRaw != null
                ? BigDecimal.valueOf(avgRatingRaw).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long totalCategories = appRepository.countDistinctCategories();

        Map<String, Long> platformSplit = new HashMap<>();
        appRepository.countByStore()
                .forEach(row -> platformSplit.put((String) row[0], (Long) row[1]));

        Map<String, Long> appTypeSplit = new HashMap<>();
        appRepository.countByAppType()
                .forEach(row -> appTypeSplit.put((String) row[0], (Long) row[1]));

        LocalDateTime lastScrapedAt = scrapeRunRepository.findLatest()
                .map(run -> run.getCompletedAt() != null
                        ? run.getCompletedAt() : run.getStartedAt())
                .orElse(null);

        return new AppStatsDto(totalApps, avgRating, totalCategories,
                platformSplit, appTypeSplit, lastScrapedAt);
    }

    private AppDto toDto(App app) {
        return new AppDto(app.getId(), app.getAppId(), app.getName(), app.getStore(),
                app.getCategory(), app.getRating(), app.getReviewCount(), app.getInstallCount(),
                app.getPrice(), app.getAppType(), app.getSizeMb(), app.getContentRating(),
                app.getLastUpdated(), app.getIconUrl(), app.getScrapedAt());
    }

    private Sort buildSort(String sortBy) {
        if (sortBy == null) return Sort.by(Sort.Direction.DESC, "rating");
        return switch (sortBy) {
            case "reviewCount"  -> Sort.by(Sort.Direction.DESC, "reviewCount");
            case "installCount" -> Sort.by(Sort.Direction.DESC, "installCount");
            case "lastUpdated"  -> Sort.by(Sort.Direction.DESC, "lastUpdated");
            case "name"         -> Sort.by(Sort.Direction.ASC, "name");
            default             -> Sort.by(Sort.Direction.DESC, "rating");
        };
    }
}