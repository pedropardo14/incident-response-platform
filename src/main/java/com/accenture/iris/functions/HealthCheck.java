package com.accenture.iris.functions;

import com.accenture.iris.start.DbAccess;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.Map;

/**
 * Health check function registered with Mercury's health dependency system.
 * Also used as the scheduled post-deploy checker — queries Postgres to confirm
 * connectivity and returns open incident count.
 *
 * Route: iris.health  (used by mandatory.health.dependencies in application.properties)
 */
@PreLoad(route = "iris.health", instances = 5)
public class HealthCheck implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        Integer openCount = DbAccess.get().queryForObject(
            "SELECT COUNT(*) FROM incidents WHERE status = 'open'", Integer.class);
        Integer totalCount = DbAccess.get().queryForObject(
            "SELECT COUNT(*) FROM incidents", Integer.class);

        return Map.of(
            "status", "UP",
            "open_incidents", openCount == null ? 0 : openCount,
            "total_incidents", totalCount == null ? 0 : totalCount,
            "database", "connected"
        );
    }
}