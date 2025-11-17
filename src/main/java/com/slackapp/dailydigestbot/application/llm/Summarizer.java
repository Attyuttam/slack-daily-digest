package com.slackapp.dailydigestbot.application.llm;

import java.util.List;

public interface Summarizer {
    public String summarizeMessages(String title, List<String> messages);
}
