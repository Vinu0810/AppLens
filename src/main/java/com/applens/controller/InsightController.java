package com.applens.controller;

import com.applens.dto.ApiResponse;
import com.applens.dto.InsightDto;
import com.applens.dto.StatTestDto;
import com.applens.entity.Insight;
import com.applens.entity.StatisticalTest;
import com.applens.service.InsightService;
import com.applens.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/insights")
@RequiredArgsConstructor
@Tag(name = "Insights", description = "Computed market insights and statistical tests")
public class InsightController {

    private final InsightService insightService;
    private final StatisticsService statisticsService;

    @GetMapping
    @Operation(summary = "Get all computed insights")
    public ResponseEntity<ApiResponse<List<InsightDto>>> getInsights() {
        List<InsightDto> dtos = insightService.getAll().stream()
                .map(this::toDto).toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get all statistical test results")
    public ResponseEntity<ApiResponse<List<StatTestDto>>> getStatistics() {
        List<StatTestDto> dtos = statisticsService.getAll().stream()
                .map(this::toTestDto).toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @PostMapping("/recompute")
    @Operation(summary = "Recompute all insights and statistics")
    public ResponseEntity<ApiResponse<String>> recompute() {
        insightService.recomputeAll();
        statisticsService.recomputeAll();
        return ResponseEntity.ok(ApiResponse.success("Recomputed successfully"));
    }

    private InsightDto toDto(Insight i) {
        return new InsightDto(i.getId(), i.getInsightKey(), i.getTitle(),
                i.getInsightText(), i.getRecommendation(), i.getConfidence(),
                i.getSupportingData(), i.getComputedAt());
    }

    private StatTestDto toTestDto(StatisticalTest t) {
        return new StatTestDto(t.getId(), t.getTestName(), t.getGroupALabel(),
                t.getGroupBLabel(), t.getGroupAMean(), t.getGroupBMean(),
                t.getGroupACount(), t.getGroupBCount(), t.getTStatistic(),
                t.getPValue(), t.getIsSignificant(), t.getConfidence(),
                t.getComputedAt());
    }
}