package com.accenture.iris.models;

import java.util.List;
import java.util.Map;

/**
 * The assembled triage context built up through the incident flow.
 * Carried in the Mercury state machine (model.*) as plain maps.
 */
public class IncidentContext {
    public String workflowName;
    public String repository;
    public String branch;
    public String conclusion;
    public String workflowUrl;
    public String severity;
    public List<Map<String, Object>> recentCommits;
    public List<Map<String, Object>> errorMetrics;
    public Integer incidentId;
    public String notionPageUrl;
}