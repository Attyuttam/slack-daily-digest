package com.slackapp.dailydigestbot.application.llm;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GithubSummarizer{
    private final SummarizerClient summarizerClient;
    public String summarizeMessages(JsonNode prsCreated, JsonNode prsToBeReviewed, JsonNode assignedIssues, JsonNode createdIssues) {

        String prompt = """
            You are an engineering productivity assistant.

            Summarize the following GitHub activity into a concise engineering digest.
            Focus on urgency, pending actions, blockers, and workload.

            --------------------------
            📌 PRs created by user:
            %s

            --------------------------
            📌 PRs requiring review:
            %s

            --------------------------
            📌 Issues assigned to user:
            %s

            --------------------------
            📌 Issues created by user:
            %s

            --------------------------

            Output format:
            - High-level summary
            - What needs attention
            - Pending reviews
            - Pending merges
            - Priority suggestions

            Do NOT include raw JSON. Keep it crisp and actionable.
            """.formatted(
                prsCreated.toPrettyString(),
                prsToBeReviewed.toPrettyString(),
                assignedIssues.toPrettyString(),
                createdIssues.toPrettyString()
        );

        return summarizerClient.summarize(prompt);
    }
}
