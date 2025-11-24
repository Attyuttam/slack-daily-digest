package com.slackapp.dailydigestbot.infra.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.slackapp.dailydigestbot.application.digest.GithubDigestService;
import com.slackapp.dailydigestbot.application.SlackSignatureVerifier;
import com.slackapp.dailydigestbot.application.llm.GithubSummarizer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
@Slf4j
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
            @RequestHeader("X-Slack-Signature") String signature) throws Exception {

        // 1. Verify Slack signature
        String body;
        try (BufferedReader reader = request.getReader()) {
            body = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }

        if (!verifier.verify(signingSecret, timestamp, body, signature)) {
            return ResponseEntity.status(401).body("invalid signature");
        }
        // 2. Parse Slack form payload
        // Parse form body (application/x-www-form-urlencoded)
        Map<String, String> params = java.util.Arrays.stream(body.split("&"))
                .map(s -> s.split("=", 2))
                .collect(Collectors.toMap(a -> urlDecode(a[0]), a -> urlDecode(a.length > 1 ? a[1] : "")));

        String text = params.getOrDefault("text", null); // text typed after the slash command
        String channelId = params.getOrDefault("channel_id", null);
        String slackUserName = params.getOrDefault("user_name", null);

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
                githubDigestService.generateAndSendDigestToChannel(githubUser, repo, channelId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // Slack requires a response within 3 seconds
        return ResponseEntity.ok("Generating GitHub digest...");
    }

    private static String urlDecode(String s) {
        try {
            return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }
}
