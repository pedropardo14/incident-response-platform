package com.accenture.iris.functions;

import com.accenture.iris.start.DbAccess;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.List;
import java.util.Map;

/**
 * Returns all incidents ordered by most recent first.
 * Supports optional ?status=open|resolved query filter passed via header.
 *
 * Input headers: status (optional filter)
 * Output:        List of incident records
 */
@PreLoad(route = "v1.incident.list", instances = 10)
public class ListIncidents implements TypedLambdaFunction<Map<String, Object>, List<Map<String, Object>>> {

    @Override
    public List<Map<String, Object>> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        String statusFilter = headers.getOrDefault("status_filter", "");

        String sql = """
            SELECT id, title, severity, status, workflow_name, repository, branch, conclusion,
                   TO_CHAR(triggered_at, 'YYYY-MM-DD HH24:MI:SS') AS triggered_at,
                   TO_CHAR(resolved_at, 'YYYY-MM-DD HH24:MI:SS') AS resolved_at,
                   notion_page_url, teams_notified
            FROM incidents
            """ + (statusFilter.isBlank() ? "" : "WHERE status = '" + statusFilter + "'") + """
            ORDER BY triggered_at DESC
            LIMIT 50
        """;

        return DbAccess.get().queryForList(sql);
    }
}