CREATE TABLE apps (
    id              BIGSERIAL PRIMARY KEY,
    app_id          VARCHAR(255) UNIQUE NOT NULL,
    name            VARCHAR(500) NOT NULL,
    store           VARCHAR(20) NOT NULL,
    category        VARCHAR(100),
    rating          DECIMAL(3,2),
    review_count    BIGINT,
    install_count   BIGINT,
    price           DECIMAL(10,2) DEFAULT 0,
    app_type        VARCHAR(10),
    size_mb         DECIMAL(10,2),
    content_rating  VARCHAR(50),
    min_age         INT,
    last_updated    DATE,
    description     TEXT,
    icon_url        VARCHAR(1000),
    scraped_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_apps_store     ON apps(store);
CREATE INDEX idx_apps_category  ON apps(category);
CREATE INDEX idx_apps_app_type  ON apps(app_type);
CREATE INDEX idx_apps_rating    ON apps(rating DESC);