package com.accenture.iris.functions;

import com.accenture.iris.start.DbAccess;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.List;
import java.util.Map;

/**
 * Fetches a single incident by ID.
 *
 * Input headers: incident_id
 * Output:        Single incident record as Map
 */
@PreLoad(route = "v1.incident.get", instances = 10)
public class GetIncident implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        String incidentIdStr = headers.getOrDefault("incident_id", "");
        if (incidentIdStr.isBlank()) {
            throw new IllegalArgumentException("incident_id is required");
        }

        int incidentId = Integer.parseInt(incidentIdStr);
        List<Map<String, Object>> rows = DbAccess.get().queryForList(
            """
            SELECT id, title, severity, status, workflow_name, repository, branch, conclusion,
                   TO_CHAR(triggered_at, 'YYYY-MM-DD HH24:MI:SS') AS triggered_at,
                   TO_CHAR(resolved_at, 'YYYY-MM-DD HH24:MI:SS') AS resolved_at,
                   notion_page_url, teams_notified
            FROM incidents WHERE id = ?
            """,
            incidentId
        );

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Incident " + incidentId + " not found");
        }
        return rows.get(0);
    }
}