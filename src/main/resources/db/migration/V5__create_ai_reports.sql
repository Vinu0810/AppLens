CREATE TABLE ai_reports (
    id              BIGSERIAL PRIMARY KEY,
    report_type     VARCHAR(50),
    data_hash       VARCHAR(64),
    question        TEXT,
    answer          TEXT NOT NULL,
    model_used      VARCHAR(50),
    tokens_used     INT,
    generated_at    TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_ai_reports_hash ON ai_reports(data_hash);