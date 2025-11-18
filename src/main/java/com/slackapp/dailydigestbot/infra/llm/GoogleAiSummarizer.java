package com.slackapp.dailydigestbot.infra.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slackapp.dailydigestbot.application.llm.SummarizerClient;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class GoogleAiSummarizer implements SummarizerClient {

    @Value("${llm.google-api-key}")
    private String apiKey;

    @Value("${llm.google-model:gemini-1.5-flash}")
    private String model;

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String summarize(String prompt) {
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
}
