package com.slackapp.dailydigestbot.application.digest;

import com.slack.api.model.Message;
import com.slackapp.dailydigestbot.application.SlackClientService;
import com.slackapp.dailydigestbot.application.llm.SlackSummarizer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SlackDigestService {

    private final SlackClientService slackClient;
    private final SlackSummarizer slackSummarizer;

    @Value("${slack-bot.default.digest-channel}")
    private String defaultChannel;

    @Value("${slack-bot.default.digest-cron:0 0 9 * * ?}")
    private String cron;

    public void testSendMessage(){
        try {
            slackClient.postMessage(defaultChannel, "This is a test message from local");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void generateAndSendDigestToChannel(String channelId) throws Exception {
        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("Channel id not specified");
        }

        // 1) Fetch messages (last 50)
        List<Message> messages = slackClient.fetchChannelMessages(channelId, 50);

        // 2) Transform messages to simple strings & filter bots
        List<String> textMessages = messages.stream()
                .filter(m -> m.getSubtype() == null) // exclude bot-subtypes; adjust as needed
                .map(m -> {
                    String user = m.getUser() != null ? "<@" + m.getUser() + ">" : "someone";
                    String ts = m.getTs();
                    String text = m.getText() != null ? m.getText().replaceAll("\\n", " ") : "";
                    return user + ": " + text;
                })
                .collect(Collectors.toList());

        if (textMessages.isEmpty()) {
            slackClient.postMessage(channelId, "No recent messages to summarize.");
            return;
        }

        // 3) Summarize with LLM
        String title = "Digest for channel " + channelId;
        String summary = slackSummarizer.summarizeMessages(title, textMessages);

        // 4) Post summary
        String finalMessage = "🔔 *Daily Digest*\n\n" + summary;
        slackClient.postMessage(channelId, finalMessage);
    }
}
