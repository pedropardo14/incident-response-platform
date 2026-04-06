package com.accenture.iris.functions;

import com.accenture.iris.start.DbAccess;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.List;
import java.util.Map;

/**
 * Marks an incident as resolved in Postgres.
 *
 * Input headers: incident_id (from URL path parameter)
 * Output:        Map with keys: incident_id, status, notion_page_url, teams_notified
 */
@PreLoad(route = "v1.incident.resolve", instances = 10)
public class ResolveIncident implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        String incidentIdStr = headers.getOrDefault("incident_id", "");
        if (incidentIdStr.isBlank()) {
            throw new IllegalArgumentException("incident_id is required");
        }

        int incidentId = Integer.parseInt(incidentIdStr);
        int updated = DbAccess.get().update(
            "UPDATE incidents SET status = 'resolved', resolved_at = NOW() WHERE id = ? AND status = 'open'",
            incidentId
        );

        if (updated == 0) {
            throw new IllegalStateException("Incident " + incidentId + " not found or already resolved");
        }

        List<Map<String, Object>> rows = DbAccess.get().queryForList(
            "SELECT id, title, severity, status, notion_page_url, teams_notified, " +
            "TO_CHAR(triggered_at, 'YYYY-MM-DD HH24:MI:SS') AS triggered_at, " +
            "TO_CHAR(resolved_at, 'YYYY-MM-DD HH24:MI:SS') AS resolved_at " +
            "FROM incidents WHERE id = ?",
            incidentId
        );

        if (rows.isEmpty()) {
            throw new IllegalStateException("Incident " + incidentId + " not found after update");
        }

        return rows.get(0);
    }
}