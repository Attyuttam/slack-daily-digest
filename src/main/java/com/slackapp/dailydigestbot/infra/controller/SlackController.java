package com.slackapp.dailydigestbot.infra.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slackapp.dailydigestbot.application.DigestService;
import com.slackapp.dailydigestbot.application.SlackSignatureVerifier;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/slack")
public class SlackController {

    private final SlackSignatureVerifier verifier;
    private final DigestService digestService;
    private final ObjectMapper mapper;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Value("${slack-bot.signing.secret}")
    private String signingSecret;

    @GetMapping("/test-digest")
    public String testDigest(){
        digestService.testSendMessage();
        return "sent";
    }
    @PostMapping("/digest")
    public ResponseEntity<String> handleDigestSlash(HttpServletRequest request,
                                                    @RequestHeader("X-Slack-Request-Timestamp") String timestamp,
                                                    @RequestHeader("X-Slack-Signature") String sig) throws Exception {
        String body;
        try (BufferedReader reader = request.getReader()) {
            body = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }

        if (!verifier.verify(signingSecret, timestamp, body, sig)) {
            return ResponseEntity.status(401).body("invalid signature");
        }

        // Parse form body (application/x-www-form-urlencoded)
        Map<String, String> params = java.util.Arrays.stream(body.split("&"))
            .map(s -> s.split("=", 2))
            .collect(Collectors.toMap(a -> urlDecode(a[0]), a -> urlDecode(a.length > 1 ? a[1] : "")));

        String channelId = params.getOrDefault("channel_id", null);
        String userId = params.getOrDefault("user_id", null);

        // immediate response to Slack (ack)
        // we send ephemeral response acknowledging receipt:
        // (Slack expects a 200 within 3s)
        new Thread(() -> {
            // asynchronously create and send digest (so we can reply quickly)
            try {
                digestService.generateAndSendDigestToChannel(channelId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        return ResponseEntity.ok("Preparing your digest — will post to channel shortly.");
    }
    private static String urlDecode(String s) {
        try {
            return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (Exception e) { return s; }
    }
}
