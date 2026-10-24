package com.rendezvous.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.push")
public record ExpoPushProperties(String url, String accessToken) {
}
