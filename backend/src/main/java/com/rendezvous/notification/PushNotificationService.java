package com.rendezvous.notification;

import com.rendezvous.user.service.PushTokenService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PushNotificationService {

    private final PushTokenService pushTokens;
    private final ExpoPushClient client;

    public PushNotificationService(PushTokenService pushTokens, ExpoPushClient client) {
        this.pushTokens = pushTokens;
        this.client = client;
    }

    public void notify(Collection<UUID> userIds, String body, Map<String, String> data) {
        List<String> tokens = pushTokens.tokensOf(userIds);
        if (tokens.isEmpty()) {
            return;
        }
        List<PushMessage> messages = tokens.stream()
                .map(token -> new PushMessage(token, PushTexts.TITLE, body, data))
                .toList();
        List<String> invalid = client.send(messages);
        if (!invalid.isEmpty()) {
            pushTokens.forget(invalid);
        }
    }
}
