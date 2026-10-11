package com.applens.controller;

import com.applens.dto.ApiResponse;
import com.applens.dto.ScrapeRunDto;
import com.applens.entity.ScrapeRun;
import com.applens.repository.ScrapeRunRepository;
import com.applens.service.AppEnrichmentService;
import com.applens.service.InsightService;
import com.applens.service.PlayStoreScraperService;
import com.applens.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin", description = "Admin operations")
public class AdminController {

    private final PlayStoreScraperService scraperService;
    private final InsightService insightService;
    private final StatisticsService statisticsService;
    private final AppEnrichmentService enrichmentService;
    private final ScrapeRunRepository scrapeRunRepository;

    @PostMapping("/trigger-scrape")
    @Operation(summary = "Trigger a Play Store scrape then recompute insights")
    public ResponseEntity<ApiResponse<String>> triggerScrape() {
        log.info("Manual scrape triggered");
        ScrapeRun run = scraperService.scrapeTopApps();
        insightService.recomputeAll();
        statisticsService.recomputeAll();
        return ResponseEntity.ok(ApiResponse.success(
                "Scrape complete. Created: " + run.getAppsCreated() +
                ", Updated: " + run.getAppsUpdated() +
                ". Insights recomputed."));
    }

    @PostMapping("/enrich-ratings")
    @Operation(summary = "Enrich apps with ratings from individual app pages")
    public ResponseEntity<ApiResponse<String>> enrichRatings(
            @RequestParam(defaultValue = "50") int maxApps) {
        log.info("Rating enrichment triggered for {} apps", maxApps);
        enrichmentService.enrichRatings(maxApps);
        insightService.recomputeAll();
        statisticsService.recomputeAll();
        return ResponseEntity.ok(ApiResponse.success(
                "Enrichment complete for up to " + maxApps + " apps. Insights recomputed."));
    }

    @GetMapping("/scrape-runs")
    @Operation(summary = "Get scrape run history")
    public ResponseEntity<ApiResponse<Page<ScrapeRunDto>>> getScrapeRuns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ScrapeRunDto> runs = scrapeRunRepository
                .findAllByOrderByStartedAtDesc(PageRequest.of(page, size))
                .map(this::toDto);
        return ResponseEntity.ok(ApiResponse.success(runs));
    }

    private ScrapeRunDto toDto(ScrapeRun run) {
        return new ScrapeRunDto(run.getId(), run.getStartedAt(), run.getCompletedAt(),
                run.getStatus(), run.getAppsScraped(), run.getAppsCreated(),
                run.getAppsUpdated(), run.getErrorMessage(), run.getStore());
    }
}