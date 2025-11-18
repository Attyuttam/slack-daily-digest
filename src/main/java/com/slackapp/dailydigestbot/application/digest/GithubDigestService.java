package com.slackapp.dailydigestbot.application.digest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.slackapp.dailydigestbot.application.SlackClientService;
import com.slackapp.dailydigestbot.application.llm.GithubSummarizer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class GithubDigestService {
    private final RestTemplate restTemplate;
    private final HttpHeaders githubHeaders;
    private final ObjectMapper mapper;
    private final GithubSummarizer githubSummarizer;
    private final SlackClientService slackClientService;

    public void generateAndSendDigestToChannel(String githubUser, String repo, String channelId) throws Exception {
        // 1. Fetch GitHub data
        JsonNode created = getOpenPRsCreatedBy(githubUser, repo);
        JsonNode review = getPRsToReview(githubUser, repo);
        JsonNode assigned = getAssignedIssues(githubUser, repo);
        JsonNode createdIssues = getIssuesCreatedBy(githubUser, repo);

        // 2. Summarize with AI
        String summary = githubSummarizer.summarizeMessages(created, review, assigned, createdIssues);

        // 3. Post directly to Slack
        slackClientService.postMessage(channelId, summary);
    }

    //-----------------------------------------------------
    // 1. Pull requests created by user (NOT MERGED)
    //-----------------------------------------------------
    public JsonNode getOpenPRsCreatedBy(String username, String repo) {
        String url = String.format(
                "https://api.github.com/repos/%s/pulls?state=open",
                repo
        );

        HttpEntity<Void> e = new HttpEntity<>(githubHeaders);
        ResponseEntity<JsonNode> res = restTemplate.exchange(url, HttpMethod.GET, e, JsonNode.class);

        ArrayNode filtered = mapper.createArrayNode();
        for (JsonNode pr : res.getBody()) {
            if (pr.get("user").get("login").asText().equals(username))
                filtered.add(pr);
        }
        return filtered;
    }

    //-----------------------------------------------------
    // 2. Pull requests requiring this user as reviewer
    //-----------------------------------------------------
    public JsonNode getPRsToReview(String username, String repo) {
        String url = String.format(
                "https://api.github.com/repos/%s/pulls?state=open",
                repo
        );

        HttpEntity<Void> e = new HttpEntity<>(githubHeaders);
        ResponseEntity<JsonNode> res = restTemplate.exchange(url, HttpMethod.GET, e, JsonNode.class);

        ArrayNode filtered = new ObjectMapper().createArrayNode();
        for (JsonNode pr : res.getBody()) {
            for (JsonNode reviewer : pr.get("requested_reviewers")) {
                if (reviewer.get("login").asText().equals(username))
                    filtered.add(pr);
            }
        }
        return filtered;
    }

    //-----------------------------------------------------
    // 3. Issues assigned to user
    //-----------------------------------------------------
    public JsonNode getAssignedIssues(String username, String repo) {
        String url = String.format(
                "https://api.github.com/repos/%s/issues?state=open&assignee=%s",
                repo, username
        );

        HttpEntity<Void> e = new HttpEntity<>(githubHeaders);
        ResponseEntity<JsonNode> res = restTemplate.exchange(url, HttpMethod.GET, e, JsonNode.class);

        return res.getBody();
    }

    //-----------------------------------------------------
    // 4. Issues created by the user
    //-----------------------------------------------------
    public JsonNode getIssuesCreatedBy(String username, String repo) {
        String url = String.format(
                "https://api.github.com/repos/%s/issues?state=open&creator=%s",
                repo, username
        );

        HttpEntity<Void> e = new HttpEntity<>(githubHeaders);
        return restTemplate.exchange(url, HttpMethod.GET, e, JsonNode.class).getBody();
    }
}
