package com.rendezvous.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rendezvous.user.service.PushTokenService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PushNotificationServiceTest {

    private final PushTokenService pushTokens = mock(PushTokenService.class);
    private final ExpoPushClient client = mock(ExpoPushClient.class);
    private final PushNotificationService service = new PushNotificationService(pushTokens, client);

    private final List<UUID> users = List.of(UUID.randomUUID());
    private final Map<String, String> data = Map.of("type", "ALERT");

    @Test
    @SuppressWarnings("unchecked")
    void invalidTokensAreForgotten() {
        when(pushTokens.tokensOf(users)).thenReturn(List.of("a", "b"));
        when(client.send(any())).thenReturn(List.of("b"));

        service.notify(users, "Treść", data);

        ArgumentCaptor<List<PushMessage>> sent = ArgumentCaptor.forClass(List.class);
        verify(client).send(sent.capture());
        assertThat(sent.getValue()).extracting(PushMessage::to).containsExactly("a", "b");
        assertThat(sent.getValue()).allSatisfy(m -> {
            assertThat(m.title()).isEqualTo("Rendezvous");
            assertThat(m.body()).isEqualTo("Treść");
            assertThat(m.data()).isEqualTo(data);
        });
        verify(pushTokens).forget(List.of("b"));
    }

    @Test
    void noTokensMeansNoHttpCall() {
        when(pushTokens.tokensOf(users)).thenReturn(List.of());

        service.notify(users, "Treść", data);

        verify(client, never()).send(any());
        verify(pushTokens, never()).forget(any());
    }
}
