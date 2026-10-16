package com.rendezvous.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rendezvous.auth.dto.AuthResponse;
import com.rendezvous.auth.domain.RefreshToken;
import com.rendezvous.auth.repository.RefreshTokenRepository;
import com.rendezvous.auth.service.TokenService;
import com.rendezvous.support.IntegrationTest;
import com.rendezvous.user.repository.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.json.JsonMapper;

class AuthFlowIntegrationTest extends IntegrationTest {

    @Autowired
    JsonMapper json;

    @Autowired
    UserRepository users;

    @Autowired
    RefreshTokenRepository refreshTokens;

    @Autowired
    TokenService tokenService;

    @Test
    void registerLoginRefreshAndOldRefreshTokenStopsWorking() throws Exception {
        post("/api/auth/register", "{\"email\":\"flow@x.pl\",\"password\":\"password123\",\"displayName\":\"Flow\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("USER"));

        AuthResponse login = read(post("/api/auth/login", "{\"email\":\"flow@x.pl\",\"password\":\"password123\"}")
                .andExpect(status().isOk()));

        AuthResponse refreshed = read(post("/api/auth/refresh", refreshBody(login.refreshToken()))
                .andExpect(status().isOk()));

        post("/api/auth/refresh", refreshBody(login.refreshToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
        post("/api/auth/refresh", refreshBody(refreshed.refreshToken())).andExpect(status().isOk());
    }

    @Test
    void loginIsCaseInsensitiveAndDuplicateEmailDiffersOnlyByCase() throws Exception {
        post("/api/auth/register", "{\"email\":\" Ala@X.pl \",\"password\":\"password123\",\"displayName\":\"Ala\"}")
                .andExpect(status().isCreated());

        post("/api/auth/login", "{\"email\":\"ala@x.pl\",\"password\":\"password123\"}")
                .andExpect(status().isOk());
        post("/api/auth/register", "{\"email\":\"ALA@X.PL\",\"password\":\"password123\",\"displayName\":\"Ala\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
    }

    @Test
    void invalidRegistrationReturnsFieldErrors() throws Exception {
        post("/api/auth/register", "{\"email\":\"short@x.pl\",\"password\":\"short\",\"displayName\":\"Short\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'password')]").exists());
    }

    @Test
    void passwordOverBcryptByteLimitIsAValidationError() throws Exception {
        String twoBytesEach = "ż";

        post("/api/auth/register", registerBody("bytes-long@x.pl", twoBytesEach.repeat(37)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[?(@.field == 'password')]").exists());
        post("/api/auth/register", registerBody("bytes-max@x.pl", twoBytesEach.repeat(36)))
                .andExpect(status().isCreated());
    }

    @Test
    void logoutInvalidatesRefreshToken() throws Exception {
        AuthResponse registered = api.register("logout@x.pl");

        post("/api/auth/logout", refreshBody(registered.refreshToken()))
                .andExpect(status().isNoContent());
        post("/api/auth/refresh", refreshBody(registered.refreshToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void adminAccountIsBootstrappedFromProperties() throws Exception {
        post("/api/auth/login", "{\"email\":\"admin@rendezvous.test\",\"password\":\"admin-pass-123\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    void expiredRefreshTokenIsRejectedAndDeleted() throws Exception {
        api.register("expired@x.pl");
        String hash = tokenService.hash("expired-raw-token");
        Instant past = Instant.parse("2020-01-01T00:00:00Z");
        refreshTokens.save(new RefreshToken(users.findByEmail("expired@x.pl").orElseThrow(), hash, past, past));

        post("/api/auth/refresh", refreshBody("expired-raw-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

        assertThat(refreshTokens.findByTokenHash(hash)).isEmpty();
    }

    private ResultActions post(String url, String body) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static String registerBody(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"displayName\":\"Bytes\"}";
    }

    private static String refreshBody(String refreshToken) {
        return "{\"refreshToken\":\"" + refreshToken + "\"}";
    }

    private AuthResponse read(ResultActions result) throws Exception {
        return json.readValue(result.andReturn().getResponse().getContentAsString(), AuthResponse.class);
    }
}
