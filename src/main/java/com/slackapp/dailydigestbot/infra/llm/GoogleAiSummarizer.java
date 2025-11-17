package com.slackapp.dailydigestbot.infra.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slackapp.dailydigestbot.application.llm.Summarizer;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class GoogleAiSummarizer implements Summarizer {

    @Value("${llm.google-api-key}")
    private String apiKey;

    @Value("${llm.google-model:gemini-1.5-flash}")
    private String model;

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String summarizeMessages(String title, List<String> messages) {

        // Limit number of messages to avoid large payloads
        List<String> truncated =
                messages.size() > 30
                        ? messages.subList(messages.size() - 30, messages.size())
                        : messages;

        String prompt = buildPrompt(title, truncated);

        // Gemini expects request format:
        //
        // {
        //   "contents": [
        //     { "parts": [{ "text": "..." }] }
        //   ]
        // }
        //
        Map<String, Object> root = new HashMap<>();
        List<Object> contents = new ArrayList<>();
        Map<String, Object> part = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(part));
        contents.add(content);
        root.put("contents", contents);

        RequestBody body = null;
        try {
            body = RequestBody.create(
                    mapper.writeValueAsString(root),
                    MediaType.parse("application/json")
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        Request request = new Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/" 
                        + model + ":generateContent?key=" + apiKey)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {

            if (!response.isSuccessful()) {
                throw new IOException("Gemini API error: " + response.code() + " " + response.message());
            }

            Map<?, ?> responseMap = mapper.readValue(response.body().string(), Map.class);

            // Structure:
            // {
            //   "candidates": [
            //     {
            //       "content": {
            //          "parts": [{ "text": "SUMMARY TEXT" }]
            //       }
            //     }
            //   ]
            // }
            //
            List<?> candidates = (List<?>) responseMap.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                return "No summary produced.";
            }

            Map<?, ?> candidate = (Map<?, ?>) candidates.get(0);
            Map<?, ?> contentObj = (Map<?, ?>) candidate.get("content");

            List<?> parts = (List<?>) contentObj.get("parts");
            if (parts == null || parts.isEmpty()) {
                return "No summary produced.";
            }

            Map<?, ?> firstPart = (Map<?, ?>) parts.get(0);
            return firstPart.get("text").toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String buildPrompt(String title, List<String> messages) {
        StringBuilder b = new StringBuilder();
        b.append("Create a concise, high-signal Slack channel digest.\n");
        b.append("Title: ").append(title).append("\n\n");
        b.append("Messages:\n");
        for (String m : messages) {
            b.append("- ").append(m).append("\n");
        }

        b.append("""
                
                ---
                Produce the digest in 3 short sections:
                1) **Key Updates** — major points only, in bullets.
                2) **Action Items** — specify WHO and WHAT clearly.
                3) **Blockers / Questions** — only if relevant.
                
                Rules:
                - Max 6 bullets total
                - Avoid fluff
                - Convert vague statements into concrete items
                
                Begin:
                """);

        return b.toString();
    }
}
