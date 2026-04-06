package com.accenture.iris.functions;

import com.google.gson.Gson;
import okhttp3.*;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.Map;

/**
 * Posts a resolution notification to Teams when an incident is resolved.
 *
 * Input body:    resolved incident record from v1.incident.resolve
 * Output:        Map with key: teams_notified (boolean)
 */
@PreLoad(route = "v1.teams.notify.resolved", instances = 10)
public class NotifyTeamsResolved implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {
    private static final Logger log = LoggerFactory.getLogger(NotifyTeamsResolved.class);
    private static final Gson gson = new Gson();
    private static final OkHttpClient http = new OkHttpClient();
    private static final MediaType JSON = MediaType.parse("application/json");

    @Value("${teams.webhook.url:}")
    private String webhookUrl;

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> incident, int instance) {
        String title = incident.getOrDefault("title", "Incident").toString();
        String id = incident.getOrDefault("id", "?").toString();
        String resolvedAt = incident.getOrDefault("resolved_at", "now").toString();
        String notionUrl = incident.getOrDefault("notion_page_url", "").toString();

        String message = """
            {
              "@type": "MessageCard",
              "@context": "http://schema.org/extensions",
              "themeColor": "00AA00",
              "summary": "Incident Resolved: %s",
              "sections": [{
                "activityTitle": "[IRIS] RESOLVED - Incident #%s",
                "activitySubtitle": "%s",
                "facts": [
                  { "name": "Status", "value": "Resolved" },
                  { "name": "Resolved At", "value": "%s" }
                ]
              }]
            }
            """.formatted(escape(title), id, escape(title), resolvedAt);

        if (webhookUrl.isBlank()) {
            log.info("=== TEAMS RESOLUTION NOTIFICATION ===");
            log.info("Incident #{} resolved: {}", id, title);
            log.info("Resolved at: {} | Notion: {}", resolvedAt, notionUrl);
            return Map.of("teams_notified", false);
        }

        try {
            Request request = new Request.Builder()
                    .url(webhookUrl)
                    .post(RequestBody.create(message, JSON))
                    .build();
            try (Response response = http.newCall(request).execute()) {
                return Map.of("teams_notified", response.isSuccessful());
            }
        } catch (IOException e) {
            log.error("Teams resolution notification failed: {}", e.getMessage());
            return Map.of("teams_notified", false);
        }
    }

    private String escape(String s) {
        return s.replace("\"", "\\\"").replace("\n", "\\n");
    }
}