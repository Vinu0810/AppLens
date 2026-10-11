package com.applens.controller;

import com.applens.dto.ApiResponse;
import com.applens.dto.AppDto;
import com.applens.dto.AppStatsDto;
import com.applens.service.AppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/apps")
@RequiredArgsConstructor
@Tag(name = "Apps", description = "App browsing and statistics")
public class AppController {

    private final AppService appService;

    @GetMapping
    @Operation(summary = "Get paginated apps with optional filters")
    public ResponseEntity<ApiResponse<Page<AppDto>>> getApps(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String store,
            @RequestParam(required = false) String appType,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "rating") String sortBy
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                appService.getApps(category, store, appType, search, page, size, sortBy)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get single app by ID")
    public ResponseEntity<ApiResponse<AppDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(appService.getById(id)));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get platform-wide statistics")
    public ResponseEntity<ApiResponse<AppStatsDto>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(appService.getStats()));
    }
}