package com.rendezvous.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rendezvous.support.IntegrationTest;
import com.rendezvous.user.repository.PushTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class MeIntegrationTest extends IntegrationTest {

    @Autowired
    private PushTokenRepository pushTokens;

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void profileCanBeReadAndRenamed() throws Exception {
        String token = api.token("me-profile@x.pl");

        mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me-profile@x.pl"));

        mvc.perform(patch("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Ola\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Ola"));

        mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.displayName").value("Ola"));
    }

    @Test
    void renameValidatesLength() throws Exception {
        String token = api.token("me-validate@x.pl");

        mvc.perform(patch("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"A\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void renameValidatesTrimmedLength() throws Exception {
        String token = api.token("me-trim@x.pl");

        mvc.perform(patch("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"  A  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void registrationValidatesTrimmedDisplayName() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"me-trim-reg@x.pl\",\"password\":\"password123\","
                                + "\"displayName\":\"  A  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void pushTokenRegistrationIsIdempotent() throws Exception {
        String token = api.token("me-push@x.pl");
        String pushToken = "ExponentPushToken[abc123-me-push]";

        for (int i = 0; i < 2; i++) {
            mvc.perform(put("/api/me/push-tokens/{token}", pushToken)
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isNoContent());
        }
        assertThat(pushTokens.findByToken(pushToken)).isPresent();

        mvc.perform(delete("/api/me/push-tokens/{token}", pushToken)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());
        assertThat(pushTokens.findByToken(pushToken)).isEmpty();
    }
}
