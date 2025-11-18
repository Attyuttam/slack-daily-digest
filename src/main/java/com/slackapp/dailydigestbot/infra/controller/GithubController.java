package com.slackapp.dailydigestbot.infra.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.slackapp.dailydigestbot.application.digest.GithubDigestService;
import com.slackapp.dailydigestbot.application.SlackClientService;
import com.slackapp.dailydigestbot.application.SlackSignatureVerifier;
import com.slackapp.dailydigestbot.application.llm.GithubSummarizer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/github")
public class GithubController {
    @Value("${slack-bot.signing.secret}")
    private String signingSecret;
    private final SlackSignatureVerifier verifier;
    private final GithubDigestService githubDigestService;

    @PostMapping("/digest")
    public ResponseEntity<String> handleGithubDigestSlash(
            HttpServletRequest request,
            @RequestHeader("X-Slack-Request-Timestamp") String timestamp,
            @RequestHeader("X-Slack-Signature") String signature
    ) throws Exception {

        // 1. Verify Slack signature
        String body;
        try (BufferedReader reader = request.getReader()) {
            body = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }

        if (!verifier.verify(signingSecret, timestamp, body, signature)) {
            return ResponseEntity.status(401).body("invalid signature");
        }

        // 2. Parse Slack form payload
        String text = request.getParameter("text");     // text typed after the slash command
        String channelId = request.getParameter("channel_id");
        String slackUserName = request.getParameter("user_name");

        // Example: "john org/repo"
        String[] parts = text.split("\\s+");
        String githubUser = parts.length > 0 ? parts[0] : slackUserName; // fallback
        String repo = parts.length > 1 ? parts[1] : "default/repo";

        // immediate response to Slack (ack)
        // we send ephemeral response acknowledging receipt:
        // (Slack expects a 200 within 3s)
        new Thread(() -> {
            // asynchronously create and send digest (so we can reply quickly)
            try {
                githubDigestService.generateAndSendDigestToChannel(githubUser,repo,channelId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // Slack requires a response within 3 seconds
        return ResponseEntity.ok("Generating GitHub digest...");
    }

}
