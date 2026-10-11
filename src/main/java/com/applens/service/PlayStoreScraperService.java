package com.applens.service;

import com.applens.dto.ScrapedAppData;
import com.applens.entity.App;
import com.applens.entity.ScrapeRun;
import com.applens.exception.ScraperException;
import com.applens.repository.AppRepository;
import com.applens.repository.ScrapeRunRepository;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlayStoreScraperService {

    private final AppRepository appRepository;
    private final ScrapeRunRepository scrapeRunRepository;

    private static final List<String> CATEGORIES = List.of(
            "GAME", "PRODUCTIVITY", "EDUCATION", "FINANCE", "HEALTH_AND_FITNESS"
    );

    private static final String BROWSERS_PATH =
            System.getProperty("user.home") + "/Library/Caches/ms-playwright";

    @Transactional
    public ScrapeRun scrapeTopApps() {
        ScrapeRun run = scrapeRunRepository.save(ScrapeRun.started("GOOGLE_PLAY"));
        int created = 0;
        int updated = 0;

        log.info("Starting Play Store scrape...");

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
                            .setViewportSize(1280, 900)
                            .setLocale("en-US"));

            Page page = context.newPage();

            for (String category : CATEGORIES) {
                log.info("Scraping category: {}", category);
                try {
                    List<ScrapedAppData> apps = scrapeCategory(page, category);
                    for (ScrapedAppData data : apps) {
                        try {
                            boolean isNew = upsertApp(data);
                            if (isNew) created++; else updated++;
                        } catch (Exception e) {
                            log.warn("Failed to upsert {}: {}", data.appId(), e.getMessage());
                        }
                    }
                    log.info("Category {} done: {} apps scraped", category, apps.size());
                    Thread.sleep(2500);
                } catch (Exception e) {
                    log.error("Failed category {}: {}", category, e.getMessage());
                }
            }

            browser.close();

        } catch (Exception e) {
            log.error("Scrape failed", e);
            scrapeRunRepository.save(run.failed(e.getMessage()));
            throw new ScraperException("Scrape failed: " + e.getMessage(), e);
        }

        scrapeRunRepository.save(run.completed(created, updated));
        log.info("Scrape complete. Created: {}, Updated: {}", created, updated);
        return run;
    }

    private List<ScrapedAppData> scrapeCategory(Page page, String category)
            throws InterruptedException {

        String url = "https://play.google.com/store/apps/category/" + category;
        List<ScrapedAppData> results = new ArrayList<>();

        page.navigate(url, new Page.NavigateOptions()
                .setWaitUntil(WaitUntilState.NETWORKIDLE)
                .setTimeout(45000));
        Thread.sleep(3000);

        // Scroll to load more content
        for (int i = 0; i < 4; i++) {
            page.evaluate("window.scrollBy(0, 600)");
            Thread.sleep(800);
        }

        // Get all app detail links — we confirmed 104 exist on this page
        List<ElementHandle> appLinks = page.querySelectorAll("a[href*='/store/apps/details']");
        log.info("Found {} app links in {}", appLinks.size(), category);

        for (ElementHandle link : appLinks) {
            try {
                ScrapedAppData app = extractApp(link, category);
                if (app != null && !isDuplicate(results, app.appId())) {
                    results.add(app);
                }
            } catch (Exception e) {
                // skip this link, continue
            }
        }

        return results;
    }

    private ScrapedAppData extractApp(ElementHandle link, String category) {
        // Get app ID from href  e.g. /store/apps/details?id=com.example.app
        String href = link.getAttribute("href");
        if (href == null || !href.contains("id=")) return null;

        String appId = href.substring(href.indexOf("id=") + 3);
        if (appId.contains("&")) appId = appId.substring(0, appId.indexOf("&"));
        if (appId.isBlank()) return null;

        // App name — try aria-label first (most reliable), then inner text
        String name = null;
        try {
            String aria = link.getAttribute("aria-label");
            if (aria != null && !aria.isBlank()) name = aria.trim();
        } catch (Exception ignored) {}

        if (name == null || name.isBlank()) {
            try {
                // Find the title element inside the card
                ElementHandle titleEl = link.querySelector(
                        "span.DdYX5, div.vWM94c, span[jsname='r4nke'], " +
                        "div[class*='WsMG1c'], span[class*='ubGTjb']");
                if (titleEl != null) name = titleEl.innerText().trim();
            } catch (Exception ignored) {}
        }

        if (name == null || name.isBlank()) {
            // Last resort: get all text from the link
            try {
                String text = link.innerText().trim();
                if (!text.isBlank()) {
                    // Take first line only
                    name = text.split("\n")[0].trim();
                }
            } catch (Exception ignored) {}
        }

        if (name == null || name.isBlank()) name = appId;

        // Rating
        BigDecimal rating = null;
        try {
            ElementHandle ratingEl = link.querySelector(
                    "span.w2kbF, div[aria-label*='Rated'], span[class*='w2kbF']");
            if (ratingEl != null) {
                String ratingText = ratingEl.innerText().trim().replace(",", ".");
                if (!ratingText.isBlank() && ratingText.matches("[0-9.]+")) {
                    BigDecimal r = new BigDecimal(ratingText);
                    if (r.compareTo(BigDecimal.ZERO) > 0 &&
                        r.compareTo(BigDecimal.valueOf(5)) <= 0) {
                        rating = r;
                    }
                }
            }
        } catch (Exception ignored) {}

        // Icon URL
        String iconUrl = null;
        try {
            ElementHandle img = link.querySelector("img");
            if (img != null) {
                String src = img.getAttribute("src");
                if (src != null && src.startsWith("http")) iconUrl = src;
            }
        } catch (Exception ignored) {}

        return new ScrapedAppData(
                appId, name, "GOOGLE_PLAY", category,
                rating, null, null,
                BigDecimal.ZERO, "Free",
                null, iconUrl, null
        );
    }

    private boolean isDuplicate(List<ScrapedAppData> list, String appId) {
        return list.stream().anyMatch(a -> a.appId().equals(appId));
    }

    @Transactional
    public boolean upsertApp(ScrapedAppData data) {
        return appRepository.findByAppId(data.appId())
                .map(existing -> {
                    existing.setName(data.name());
                    existing.setCategory(data.category());
                    if (data.rating() != null) existing.setRating(data.rating());
                    if (data.iconUrl() != null) existing.setIconUrl(data.iconUrl());
                    appRepository.save(existing);
                    return false;
                })
                .orElseGet(() -> {
                    App app = App.builder()
                            .appId(data.appId())
                            .name(data.name())
                            .store(data.store())
                            .category(data.category())
                            .rating(data.rating())
                            .price(data.price())
                            .appType(data.appType())
                            .iconUrl(data.iconUrl())
                            .build();
                    appRepository.save(app);
                    return true;
                });
    }
}