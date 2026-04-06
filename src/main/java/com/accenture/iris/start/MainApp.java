package com.accenture.iris.start;

import org.platformlambda.core.annotations.MainApplication;
import org.platformlambda.core.models.EntryPoint;
import org.platformlambda.core.system.AutoStart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MainApplication
public class MainApp implements EntryPoint {
    private static final Logger log = LoggerFactory.getLogger(MainApp.class);

    public static void main(String[] args) {
        AutoStart.main(args);
    }

    @Override
    public void start(String[] args) {
        initSchema();
        log.info("IRIS - Incident Response Intelligence System started");
    }

    private void initSchema() {
        var jdbc = DbAccess.get();

        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS incidents (
                id              SERIAL PRIMARY KEY,
                title           TEXT NOT NULL,
                severity        VARCHAR(10) NOT NULL,
                status          VARCHAR(20) NOT NULL DEFAULT 'open',
                workflow_name   TEXT,
                repository      TEXT,
                branch          TEXT,
                conclusion      TEXT,
                triggered_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                resolved_at     TIMESTAMPTZ,
                notion_page_url TEXT,
                teams_notified  BOOLEAN DEFAULT FALSE
            )
        """);

        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS service_metrics (
                id            SERIAL PRIMARY KEY,
                service_name  VARCHAR(100) NOT NULL,
                recorded_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                error_rate    NUMERIC(5,2) NOT NULL,
                latency_ms    INTEGER NOT NULL,
                request_count INTEGER NOT NULL
            )
        """);

        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS recent_commits (
                sha          VARCHAR(40) PRIMARY KEY,
                repository   TEXT NOT NULL,
                message      TEXT NOT NULL,
                author       TEXT NOT NULL,
                committed_at TIMESTAMPTZ NOT NULL
            )
        """);

        seedDemoMetrics(jdbc);
        log.info("Database schema initialized");
    }

    private void seedDemoMetrics(org.springframework.jdbc.core.JdbcTemplate jdbc) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM service_metrics", Integer.class);
        if (count != null && count == 0) {
            jdbc.execute("""
                INSERT INTO service_metrics (service_name, error_rate, latency_ms, request_count)
                VALUES
                  ('api-gateway',           0.5,  120, 8420),
                  ('auth-service',          1.2,  250, 3100),
                  ('user-service',          0.1,   80, 5500),
                  ('payment-service',       8.7,  950, 1200),
                  ('notification-service',  0.3,   95, 2800)
            """);
            log.info("Demo metrics seeded");
        }
    }
}