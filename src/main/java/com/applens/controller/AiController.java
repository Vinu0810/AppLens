package com.applens.controller;

import com.applens.dto.ApiResponse;
import com.applens.service.AiService;
import com.applens.service.RateLimiterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "AI", description = "AI-powered market analysis endpoints")
public class AiController {

    private final AiService aiService;
    private final RateLimiterService rateLimiterService;

    @PostMapping("/summary")
    @Operation(summary = "Generate AI executive summary of current market data")
    public ResponseEntity<ApiResponse<String>> generateSummary() {
        log.info("Executive summary requested");
        String summary = aiService.generateExecutiveSummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @PostMapping("/ask")
    @Operation(summary = "Ask a business question about the app market data")
    public ResponseEntity<ApiResponse<String>> askQuestion(
            @RequestParam @NotBlank String question,
            HttpServletRequest request) {

        // Rate limit: 10 requests per hour per IP
        String clientIp = request.getRemoteAddr();
        String rateLimitKey = "rate:ai-ask:" + clientIp;

        if (!rateLimiterService.isAllowed(rateLimitKey, 10, Duration.ofHours(1))) {
            long ttl = rateLimiterService.getTtlSeconds(rateLimitKey);
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error(
                            "Rate limit exceeded. Try again in " + ttl + " seconds.", 429));
        }

        log.info("AI question from {}: {}", clientIp, question);
        String answer = aiService.answerQuestion(question);
        return ResponseEntity.ok(ApiResponse.success(answer));
    }

    @GetMapping("/reports")
    @Operation(summary = "Get all cached AI reports")
    public ResponseEntity<ApiResponse<String>> getCachedReports() {
        return ResponseEntity.ok(ApiResponse.success(
                "AI reports stored in ai_reports table with SHA-256 cache keys."));
    }
}