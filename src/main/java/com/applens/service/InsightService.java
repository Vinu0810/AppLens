package com.applens.service;

import com.applens.entity.Insight;
import com.applens.repository.AppRepository;
import com.applens.repository.InsightRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class InsightService {

    private final AppRepository appRepository;
    private final InsightRepository insightRepository;
    private final CacheService cacheService;

    @Transactional
    public void recomputeAll() {
        log.info("Recomputing all insights...");
        insightRepository.deleteAll();

        List<Insight> insights = new ArrayList<>();
        insights.add(computeFreeVsPaidInsight());
        insights.add(computeBestCategoryInsight());
        insights.add(computePlatformComparisonInsight());
        insights.add(computeTopCategoriesByInstallsInsight());
        insights.add(computeContentRatingInsight());
        insights.add(computeTopRatedCategoryInsight());
        insights.add(computeMostReviewedCategoryInsight());
        insights.add(computeAppTypeDistributionInsight());
        insights.add(computeCategoryCountInsight());
        insights.add(computeOverallMarketInsight());

        insightRepository.saveAll(insights);
        // Invalidate cached stats and insights after recompute
        cacheService.deleteByPattern("applens:*");
        log.info("Computed {} insights. Cache invalidated.", insights.size());
    }

    @Transactional(readOnly = true)
    public List<Insight> getAll() {
        return insightRepository.findAllByOrderByComputedAtDesc();
    }

    // ── Insight 1: Free vs Paid ──────────────────────────────────────
    private Insight computeFreeVsPaidInsight() {
        List<Object[]> stats = appRepository.findAvgRatingByAppType();

        Map<String, Object> supporting = new LinkedHashMap<>();
        double freeAvg = 0, paidAvg = 0;
        long freeCount = 0, paidCount = 0;

        for (Object[] row : stats) {
            String type = (String) row[0];
            double avg = row[1] != null ? ((Number) row[1]).doubleValue() : 0;
            long count = row[2] != null ? ((Number) row[2]).longValue() : 0;
            supporting.put(type + "_avg_rating", round2(avg));
            supporting.put(type + "_count", count);
            if ("Free".equalsIgnoreCase(type)) { freeAvg = avg; freeCount = count; }
            else { paidAvg = avg; paidCount = count; }
        }

        long totalApps = freeCount + paidCount;
        String dominant = freeCount > paidCount ? "Free" : "Paid";
        double dominantPct = totalApps > 0
                ? Math.round((double) Math.max(freeCount, paidCount) / totalApps * 100) : 0;

        String text = String.format(
                "%s apps dominate the market at %.0f%% of all apps (%,d apps). " +
                "Free apps average %.2f stars while paid apps average %.2f stars.",
                dominant, dominantPct, Math.max(freeCount, paidCount), freeAvg, paidAvg);

        String recommendation = freeAvg >= paidAvg
                ? "Free apps with in-app purchases outperform paid apps in ratings. Prioritize freemium model."
                : "Paid apps maintain higher ratings. Quality over volume strategy shows measurable results.";

        return buildInsight("free_vs_paid", "Free vs Paid App Analysis", text, recommendation,
                0.88, supporting);
    }

    // ── Insight 2: Best Category by Rating ──────────────────────────
    private Insight computeBestCategoryInsight() {
        List<Object[]> stats = appRepository.findAvgRatingByCategory();
        if (stats.isEmpty()) return buildInsight("best_category", "Best Category by Rating",
                "Insufficient data.", null, 0.5, Map.of());

        Object[] best = stats.get(0);
        String category = (String) best[0];
        double avgRating = best[1] != null ? ((Number) best[1]).doubleValue() : 0;
        long count = best[2] != null ? ((Number) best[2]).longValue() : 0;

        Map<String, Object> supporting = new LinkedHashMap<>();
        stats.forEach(row -> supporting.put((String) row[0],
                Map.of("avgRating", round2(((Number) row[1]).doubleValue()),
                       "count", ((Number) row[2]).longValue())));

        String text = String.format(
                "%s is the top-rated category with an average rating of %.2f stars across %,d apps.",
                category, avgRating, count);

        return buildInsight("best_category", "Highest Rated Category", text,
                "Focus development resources on " + category + " — users rate it highest.",
                0.85, supporting);
    }

    // ── Insight 3: Platform Comparison ──────────────────────────────
    private Insight computePlatformComparisonInsight() {
        List<Object[]> stats = appRepository.findAvgRatingByStore();
        Map<String, Object> supporting = new LinkedHashMap<>();
        String text;

        if (stats.isEmpty()) {
            text = "Only Google Play data available currently.";
        } else if (stats.size() == 1) {
            String store = (String) stats.get(0)[0];
            double avg = stats.get(0)[1] != null ? ((Number) stats.get(0)[1]).doubleValue() : 0;
            long count = ((Number) stats.get(0)[2]).longValue();
            supporting.put(store, Map.of("avgRating", round2(avg), "count", count));
            text = String.format("%s has %,d apps with an average rating of %.2f stars.", store, count, avg);
        } else {
            stats.forEach(row -> supporting.put((String) row[0],
                    Map.of("avgRating", round2(((Number) row[1]).doubleValue()),
                           "count", ((Number) row[2]).longValue())));
            text = "Multi-platform comparison available across " + stats.size() + " stores.";
        }

        return buildInsight("platform_comparison", "Platform Comparison", text,
                "Expand to additional platforms to increase market reach.",
                0.80, supporting);
    }

    // ── Insight 4: Top Categories by Installs ───────────────────────
    private Insight computeTopCategoriesByInstallsInsight() {
        List<Object[]> stats = appRepository.findTopCategoriesByInstalls();
        Map<String, Object> supporting = new LinkedHashMap<>();

        if (stats.isEmpty()) {
            return buildInsight("top_by_installs", "Top Categories by Installs",
                    "Install count data not yet available.", null, 0.5, supporting);
        }

        stats.stream().limit(5).forEach(row -> {
            String cat = (String) row[0];
            long installs = row[1] != null ? ((Number) row[1]).longValue() : 0;
            long count = ((Number) row[2]).longValue();
            supporting.put(cat, Map.of("totalInstalls", installs, "appCount", count));
        });

        String topCategory = (String) stats.get(0)[0];
        long topInstalls = stats.get(0)[1] != null ? ((Number) stats.get(0)[1]).longValue() : 0;

        String text = String.format(
                "%s leads with %,d total installs across its apps, making it the highest-volume category.",
                topCategory, topInstalls);

        return buildInsight("top_by_installs", "Top Categories by Install Volume", text,
                "Invest in " + topCategory + " for maximum user reach.",
                0.82, supporting);
    }

    // ── Insight 5: Content Rating Distribution ──────────────────────
    private Insight computeContentRatingInsight() {
        List<Object[]> stats = appRepository.findContentRatingStats();
        Map<String, Object> supporting = new LinkedHashMap<>();
        long total = 0;
        String dominant = "Unknown";
        long dominantCount = 0;

        for (Object[] row : stats) {
            String rating = (String) row[0];
            long count = ((Number) row[1]).longValue();
            double avgR = row[2] != null ? ((Number) row[2]).doubleValue() : 0;
            supporting.put(rating, Map.of("count", count, "avgRating", round2(avgR)));
            total += count;
            if (count > dominantCount) { dominantCount = count; dominant = rating; }
        }

        double dominantPct = total > 0 ? Math.round((double) dominantCount / total * 100) : 0;
        String text = String.format(
                "'%s' rated apps make up %.0f%% of the market (%,d apps), " +
                "indicating the primary target demographic.",
                dominant, dominantPct, dominantCount);

        return buildInsight("content_rating", "Content Rating Distribution", text,
                "Develop primarily for '" + dominant + "' rated content to reach the largest audience.",
                0.79, supporting);
    }

    // ── Insight 6: Top Rated Category ───────────────────────────────
    private Insight computeTopRatedCategoryInsight() {
        List<Object[]> stats = appRepository.findAvgRatingByCategory();
        if (stats.isEmpty()) return buildInsight("top_rated_category", "Top Rated Category",
                "No rating data available.", null, 0.5, Map.of());

        // Find category with highest avg rating that has at least 5 apps
        Object[] best = stats.stream()
                .filter(r -> r[2] != null && ((Number) r[2]).longValue() >= 5)
                .findFirst()
                .orElse(stats.get(0));

        String category = (String) best[0];
        double avgRating = best[1] != null ? ((Number) best[1]).doubleValue() : 0;
        long appCount = best[2] != null ? ((Number) best[2]).longValue() : 0;

        String text = String.format(
                "%s apps receive the highest user satisfaction with %.2f average rating " +
                "across %,d apps — consistently above market average.",
                category, avgRating, appCount);

        return buildInsight("top_rated_category", "User Satisfaction Leader", text,
                "Study UX patterns in " + category + " apps and apply to other categories.",
                0.83, Map.of("category", category, "avgRating", round2(avgRating),
                             "appCount", appCount));
    }

    // ── Insight 7: Most Reviewed Category ───────────────────────────
    private Insight computeMostReviewedCategoryInsight() {
        List<Object[]> stats = appRepository.findCategoryBreakdown();
        if (stats.isEmpty()) return buildInsight("most_reviewed", "Most Reviewed Category",
                "No review data available.", null, 0.5, Map.of());

        // Sort by total reviews (index 3)
        Object[] mostReviewed = stats.stream()
                .filter(r -> r[3] != null)
                .max(Comparator.comparingLong(r -> ((Number) r[3]).longValue()))
                .orElse(stats.get(0));

        String category = (String) mostReviewed[0];
        long totalReviews = mostReviewed[3] != null
                ? ((Number) mostReviewed[3]).longValue() : 0;
        long appCount = ((Number) mostReviewed[2]).longValue();

        String text = String.format(
                "%s generates the most user engagement with %,d total reviews " +
                "across %,d apps — indicating strong community involvement.",
                category, totalReviews, appCount);

        return buildInsight("most_reviewed", "Highest User Engagement", text,
                "Leverage " + category + "'s engaged user base for feedback and iteration loops.",
                0.81, Map.of("category", category, "totalReviews", totalReviews,
                             "appCount", appCount));
    }

    // ── Insight 8: App Type Distribution ────────────────────────────
    private Insight computeAppTypeDistributionInsight() {
        List<Object[]> typeStats = appRepository.findAvgRatingByAppType();
        long total = typeStats.stream()
                .mapToLong(r -> ((Number) r[2]).longValue()).sum();

        Map<String, Object> supporting = new LinkedHashMap<>();
        typeStats.forEach(r -> {
            String type = (String) r[0];
            long count = ((Number) r[2]).longValue();
            double pct = total > 0 ? Math.round((double) count / total * 100.0) : 0;
            supporting.put(type, Map.of("count", count, "percentage", pct));
        });

        String text = String.format(
                "The app market currently tracks %,d apps. " +
                "Distribution across app types reflects monetization strategy trends in mobile.",
                total);

        return buildInsight("app_type_distribution", "App Type Market Distribution", text,
                "Monitor the free-to-paid ratio — shifts indicate changing monetization preferences.",
                0.75, supporting);
    }

    // ── Insight 9: Category Count ────────────────────────────────────
    private Insight computeCategoryCountInsight() {
        long categoryCount = appRepository.countDistinctCategories();
        long totalApps = appRepository.count();
        long avgAppsPerCategory = categoryCount > 0 ? totalApps / categoryCount : 0;

        String text = String.format(
                "The platform spans %d distinct app categories with an average of %,d apps per category. " +
                "Category diversity indicates broad market coverage.",
                categoryCount, avgAppsPerCategory);

        return buildInsight("category_diversity", "Category Coverage Analysis", text,
                "Target underserved categories with fewer apps but growing user demand.",
                0.72, Map.of("categoryCount", categoryCount, "totalApps", totalApps,
                             "avgAppsPerCategory", avgAppsPerCategory));
    }

    // ── Insight 10: Overall Market Summary ──────────────────────────
    private Insight computeOverallMarketInsight() {
        long totalApps = appRepository.count();
        long categories = appRepository.countDistinctCategories();
        Double avgRating = appRepository.findAverageRating();
        double avg = avgRating != null ? avgRating : 0;

        String text = String.format(
                "AppLens currently tracks %,d apps across %d categories on Google Play Store. " +
                "The overall market average rating is %.2f stars, " +
                "reflecting user satisfaction across all app types.",
                totalApps, categories, avg);

        return buildInsight("market_overview", "Market Overview", text,
                "Use this baseline to benchmark individual app performance against market standards.",
                0.95, Map.of("totalApps", totalApps, "categories", categories,
                             "avgRating", round2(avg)));
    }

    // ── Helpers ──────────────────────────────────────────────────────
    private Insight buildInsight(String key, String title, String text,
                                  String recommendation, double confidence,
                                  Map<String, Object> supporting) {
        return Insight.builder()
                .insightKey(key)
                .title(title)
                .insightText(text)
                .recommendation(recommendation)
                .confidence(BigDecimal.valueOf(confidence))
                .supportingData(supporting)
                .build();
    }

    private double round2(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}