package com.accenture.iris.functions;

import com.accenture.iris.start.DbAccess;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import java.sql.PreparedStatement;
import java.util.Map;
import java.util.Objects;

/**
 * Persists the incident record to Postgres.
 *
 * Input headers: workflow_name, repository, branch, conclusion, severity
 * Output:        Map with key: incident_id (integer)
 */
@PreLoad(route = "v1.incident.store", instances = 10)
public class StoreIncident implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        String workflowName = headers.getOrDefault("workflow_name", "unknown");
        String repository = headers.getOrDefault("repository", "unknown");
        String branch = headers.getOrDefault("branch", "unknown");
        String conclusion = headers.getOrDefault("conclusion", "failure");
        String severity = headers.getOrDefault("severity", "medium");

        String title = "[" + severity.toUpperCase() + "] " + workflowName + " failed on " + branch;

        var keyHolder = new GeneratedKeyHolder();
        DbAccess.get().update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO incidents (title, severity, status, workflow_name, repository, branch, conclusion) " +
                "VALUES (?, ?, 'open', ?, ?, ?, ?)",
                new String[]{"id"}
            );
            ps.setString(1, title);
            ps.setString(2, severity);
            ps.setString(3, workflowName);
            ps.setString(4, repository);
            ps.setString(5, branch);
            ps.setString(6, conclusion);
            return ps;
        }, keyHolder);

        int incidentId = Objects.requireNonNull(keyHolder.getKey()).intValue();
        return Map.of("incident_id", incidentId, "title", title);
    }
}