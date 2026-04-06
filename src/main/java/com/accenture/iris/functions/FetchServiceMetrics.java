package com.accenture.iris.functions;

import com.accenture.iris.start.DbAccess;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.List;
import java.util.Map;

/**
 * Pulls the latest error metrics for all services from Postgres.
 * These are simulated metrics representing what you would normally
 * fetch from Grafana/Datadog during triage.
 *
 * Input:  ignored
 * Output: list of [{service_name, error_rate, latency_ms, request_count, recorded_at}]
 */
@PreLoad(route = "v1.metrics.fetch", instances = 20)
public class FetchServiceMetrics implements TypedLambdaFunction<Map<String, Object>, List<Map<String, Object>>> {

    @Override
    public List<Map<String, Object>> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        return DbAccess.get().queryForList("""
            SELECT DISTINCT ON (service_name)
                service_name,
                CAST(error_rate AS FLOAT) AS error_rate,
                latency_ms,
                request_count,
                TO_CHAR(recorded_at, 'YYYY-MM-DD HH24:MI:SS') AS recorded_at
            FROM service_metrics
            ORDER BY service_name, recorded_at DESC
        """);
    }
}