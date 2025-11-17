package com.slackapp.dailydigestbot.application;

import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.conversations.ConversationsHistoryResponse;
import com.slack.api.methods.request.conversations.ConversationsHistoryRequest;
import com.slack.api.model.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SlackClientService {

    @Value("${slack-bot.token}")
    private String botToken;

    private final Slack slack;

    public void postMessage(String channelId, String text) throws IOException, SlackApiException {
        ChatPostMessageResponse res = slack.methods(botToken).chatPostMessage(r -> r
                .channel(channelId)
                .text(text)
                .mrkdwn(true)
        );
        if (!res.isOk()) {
            throw new RuntimeException("Slack post failed: " + res.getError());
        }
    }

    public List<Message> fetchChannelMessages(String channelId, int limit) throws IOException, SlackApiException {
        ConversationsHistoryResponse res = slack.methods(botToken).conversationsHistory(ConversationsHistoryRequest.builder()
                .token(botToken)
                .channel(channelId)
                .limit(limit)
                .build()
        );
        if (!res.isOk()) {
            throw new RuntimeException("Slack history failed: " + res.getError());
        }
        return res.getMessages();
    }
}
