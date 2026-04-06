package com.accenture.iris.functions;

import com.accenture.iris.start.DbAccess;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.Map;

/**
 * Updates the incident record in Postgres with Notion page URL and
 * teams_notified flag after the fork-n-join completes.
 *
 * Input headers: incident_id, notion_page_url, teams_notified
 * Output:        Map with key: updated (boolean)
 */
@PreLoad(route = "v1.incident.update", instances = 10)
public class UpdateIncidentRecord implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        String incidentIdStr = headers.getOrDefault("incident_id", "0");
        String notionPageUrl = headers.getOrDefault("notion_page_url", "");
        String teamsNotifiedStr = headers.getOrDefault("teams_notified", "false");

        int incidentId = Integer.parseInt(incidentIdStr);
        boolean teamsNotified = Boolean.parseBoolean(teamsNotifiedStr);

        int updated = DbAccess.get().update(
            "UPDATE incidents SET notion_page_url = ?, teams_notified = ? WHERE id = ?",
            notionPageUrl.isBlank() ? null : notionPageUrl,
            teamsNotified,
            incidentId
        );

        return Map.of("updated", updated > 0, "incident_id", incidentId);
    }
}