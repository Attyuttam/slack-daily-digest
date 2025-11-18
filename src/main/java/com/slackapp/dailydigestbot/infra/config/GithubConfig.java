package com.slackapp.dailydigestbot.infra.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

@Configuration
public class GithubConfig {
    @Value("${github.token}")
    private String githubToken;

    @Bean
    @Qualifier("githubHeaders")
    public HttpHeaders githubHeaders(){
        HttpHeaders h = new HttpHeaders();
        h.set("Authorization", "Bearer " + githubToken);
        h.set("Accept", "application/vnd.github+json");
        return h;
    }
}
