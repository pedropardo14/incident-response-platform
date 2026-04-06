package com.accenture.iris.functions;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.HashMap;
import java.util.Map;

/**
 * Generates a fake GitHub Actions webhook payload for demo/testing purposes.
 * POST /api/simulate/failure  →  produces the same payload GitHub would send.
 *
 * Input body (optional overrides): workflow_name, repository, branch, conclusion
 * Output: a synthetic webhook payload matching the GitHub Actions schema
 */
@PreLoad(route = "v1.simulate.failure", instances = 10)
public class SimulateFailure implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    private static final String[] WORKFLOWS = {
        "Deploy to Production", "Run Integration Tests",
        "Build & Publish Docker Image", "Security Scan", "Database Migration"
    };
    private static final String[] REPOS = {
        "acme-corp/api-gateway", "acme-corp/auth-service",
        "acme-corp/payment-service", "acme-corp/user-service"
    };
    private static final String[] BRANCHES = { "main", "main", "main", "develop", "release/v2.1" };
    private static final String[] CONCLUSIONS = { "failure", "failure", "timed_out", "cancelled" };

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        int seed = (int) (System.currentTimeMillis() % 1000);

        String workflowName = (String) input.getOrDefault("workflow_name", WORKFLOWS[seed % WORKFLOWS.length]);
        String repository = (String) input.getOrDefault("repository", REPOS[seed % REPOS.length]);
        String branch = (String) input.getOrDefault("branch", BRANCHES[seed % BRANCHES.length]);
        String conclusion = (String) input.getOrDefault("conclusion", CONCLUSIONS[seed % CONCLUSIONS.length]);
        String sha = "abc" + Long.toHexString(System.currentTimeMillis()).substring(0, 7);

        Map<String, Object> workflowRun = new HashMap<>();
        workflowRun.put("id", System.currentTimeMillis());
        workflowRun.put("name", workflowName);
        workflowRun.put("head_branch", branch);
        workflowRun.put("head_sha", sha);
        workflowRun.put("conclusion", conclusion);
        workflowRun.put("status", "completed");
        workflowRun.put("html_url", "https://github.com/" + repository + "/actions/runs/" + seed);
        workflowRun.put("created_at", java.time.Instant.now().toString());
        workflowRun.put("updated_at", java.time.Instant.now().toString());

        Map<String, Object> repo = new HashMap<>();
        repo.put("full_name", repository);
        repo.put("html_url", "https://github.com/" + repository);
        repo.put("default_branch", "main");

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "completed");
        payload.put("workflow_run", workflowRun);
        payload.put("repository", repo);

        return payload;
    }
}