package com.rendezvous.common.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rendezvous.support.IntegrationTest;
import org.junit.jupiter.api.Test;

class OpenApiIntegrationTest extends IntegrationTest {

    @Test
    void openApiDocsArePublicAndDescribeBearerAuth() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Rendezvous API"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/rooms']").exists())
                .andExpect(jsonPath("$.paths['/api/me/locations']").exists())
                .andExpect(jsonPath("$.paths['/api/admin/users']").exists());
    }
}
