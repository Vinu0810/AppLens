CREATE TABLE insights (
    id              BIGSERIAL PRIMARY KEY,
    insight_key     VARCHAR(100) NOT NULL,
    title           VARCHAR(255) NOT NULL,
    insight_text    TEXT NOT NULL,
    recommendation  TEXT,
    confidence      DECIMAL(4,3),
    supporting_data JSONB,
    computed_at     TIMESTAMP DEFAULT NOW()
);