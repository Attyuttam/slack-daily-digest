package com.slackapp.dailydigestbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DailydigestbotApplication {

	public static void main(String[] args) {
		SpringApplication.run(DailydigestbotApplication.class, args);
	}

}
