package com.accenture.iris.functions;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.List;
import java.util.Map;

/**
 * Determines incident severity based on:
 *  - workflow conclusion (timed_out / cancelled escalate severity)
 *  - branch (failures on main/master are more severe)
 *  - highest error rate across all services
 *
 * Input headers: conclusion, branch
 * Input body:    list of service metrics from FetchServiceMetrics
 * Output:        Map with keys: severity (critical/high/medium/low), reason
 */
@PreLoad(route = "v1.severity.assess", instances = 20)
public class AssessSeverity implements TypedLambdaFunction<List<Map<String, Object>>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, List<Map<String, Object>> metrics, int instance) {
        String conclusion = headers.getOrDefault("conclusion", "failure");
        String branch = headers.getOrDefault("branch", "");

        boolean isMainBranch = "main".equals(branch) || "master".equals(branch);
        boolean isTimeout = "timed_out".equals(conclusion);

        double maxErrorRate = metrics.stream()
                .mapToDouble(m -> {
                    Object rate = m.get("error_rate");
                    return rate == null ? 0.0 : Double.parseDouble(rate.toString());
                })
                .max()
                .orElse(0.0);

        String severity;
        String reason;

        if (isMainBranch && (maxErrorRate >= 5.0 || isTimeout)) {
            severity = "critical";
            reason = isTimeout
                ? "Workflow timed out on main branch (error rate: " + maxErrorRate + "%)"
                : "Main branch failure with high error rate (" + maxErrorRate + "%)";
        } else if (isMainBranch || maxErrorRate >= 5.0) {
            severity = "high";
            reason = isMainBranch
                ? "Failure on main branch (error rate: " + maxErrorRate + "%)"
                : "High service error rate (" + maxErrorRate + "%) on branch: " + branch;
        } else if (maxErrorRate >= 1.0 || "cancelled".equals(conclusion)) {
            severity = "medium";
            reason = "Elevated error rate (" + maxErrorRate + "%) or workflow cancelled on branch: " + branch;
        } else {
            severity = "low";
            reason = "Workflow " + conclusion + " on branch: " + branch + " (error rate: " + maxErrorRate + "%)";
        }

        return Map.of("severity", severity, "reason", reason);
    }
}