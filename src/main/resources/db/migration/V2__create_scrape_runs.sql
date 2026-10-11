CREATE TABLE scrape_runs (
    id              BIGSERIAL PRIMARY KEY,
    started_at      TIMESTAMP NOT NULL,
    completed_at    TIMESTAMP,
    status          VARCHAR(20),
    apps_scraped    INT DEFAULT 0,
    apps_created    INT DEFAULT 0,
    apps_updated    INT DEFAULT 0,
    error_message   TEXT,
    store           VARCHAR(20)
);