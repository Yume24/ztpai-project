package com.rendezvous.auth.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rendezvous.common.config.ClockConfig;
import com.rendezvous.common.security.CurrentUserId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = SecurityConfigTest.TestController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, ClockConfig.class, SecurityConfigTest.TestController.class})
class SecurityConfigTest {

    private static final UUID ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    MockMvc mvc;

    @Test
    void requestWithoutTokenIs401ProblemDetail() throws Exception {
        mvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void validTokenResolvesCurrentUserId() throws Exception {
        mvc.perform(get("/api/me").with(jwt().jwt(j -> j.subject(ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(content().string(ID.toString()));
    }

    @Test
    void adminPathRequiresAdminRole() throws Exception {
        mvc.perform(get("/api/admin/x")
                        .with(jwt().jwt(j -> j.subject(ID.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @RestController
    static class TestController {

        @GetMapping("/api/me")
        String me(@CurrentUserId UUID id) {
            return id.toString();
        }

        @GetMapping("/api/admin/x")
        String admin() {
            return "ok";
        }
    }
}
