-- IRIS - Incident Response Intelligence System
-- Run this manually against your Postgres instance:
--   psql -U iris -d iris -f schema.sql
--
-- The schema is also created automatically on startup via MainApp.initSchema()

CREATE TABLE IF NOT EXISTS incidents (
    id              SERIAL PRIMARY KEY,
    title           TEXT NOT NULL,
    severity        VARCHAR(10) NOT NULL CHECK (severity IN ('critical', 'high', 'medium', 'low')),
    status          VARCHAR(20) NOT NULL DEFAULT 'open' CHECK (status IN ('open', 'resolved')),
    workflow_name   TEXT,
    repository      TEXT,
    branch          TEXT,
    conclusion      TEXT,
    triggered_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at     TIMESTAMPTZ,
    notion_page_url TEXT,
    teams_notified  BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS service_metrics (
    id            SERIAL PRIMARY KEY,
    service_name  VARCHAR(100) NOT NULL,
    recorded_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    error_rate    NUMERIC(5,2) NOT NULL,
    latency_ms    INTEGER NOT NULL,
    request_count INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS recent_commits (
    sha          VARCHAR(40) PRIMARY KEY,
    repository   TEXT NOT NULL,
    message      TEXT NOT NULL,
    author       TEXT NOT NULL,
    committed_at TIMESTAMPTZ NOT NULL
);

-- Seed demo metrics (simulates a Grafana/Datadog data source)
INSERT INTO service_metrics (service_name, error_rate, latency_ms, request_count)
VALUES
    ('api-gateway',           0.5,  120, 8420),
    ('auth-service',          1.2,  250, 3100),
    ('user-service',          0.1,   80, 5500),
    ('payment-service',       8.7,  950, 1200),
    ('notification-service',  0.3,   95, 2800)
ON CONFLICT DO NOTHING;