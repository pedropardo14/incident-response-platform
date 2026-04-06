package com.accenture.iris.functions;

import com.google.gson.Gson;
import okhttp3.*;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Posts an Adaptive Card to a Microsoft Teams channel via an incoming webhook.
 * Falls back to console logging if the webhook URL is not configured.
 *
 * Input headers: incident_id, severity, workflow_name, repository, branch,
 *                conclusion, workflow_url
 * Input body:    list of service metrics
 * Output:        Map with key: teams_notified (boolean)
 */
@PreLoad(route = "v1.teams.notify", instances = 20)
public class NotifyTeams implements TypedLambdaFunction<List<Map<String, Object>>, Map<String, Object>> {
    private static final Logger log = LoggerFactory.getLogger(NotifyTeams.class);
    private static final Gson gson = new Gson();
    private static final OkHttpClient http = new OkHttpClient();
    private static final MediaType JSON = MediaType.parse("application/json");

    @Value("${teams.webhook.url:}")
    private String webhookUrl;

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, List<Map<String, Object>> metrics, int instance) {
        String incidentId = headers.getOrDefault("incident_id", "?");
        String severity = headers.getOrDefault("severity", "unknown");
        String workflowName = headers.getOrDefault("workflow_name", "unknown");
        String repository = headers.getOrDefault("repository", "unknown");
        String branch = headers.getOrDefault("branch", "unknown");
        String conclusion = headers.getOrDefault("conclusion", "failure");
        String workflowUrl = headers.getOrDefault("workflow_url", "");

        String themeColor = switch (severity) {
            case "critical" -> "FF0000";
            case "high" -> "FF6600";
            case "medium" -> "FFA500";
            default -> "FFCC00";
        };

        String metricsText = buildMetricsSummary(metrics);
        String title = "[IRIS] " + severity.toUpperCase() + " - " + workflowName + " failed";
        String body = buildAdaptiveCard(title, themeColor, incidentId, severity,
                workflowName, repository, branch, conclusion, workflowUrl, metricsText);

        if (webhookUrl.isBlank()) {
            log.info("=== TEAMS NOTIFICATION (no webhook configured) ===");
            log.info("Title: {}", title);
            log.info("Incident #{} | Severity: {} | Branch: {} | Conclusion: {}", incidentId, severity, branch, conclusion);
            log.info("Metrics: {}", metricsText);
            log.info("=================================================");
            return Map.of("teams_notified", false);
        }

        try {
            Request request = new Request.Builder()
                    .url(webhookUrl)
                    .post(RequestBody.create(body, JSON))
                    .build();
            try (Response response = http.newCall(request).execute()) {
                boolean success = response.isSuccessful();
                if (!success) {
                    log.warn("Teams webhook returned {}", response.code());
                }
                return Map.of("teams_notified", success);
            }
        } catch (IOException e) {
            log.error("Teams webhook call failed: {}", e.getMessage());
            return Map.of("teams_notified", false);
        }
    }

    private String buildMetricsSummary(List<Map<String, Object>> metrics) {
        if (metrics == null || metrics.isEmpty()) return "No metrics available";
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> m : metrics) {
            double errorRate = Double.parseDouble(m.getOrDefault("error_rate", 0).toString());
            if (errorRate > 1.0) {
                sb.append(m.get("service_name")).append(": ").append(errorRate).append("% errors | ");
            }
        }
        return sb.isEmpty() ? "All services within normal thresholds" : sb.toString().stripTrailing().replaceAll("\\|\\s*$", "");
    }

    private String buildAdaptiveCard(String title, String themeColor, String incidentId,
                                      String severity, String workflowName, String repository,
                                      String branch, String conclusion, String workflowUrl,
                                      String metricsText) {
        return """
            {
              "@type": "MessageCard",
              "@context": "http://schema.org/extensions",
              "themeColor": "%s",
              "summary": "%s",
              "sections": [{
                "activityTitle": "%s",
                "activitySubtitle": "Incident #%s",
                "facts": [
                  { "name": "Severity", "value": "%s" },
                  { "name": "Workflow", "value": "%s" },
                  { "name": "Repository", "value": "%s" },
                  { "name": "Branch", "value": "%s" },
                  { "name": "Conclusion", "value": "%s" },
                  { "name": "Elevated Metrics", "value": "%s" }
                ],
                "markdown": true
              }],
              "potentialAction": [{
                "@type": "OpenUri",
                "name": "View Workflow Run",
                "targets": [{ "os": "default", "uri": "%s" }]
              }]
            }
            """.formatted(themeColor, title, title, incidentId, severity,
                workflowName, repository, branch, conclusion, metricsText, workflowUrl);
    }
}