package com.accenture.iris.functions;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.HashMap;
import java.util.Map;

/**
 * Receives the raw GitHub Actions webhook body and extracts
 * the fields we care about into a flat map for the flow state machine.
 *
 * Input:  raw webhook body as Map<String, Object>
 * Output: flat incident seed map
 */
@PreLoad(route = "v1.github.parse.webhook", instances = 10)
public class ParseGitHubWebhook implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        String action = getString(input, "action");
        if (!"completed".equals(action)) {
            throw new IllegalArgumentException("Ignoring non-completed webhook action: " + action);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> run = (Map<String, Object>) input.get("workflow_run");
        @SuppressWarnings("unchecked")
        Map<String, Object> repo = (Map<String, Object>) input.get("repository");

        if (run == null || repo == null) {
            throw new IllegalArgumentException("Missing workflow_run or repository in webhook payload");
        }

        String conclusion = getString(run, "conclusion");
        // Only process failures — success webhooks are silently ignored
        if ("success".equals(conclusion) || "skipped".equals(conclusion)) {
            throw new IllegalArgumentException("Workflow succeeded — no incident needed");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("workflow_name", getString(run, "name"));
        result.put("repository", getString(repo, "full_name"));
        result.put("branch", getString(run, "head_branch"));
        result.put("conclusion", conclusion);
        result.put("workflow_url", getString(run, "html_url"));
        result.put("head_sha", getString(run, "head_sha"));
        return result;
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val == null ? "" : val.toString();
    }
}