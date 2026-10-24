package com.rendezvous.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.ServerSocket;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.client.autoconfigure.HttpClientAutoConfiguration;
import org.springframework.boot.http.client.autoconfigure.imperative.ImperativeHttpClientAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ExpoPushClientTest {

    private static final String URL = "https://expo.test/push";

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    private ExpoPushClient client(String token) {
        return new ExpoPushClient(builder, new ExpoPushProperties(URL, token));
    }

    private static PushMessage message(String to) {
        return new PushMessage(to, "Rendezvous", "Treść", Map.of("type", "ALERT"));
    }

    private static List<PushMessage> messages(int count) {
        return IntStream.range(0, count).mapToObj(i -> message("t" + i)).toList();
    }

    @Test
    void sendsJsonArrayWithBearerToken() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer secret"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].to").value("t0"))
                .andExpect(jsonPath("$[0].sound").value("default"))
                .andExpect(jsonPath("$[1].data.type").value("ALERT"))
                .andRespond(withSuccess("{\"data\":[{\"status\":\"ok\"},{\"status\":\"ok\"}]}",
                        MediaType.APPLICATION_JSON));

        assertThat(client("secret").send(messages(2))).isEmpty();

        server.verify();
    }

    @Test
    void omitsAuthorizationWhenTokenBlank() {
        server.expect(requestTo(URL))
                .andExpect(request -> assertThat(request.getHeaders().containsHeader("Authorization")).isFalse())
                .andRespond(withSuccess("{\"data\":[{\"status\":\"ok\"}]}", MediaType.APPLICATION_JSON));

        client("").send(messages(1));

        server.verify();
    }

    @Test
    void splitsIntoBatchesOfHundred() {
        server.expect(requestTo(URL)).andExpect(jsonPath("$.length()").value(100))
                .andRespond(withSuccess("{\"data\":[]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(URL)).andExpect(jsonPath("$.length()").value(50))
                .andRespond(withSuccess("{\"data\":[]}", MediaType.APPLICATION_JSON));

        client("secret").send(messages(150));

        server.verify();
    }

    @Test
    void returnsDeviceNotRegisteredTokens() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess("""
                        {"data":[{"status":"ok"},
                                 {"status":"error","details":{"error":"DeviceNotRegistered"}}]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(client("secret").send(List.of(message("token1"), message("token2"))))
                .containsExactly("token2");
    }

    @Test
    void httpErrorDoesNotThrow() {
        server.expect(requestTo(URL)).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withServerError());

        assertThat(client("secret").send(messages(1))).isEmpty();
    }

    @Test
    void readTimeoutFromHttpClientPropertiesEndsRequestToSilentServer() throws Exception {
        try (ServerSocket silent = new ServerSocket(0)) {
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(HttpClientAutoConfiguration.class,
                            ImperativeHttpClientAutoConfiguration.class, RestClientAutoConfiguration.class))
                    .withPropertyValues("spring.http.clients.read-timeout=200ms")
                    .run(context -> {
                        ExpoPushClient client = new ExpoPushClient(context.getBean(RestClient.Builder.class),
                                new ExpoPushProperties("http://localhost:" + silent.getLocalPort() + "/push", ""));

                        assertThat(assertTimeoutPreemptively(Duration.ofSeconds(5), () -> client.send(messages(1))))
                                .isEmpty();
                    });
        }
    }
}
