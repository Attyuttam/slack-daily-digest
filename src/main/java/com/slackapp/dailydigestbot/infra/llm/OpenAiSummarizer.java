//package com.slackapp.dailydigestbot.infra.llm;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.slackapp.dailydigestbot.application.llm.Summarizer;
//import lombok.RequiredArgsConstructor;
//import okhttp3.*;
//import org.jetbrains.annotations.NotNull;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import java.io.IOException;
//import java.util.*;
//
//@Service
//@RequiredArgsConstructor
//public class OpenAiSummarizer implements Summarizer {
//    @Value("${OPENAI_API_KEY}")
//    private String openaiKey;
//    @Value("${OPENAI_MODEL:gpt-4o-mini}")
//    private String openaiModel;
//
//    private final OkHttpClient client;
//    private final ObjectMapper mapper;
//
//    @Override
//    public String summarizeMessages(String title, List<String> messages) {
//        // Limit input size: send only last ~30 messages
//        List<String> truncated = messages.size() > 30 ? messages.subList(messages.size() - 30, messages.size()) : messages;
//        String prompt = buildPrompt(title, truncated);
//
//        Request req = getRequest(prompt);
//
//        try (Response resp = client.newCall(req).execute()) {
//            if (!resp.isSuccessful()) throw new IOException("OpenAI error: " + resp.code() + " " + resp.message());
//            Map<?, ?> map = mapper.readValue(resp.body().string(), Map.class);
//            List<?> choices = (List<?>) map.get("choices");
//            if (choices == null || choices.isEmpty()) return "No summary available.";
//            Map<?,?> choice = (Map<?,?>) choices.get(0);
//            Map<?,?> message = (Map<?,?>) choice.get("message");
//            return message.get("content").toString();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    @NotNull
//    private Request getRequest(String prompt) {
//        Map<String, Object> json = new HashMap<>();
//        json.put("model", openaiModel);
//        // using the chat completion format
//        List<Map<String, String>> msgs = new ArrayList<>();
//        msgs.add(Map.of("role", "system", "content", "You are an assistant that converts Slack channel messages into a short team digest with action items."));
//        msgs.add(Map.of("role", "user", "content", prompt));
//        json.put("messages", msgs);
//        json.put("max_tokens", 400);
//        RequestBody body = null;
//        body = getRequestBody(json);
//        return new Request.Builder()
//                .url("https://api.openai.com/v1/chat/completions")
//                .post(body)
//                .addHeader("Authorization", "Bearer " + openaiKey)
//                .addHeader("Content-Type", "application/json")
//                .build();
//    }
//
//    @NotNull
//    private RequestBody getRequestBody(Map<String, Object> json) {
//        RequestBody body;
//        try {
//            body = RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsString(json));
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }
//        return body;
//    }
//
//    private String buildPrompt(String title, List<String> messages) {
//        StringBuilder b = new StringBuilder();
//        b.append("Create a short digest titled: ").append(title).append("\n\n");
//        b.append("Messages:\n");
//        for (String m : messages) {
//            b.append("- ").append(m).append("\n");
//        }
//        b.append("\nInstructions: Produce a 3-section digest: 1) Important updates (bulleted), 2) Action items (who/what), 3) Questions or blockers. Keep it concise (max 6 bullets).");
//        return b.toString();
//    }
//}
