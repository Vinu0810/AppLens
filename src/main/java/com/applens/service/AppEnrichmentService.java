package com.applens.service;

import com.applens.entity.App;
import com.applens.repository.AppRepository;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AppEnrichmentService {

    private final AppRepository appRepository;

    private static final String BROWSERS_PATH =
            System.getProperty("user.home") + "/Library/Caches/ms-playwright";

    @Transactional
    public void enrichRatings(int maxApps) {
        log.info("Starting rating enrichment for up to {} apps...", maxApps);

        List<App> appsToEnrich = appRepository
                .findAll(PageRequest.of(0, maxApps))
                .getContent()
                .stream()
                .filter(a -> a.getRating() == null)
                .toList();

        log.info("Found {} apps needing rating enrichment", appsToEnrich.size());
        if (appsToEnrich.isEmpty()) {
            log.info("All apps already have ratings.");
            return;
        }

        try (Playwright playwright = Playwright.create(
                new Playwright.CreateOptions().setEnv(Map.of(
                        "PLAYWRIGHT_BROWSERS_PATH", BROWSERS_PATH,
                        "PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1"
                )))) {

            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions()
                            .setHeadless(true)
                            .setExecutablePath(Paths.get(BROWSERS_PATH,
                                    "chromium-1117", "chrome-mac", "Chromium.app",
                                    "Contents", "MacOS", "Chromium")));

            BrowserContext context = browser.newContext(
                    new Browser.NewContextOptions()
                            .setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                            .setLocale("en-US"));

            Page page = context.newPage();
            int enriched = 0;

            for (App app : appsToEnrich) {
                try {
                    String url = "https://play.google.com/store/apps/details?id=" + app.getAppId();
                    page.navigate(url, new Page.NavigateOptions()
                            .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                            .setTimeout(20000));
                    Thread.sleep(2000);

                    BigDecimal rating = extractRating(page);
                    Long reviews = extractReviewCount(page);
                    String contentRating = extractContentRating(page);

                    if (rating != null) {
                        app.setRating(rating);
                        enriched++;
                    }
                    if (reviews != null) app.setReviewCount(reviews);
                    if (contentRating != null) app.setContentRating(contentRating);

                    appRepository.save(app);

                    if (enriched % 10 == 0 && enriched > 0) {
                        log.info("Enriched {} apps so far...", enriched);
                    }
                    Thread.sleep(1500);

                } catch (Exception e) {
                    log.warn("Failed to enrich app {}: {}", app.getAppId(), e.getMessage());
                }
            }

            browser.close();
            log.info("Enrichment complete. {} apps updated with ratings.", enriched);

        } catch (Exception e) {
            log.error("Enrichment failed", e);
            throw new RuntimeException("Enrichment failed: " + e.getMessage(), e);
        }
    }

    private BigDecimal extractRating(Page page) {
        // Try CSS selectors for rating
        String[] selectors = {
            "div[class*='TT9eCd']",
            "div[itemprop='starRating'] > div",
            "span[class*='Vfppit']"
        };
        for (String sel : selectors) {
            try {
                ElementHandle el = page.querySelector(sel);
                if (el != null) {
                    String text = el.innerText().trim().replace(",", ".");
                    if (text.matches("[1-5]\\.[0-9]")) {
                        return new BigDecimal(text);
                    }
                    String aria = el.getAttribute("aria-label");
                    if (aria != null) {
                        for (String part : aria.split(" ")) {
                            try {
                                BigDecimal r = new BigDecimal(part.replace(",", "."));
                                if (r.compareTo(BigDecimal.ONE) >= 0 &&
                                    r.compareTo(BigDecimal.valueOf(5)) <= 0) {
                                    return r;
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        // Try all elements with aria-label containing "Rated"
        try {
            List<ElementHandle> elements = page.querySelectorAll("[aria-label*='Rated']");
            for (ElementHandle el : elements) {
                String aria = el.getAttribute("aria-label");
                if (aria != null && aria.contains("Rated")) {
                    for (String part : aria.split(" ")) {
                        try {
                            BigDecimal r = new BigDecimal(part.replace(",", "."));
                            if (r.compareTo(BigDecimal.ONE) >= 0 &&
                                r.compareTo(BigDecimal.valueOf(5)) <= 0) {
                                return r;
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    private Long extractReviewCount(Page page) {
        try {
            List<ElementHandle> elements = page.querySelectorAll("[aria-label*='ratings']");
            for (ElementHandle el : elements) {
                String aria = el.getAttribute("aria-label");
                if (aria != null) {
                    String num = aria.replaceAll("[^0-9]", "");
                    if (!num.isEmpty()) return Long.parseLong(num);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String extractContentRating(Page page) {
        try {
            ElementHandle el = page.querySelector("span[itemprop='contentRating']");
            if (el != null) return el.innerText().trim();
        } catch (Exception ignored) {}
        return null;
    }
}