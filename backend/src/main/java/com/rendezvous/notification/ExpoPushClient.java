package com.rendezvous.notification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ExpoPushClient {

    private static final Logger log = LoggerFactory.getLogger(ExpoPushClient.class);
    private static final int BATCH_SIZE = 100;
    private static final String DEVICE_NOT_REGISTERED = "DeviceNotRegistered";

    private final RestClient client;

    public ExpoPushClient(RestClient.Builder builder, ExpoPushProperties props) {
        RestClient.Builder configured = builder
                .baseUrl(props.url())
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE);
        if (props.accessToken() != null && !props.accessToken().isBlank()) {
            configured.defaultHeader("Authorization", "Bearer " + props.accessToken());
        }
        this.client = configured.build();
    }

    public List<String> send(List<PushMessage> messages) {
        List<String> invalid = new ArrayList<>();
        for (int from = 0; from < messages.size(); from += BATCH_SIZE) {
            List<PushMessage> batch = messages.subList(from, Math.min(from + BATCH_SIZE, messages.size()));
            try {
                collectInvalid(batch, sendBatch(batch), invalid);
            } catch (RuntimeException e) {
                log.warn("Expo push batch of {} messages failed: {}", batch.size(), e.toString());
            }
        }
        return invalid;
    }

    private Response sendBatch(List<PushMessage> batch) {
        List<Outgoing> body = batch.stream()
                .map(m -> new Outgoing(m.to(), m.title(), m.body(), m.data(), "default"))
                .toList();
        return client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Response.class);
    }

    private static void collectInvalid(List<PushMessage> batch, Response response, List<String> invalid) {
        if (response == null || response.data() == null) {
            return;
        }
        List<Ticket> tickets = response.data();
        for (int i = 0; i < Math.min(tickets.size(), batch.size()); i++) {
            Ticket ticket = tickets.get(i);
            if ("error".equals(ticket.status()) && ticket.details() != null
                    && DEVICE_NOT_REGISTERED.equals(ticket.details().get("error"))) {
                invalid.add(batch.get(i).to());
            }
        }
    }

    private record Outgoing(String to, String title, String body, Map<String, String> data, String sound) {
    }

    private record Response(List<Ticket> data) {
    }

    private record Ticket(String status, Map<String, Object> details) {
    }
}
