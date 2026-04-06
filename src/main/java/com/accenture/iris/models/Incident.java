package com.accenture.iris.models;

public class Incident {
    public Integer id;
    public String title;
    public String severity;      // critical, high, medium, low
    public String status;        // open, resolved
    public String workflowName;
    public String repository;
    public String branch;
    public String conclusion;    // failure, timed_out, cancelled
    public String triggeredAt;
    public String resolvedAt;
    public String notionPageUrl;
    public Boolean teamsNotified;
}