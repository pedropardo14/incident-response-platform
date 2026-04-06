package com.accenture.iris.functions;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.Map;

/**
 * No-op fork initiator. Called after the webhook parse returns 202 to GitHub.
 * Its only purpose is to be the fork entry point for the parallel triage tasks.
 *
 * Input headers: repository, sha (passed through to fork branches via model.*)
 * Output: empty (branches read from model.* directly)
 */
@PreLoad(route = "v1.triage.start", instances = 10)
public class TriageStart implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        return Map.of();
    }
}