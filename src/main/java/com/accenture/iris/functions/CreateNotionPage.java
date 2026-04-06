package com.accenture.iris.functions;

import com.google.gson.Gson;
import okhttp3.*;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Creates a structured incident report page in Notion.
 * Falls back to console logging if tokens are not configured.
 *
 * Input headers: incident_id, severity, workflow_name, repository, branch,
 *                conclusion, workflow_url
 * Input body:    list of recent commits [{sha, message, author, date, url}]
 * Output:        Map with keys: notion_page_url (string)
 */
@PreLoad(route = "v1.notion.create.page", instances = 10)
public class CreateNotionPage implements TypedLambdaFunction<List<Map<String, Object>>, Map<String, Object>> {
    private static final Logger log = LoggerFactory.getLogger(CreateNotionPage.class);
    private static final Gson gson = new Gson();
    private static final OkHttpClient http = new OkHttpClient();
    private static final MediaType JSON = MediaType.parse("application/json");

    @Value("${notion.token:}")
    private String notionToken;

    @Value("${notion.database.id:}")
    private String databaseId;

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, List<Map<String, Object>> commits, int instance) {
        String incidentId = headers.getOrDefault("incident_id", "?");
        String severity = headers.getOrDefault("severity", "unknown");
        String workflowName = headers.getOrDefault("workflow_name", "unknown");
        String repository = headers.getOrDefault("repository", "unknown");
        String branch = headers.getOrDefault("branch", "unknown");
        String conclusion = headers.getOrDefault("conclusion", "failure");
        String workflowUrl = headers.getOrDefault("workflow_url", "");

        String pageTitle = "[IRIS-" + incidentId + "] " + workflowName + " failed on " + branch;

        if (notionToken.isBlank() || databaseId.isBlank()) {
            log.info("=== NOTION PAGE (no token configured) ===");
            log.info("Would create page: {}", pageTitle);
            log.info("Severity: {} | Repository: {} | Conclusion: {}", severity, repository, conclusion);
            log.info("Commits in scope: {}", commits.size());
            log.info("==========================================");
            return Map.of("notion_page_url", "https://notion.so/demo-page-" + incidentId);
        }

        try {
            String payload = buildPagePayload(pageTitle, incidentId, severity,
                    workflowName, repository, branch, conclusion, workflowUrl, commits);

            Request request = new Request.Builder()
                    .url("https://api.notion.com/v1/pages")
                    .header("Authorization", "Bearer " + notionToken)
                    .header("Notion-Version", "2022-06-28")
                    .post(RequestBody.create(payload, JSON))
                    .build();

            try (Response response = http.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    log.warn("Notion API returned {}", response.code());
                    return Map.of("notion_page_url", "");
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> result = gson.fromJson(response.body().string(), Map.class);
                String pageUrl = result.getOrDefault("url", "").toString();
                return Map.of("notion_page_url", pageUrl);
            }
        } catch (IOException e) {
            log.error("Notion API call failed: {}", e.getMessage());
            return Map.of("notion_page_url", "");
        }
    }

    private String buildPagePayload(String title, String incidentId, String severity,
                                     String workflowName, String repository, String branch,
                                     String conclusion, String workflowUrl,
                                     List<Map<String, Object>> commits) {
        StringBuilder commitBullets = new StringBuilder();
        for (Map<String, Object> c : commits) {
            commitBullets.append("""
                {"object":"block","type":"bulleted_list_item","bulleted_list_item":{"rich_text":[{"type":"text","text":{"content":"%s by %s (%s)"}}]}},
                """.formatted(
                    escape(c.getOrDefault("message", "").toString()),
                    escape(c.getOrDefault("author", "").toString()),
                    escape(c.getOrDefault("sha", "").toString())
                ));
        }

        return """
            {
              "parent": { "database_id": "%s" },
              "properties": {
                "Name": { "title": [{ "text": { "content": "%s" } }] },
                "Severity": { "select": { "name": "%s" } },
                "Status": { "select": { "name": "Open" } },
                "Repository": { "rich_text": [{ "text": { "content": "%s" } }] }
              },
              "children": [
                {"object":"block","type":"heading_2","heading_2":{"rich_text":[{"type":"text","text":{"content":"Incident Summary"}}]}},
                {"object":"block","type":"paragraph","paragraph":{"rich_text":[{"type":"text","text":{"content":"Workflow: %s\\nRepository: %s\\nBranch: %s\\nConclusion: %s\\nDetected: %s"}}]}},
                {"object":"block","type":"heading_2","heading_2":{"rich_text":[{"type":"text","text":{"content":"Recent Commits in Scope"}}]}},
                %s
                {"object":"block","type":"heading_2","heading_2":{"rich_text":[{"type":"text","text":{"content":"Links"}}]}},
                {"object":"block","type":"paragraph","paragraph":{"rich_text":[{"type":"text","text":{"content":"Workflow Run: %s","link":{"url":"%s"}}}]}},
                {"object":"block","type":"heading_2","heading_2":{"rich_text":[{"type":"text","text":{"content":"Resolution Notes"}}]}},
                {"object":"block","type":"paragraph","paragraph":{"rich_text":[{"type":"text","text":{"content":"(Update this section when resolved)"}}]}}
              ]
            }
            """.formatted(
                databaseId, escape(title), severity, escape(repository),
                escape(workflowName), escape(repository), escape(branch), conclusion,
                Instant.now().toString(),
                commitBullets,
                escape(workflowUrl), escape(workflowUrl)
            );
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}