package com.slackapp.dailydigestbot.infra.scheduler;

import com.slackapp.dailydigestbot.application.DigestService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DigestScheduler {

    private final DigestService digestService;

    @Value("${slack-bot.default.digest-channel}")
    private String slackChannel;

    @Scheduled(cron = "${slack-bot.default.digest-cron}")
    public void scheduledDigestAll() {
        // This demo posts to one default channel. Extend to fetch team configs from DB.
        try {
            digestService.generateAndSendDigestToChannel(slackChannel);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
