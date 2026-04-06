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
 * Updates the Status property of an existing Notion incident page to "Resolved".
 *
 * Input body: resolved incident record (contains notion_page_url)
 * Output:     Map with key: notion_updated (boolean)
 */
@PreLoad(route = "v1.notion.update.status", instances = 10)
public class UpdateNotionStatus implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {
    private static final Logger log = LoggerFactory.getLogger(UpdateNotionStatus.class);
    private static final Gson gson = new Gson();
    private static final OkHttpClient http = new OkHttpClient();
    private static final MediaType JSON = MediaType.parse("application/json");

    @Value("${notion.token:}")
    private String notionToken;

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> incident, int instance) {
        String notionPageUrl = incident.getOrDefault("notion_page_url", "").toString();

        if (notionToken.isBlank() || notionPageUrl.isBlank()) {
            log.info("Notion not configured or no page URL — skipping status update");
            return Map.of("notion_updated", false);
        }

        String pageId = extractPageId(notionPageUrl);
        if (pageId == null) {
            log.warn("Could not extract page ID from URL: {}", notionPageUrl);
            return Map.of("notion_updated", false);
        }

        String payload = """
            {
              "properties": {
                "Status": { "select": { "name": "Resolved" } }
              }
            }
            """;

        try {
            Request request = new Request.Builder()
                    .url("https://api.notion.com/v1/pages/" + pageId)
                    .header("Authorization", "Bearer " + notionToken)
                    .header("Notion-Version", "2022-06-28")
                    .patch(RequestBody.create(payload, JSON))
                    .build();

            try (Response response = http.newCall(request).execute()) {
                boolean success = response.isSuccessful();
                if (!success) {
                    log.warn("Notion page update returned {} for page {}", response.code(), pageId);
                } else {
                    log.info("Notion page {} marked as Resolved", pageId);
                }
                return Map.of("notion_updated", success);
            }
        } catch (IOException e) {
            log.error("Notion update failed: {}", e.getMessage());
            return Map.of("notion_updated", false);
        }
    }

    /**
     * Extracts the Notion page ID from a page URL.
     * URL format: https://www.notion.so/Optional-Title-{32hexchars}
     * The page ID is the last 32 hex characters, formatted as a UUID.
     */
    private String extractPageId(String url) {
        try {
            String path = url.replaceAll("\\?.*$", "");   // strip query string
            String last = path.substring(path.lastIndexOf('/') + 1);
            // page ID is the last 32 hex chars (may be preceded by a title slug and dash)
            String hex = last.replaceAll("-", "");
            if (hex.length() < 32) return null;
            String raw = hex.substring(hex.length() - 32);
            // format as UUID: 8-4-4-4-12
            return raw.substring(0, 8) + "-"
                 + raw.substring(8, 12) + "-"
                 + raw.substring(12, 16) + "-"
                 + raw.substring(16, 20) + "-"
                 + raw.substring(20, 32);
        } catch (Exception e) {
            return null;
        }
    }
}