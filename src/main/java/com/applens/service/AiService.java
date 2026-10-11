package com.applens.service;

import com.applens.entity.AiReport;
import com.applens.entity.Insight;
import com.applens.entity.StatisticalTest;
import com.applens.repository.AiReportRepository;
import com.applens.repository.InsightRepository;
import com.applens.repository.StatisticalTestRepository;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiService {

    private final InsightRepository insightRepository;
    private final StatisticalTestRepository statisticalTestRepository;
    private final AiReportRepository aiReportRepository;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private static final String MODEL = "gemini-3.8-flash";

    // ── Executive Summary ────────────────────────────────────────────
    @Transactional
    public String generateExecutiveSummary() {
        String context = buildSummaryContext();
        String hash = sha256(context);

        Optional<AiReport> cached = aiReportRepository.findByDataHash(hash);
        if (cached.isPresent()) {
            log.info("Cache hit for executive summary (hash: {})", hash.substring(0, 8));
            return cached.get().getAnswer();
        }

        log.info("Generating executive summary via Gemini...");
        String prompt = buildSummaryPrompt(context);
        String answer = callGemini(prompt);

        AiReport report = AiReport.builder()
                .reportType("EXECUTIVE_SUMMARY")
                .dataHash(hash)
                .answer(answer)
                .modelUsed(MODEL)
                .tokensUsed(estimateTokens(prompt + answer))
                .build();
        aiReportRepository.save(report);

        return answer;
    }

    // ── Business Q&A ─────────────────────────────────────────────────
    @Transactional
    public String answerQuestion(String question) {
        String context = buildQaContext();
        String hash = sha256(context + question);

        Optional<AiReport> cached = aiReportRepository.findByDataHash(hash);
        if (cached.isPresent()) {
            log.info("Cache hit for Q&A (hash: {})", hash.substring(0, 8));
            return cached.get().getAnswer();
        }

        log.info("Answering question via Gemini: {}", question);
        String prompt = buildQaPrompt(context, question);
        String answer = callGemini(prompt);

        AiReport report = AiReport.builder()
                .reportType("QA_ANSWER")
                .dataHash(hash)
                .question(question)
                .answer(answer)
                .modelUsed(MODEL)
                .tokensUsed(estimateTokens(prompt + answer))
                .build();
        aiReportRepository.save(report);

        return answer;
    }

    // ── Gemini API Call ───────────────────────────────────────────────
    private String callGemini(String prompt) {
        try {
            Client client = Client.builder()
                    .apiKey(geminiApiKey)
                    .build();

            // correct: client.models is a field, generateContent is a method on it
            GenerateContentResponse response =
                    client.models.generateContent(MODEL, prompt, null);

            return response.text();

        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage());
            throw new RuntimeException("AI service unavailable: " + e.getMessage(), e);
        }
    }

    // ── Context Builders ─────────────────────────────────────────────
    private String buildSummaryContext() {
        List<Insight> insights = insightRepository.findAllByOrderByComputedAtDesc();
        List<StatisticalTest> tests = statisticalTestRepository.findAllByOrderByComputedAtDesc();

        StringBuilder sb = new StringBuilder();
        sb.append("=== MARKET DATA CONTEXT ===\n\n");

        sb.append("INSIGHTS:\n");
        insights.forEach(i -> sb.append("- ")
                .append(i.getTitle()).append(": ")
                .append(i.getInsightText()).append("\n")
                .append("  Recommendation: ").append(i.getRecommendation()).append("\n"));

        sb.append("\nSTATISTICAL TESTS:\n");
        tests.forEach(t -> sb.append("- ")
                .append(t.getTestName()).append(": ")
                .append(t.getGroupALabel())
                .append(" (mean=").append(t.getGroupAMean())
                .append(", n=").append(t.getGroupACount()).append(") vs ")
                .append(t.getGroupBLabel())
                .append(" (mean=").append(t.getGroupBMean())
                .append(", n=").append(t.getGroupBCount()).append(")")
                .append(" | p-value=").append(t.getPValue())
                .append(" | significant=").append(t.getIsSignificant()).append("\n"));

        return sb.toString();
    }

    private String buildQaContext() {
        return buildSummaryContext();
    }

    private String buildSummaryPrompt(String context) {
        return "You are a senior mobile app market intelligence analyst.\n"
                + "Based on the following market data, write a professional executive summary.\n\n"
                + "Structure your response with these sections:\n"
                + "**Market Overview** — 2-3 sentences on the overall market state\n"
                + "**Key Findings** — 3-4 bullet points of the most important insights\n"
                + "**Statistical Evidence** — what the data tests confirm\n"
                + "**Strategic Recommendations** — 3 actionable recommendations for app developers\n\n"
                + "Keep the tone professional and data-driven. Use specific numbers from the data.\n\n"
                + context;
    }

    private String buildQaPrompt(String context, String question) {
        return "You are a senior mobile app market intelligence analyst.\n"
                + "Answer the following business question using ONLY the provided market data.\n\n"
                + "Structure your response as:\n"
                + "**Finding:** [direct answer to the question]\n"
                + "**Supporting Data:** [specific numbers and evidence]\n"
                + "**Recommendation:** [actionable advice]\n\n"
                + "Market Data:\n"
                + context
                + "\nQuestion: " + question;
    }

    // ── Utilities ────────────────────────────────────────────────────
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 hashing failed", e);
        }
    }

    private int estimateTokens(String text) {
        return text.length() / 4;
    }
}