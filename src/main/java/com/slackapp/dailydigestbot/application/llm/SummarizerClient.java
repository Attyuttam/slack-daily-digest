package com.slackapp.dailydigestbot.application.llm;

public interface SummarizerClient {
    public String summarize(String prompt);
}
