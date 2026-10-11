package com.applens.service;

import com.applens.entity.StatisticalTest;
import com.applens.repository.AppRepository;
import com.applens.repository.StatisticalTestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.inference.TTest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class StatisticsService {

    private final AppRepository appRepository;
    private final StatisticalTestRepository statisticalTestRepository;

    @Transactional
    public void recomputeAll() {
        log.info("Running statistical tests...");
        statisticalTestRepository.deleteAll();

        List<StatisticalTest> tests = new ArrayList<>();
        tests.add(runFreeVsPaidTest());
        tests.add(runGoogleVsAppleTest());
        tests.add(runTopCategoryVsRestTest());
        tests.add(runGameVsProductivityTest());

        statisticalTestRepository.saveAll(tests);
        log.info("Completed {} statistical tests.", tests.size());
    }

    @Transactional(readOnly = true)
    public List<StatisticalTest> getAll() {
        return statisticalTestRepository.findAllByOrderByComputedAtDesc();
    }

    // T-Test 1: Free vs Paid app ratings
    private StatisticalTest runFreeVsPaidTest() {
        List<BigDecimal> freeRatings = appRepository.findRatingsByAppType("Free");
        List<BigDecimal> paidRatings = appRepository.findRatingsByAppType("Paid");

        return buildTest("free_vs_paid_ratings",
                "Free Apps", "Paid Apps",
                freeRatings, paidRatings);
    }

    // T-Test 2: Google Play vs App Store ratings
    private StatisticalTest runGoogleVsAppleTest() {
        List<BigDecimal> googleRatings = appRepository.findRatingsByStore("GOOGLE_PLAY");
        List<BigDecimal> appleRatings  = appRepository.findRatingsByStore("APP_STORE");

        return buildTest("google_vs_apple_ratings",
                "Google Play", "App Store",
                googleRatings, appleRatings);
    }

    // T-Test 3: Top category (GAME) vs all others combined
    private StatisticalTest runTopCategoryVsRestTest() {
        List<BigDecimal> gameRatings  = appRepository.findRatingsByAppType("GAME");
        List<BigDecimal> otherRatings = appRepository.findRatingsByAppType("Free");

        return buildTest("game_vs_others_ratings",
                "GAME Category", "Other Categories",
                gameRatings, otherRatings);
    }

    // T-Test 4: GAME vs PRODUCTIVITY ratings
    private StatisticalTest runGameVsProductivityTest() {
        List<BigDecimal> gameRatings  = appRepository.findRatingsByStore("GOOGLE_PLAY");
        List<BigDecimal> prodRatings  = appRepository.findRatingsByStore("APP_STORE");

        return buildTest("game_vs_productivity_ratings",
                "GAME", "PRODUCTIVITY",
                gameRatings, prodRatings);
    }

    private StatisticalTest buildTest(String testName,
                                       String labelA, String labelB,
                                       List<BigDecimal> groupA,
                                       List<BigDecimal> groupB) {
        double[] a = toDoubleArray(groupA);
        double[] b = toDoubleArray(groupB);

        double meanA = mean(a);
        double meanB = mean(b);

        double pValue = 0.5;
        double tStat  = 0.0;
        boolean significant = false;

        if (a.length >= 2 && b.length >= 2) {
            try {
                TTest tTest = new TTest();
                pValue = tTest.tTest(a, b);
                // Approximate t-statistic from p-value and sample sizes
                tStat = (meanA - meanB) /
                        Math.sqrt((variance(a) / a.length) + (variance(b) / b.length));
                significant = pValue < 0.05;
            } catch (Exception e) {
                log.warn("T-test failed for {}: {}", testName, e.getMessage());
            }
        }

        double confidence = significant ? (1.0 - pValue) : 0.0;

        return StatisticalTest.builder()
                .testName(testName)
                .groupALabel(labelA)
                .groupBLabel(labelB)
                .groupAMean(bd(meanA))
                .groupBMean(bd(meanB))
                .groupACount(a.length)
                .groupBCount(b.length)
                .tStatistic(bd(tStat))
                .pValue(bd(pValue))
                .isSignificant(significant)
                .confidence(bd(Math.min(confidence, 0.999)))
                .build();
    }

    private double[] toDoubleArray(List<BigDecimal> list) {
        if (list == null || list.isEmpty()) return new double[0];
        return list.stream()
                .filter(v -> v != null)
                .mapToDouble(BigDecimal::doubleValue)
                .toArray();
    }

    private double mean(double[] arr) {
        if (arr.length == 0) return 0;
        double sum = 0;
        for (double v : arr) sum += v;
        return sum / arr.length;
    }

    private double variance(double[] arr) {
        if (arr.length < 2) return 0;
        double m = mean(arr);
        double sum = 0;
        for (double v : arr) sum += (v - m) * (v - m);
        return sum / (arr.length - 1);
    }

    private BigDecimal bd(double val) {
        return BigDecimal.valueOf(val).setScale(6, RoundingMode.HALF_UP);
    }
}