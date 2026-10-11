CREATE TABLE statistical_tests (
    id              BIGSERIAL PRIMARY KEY,
    test_name       VARCHAR(100) NOT NULL,
    group_a_label   VARCHAR(100),
    group_b_label   VARCHAR(100),
    group_a_mean    DECIMAL(10,4),
    group_b_mean    DECIMAL(10,4),
    group_a_count   INT,
    group_b_count   INT,
    t_statistic     DECIMAL(10,4),
    p_value         DECIMAL(10,6),
    is_significant  BOOLEAN,
    confidence      DECIMAL(4,3),
    computed_at     TIMESTAMP DEFAULT NOW()
);