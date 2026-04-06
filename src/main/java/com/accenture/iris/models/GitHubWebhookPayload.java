package com.accenture.iris.models;

public class GitHubWebhookPayload {
    public String action;           // completed, requested, in_progress
    public WorkflowRun workflowRun;
    public Repository repository;

    public static class WorkflowRun {
        public long id;
        public String name;
        public String headBranch;
        public String headSha;
        public String conclusion;   // failure, success, timed_out, cancelled
        public String status;
        public String htmlUrl;
        public String createdAt;
        public String updatedAt;
    }

    public static class Repository {
        public String fullName;
        public String htmlUrl;
        public String defaultBranch;
    }
}