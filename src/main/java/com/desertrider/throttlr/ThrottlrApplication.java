package com.desertrider.throttlr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.desertrider.throttlr.config.JwtProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class ThrottlrApplication {

	public static void main(String[] args) {
		SpringApplication.run(ThrottlrApplication.class, args);
	}

}
