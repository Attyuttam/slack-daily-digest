package com.slackapp.dailydigestbot.application.llm;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SlackSummarizer{
    private final SummarizerClient summarizerClient;
    public String summarizeMessages(String title, List<String> messages) {
        // Limit number of messages to avoid large payloads
        List<String> truncated =
                messages.size() > 30
                        ? messages.subList(messages.size() - 30, messages.size())
                        : messages;

        String prompt = buildPrompt(title, truncated);
        return summarizerClient.summarize(prompt);
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
