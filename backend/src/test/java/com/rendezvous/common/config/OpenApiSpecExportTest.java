package com.rendezvous.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rendezvous.support.IntegrationTest;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class OpenApiSpecExportTest extends IntegrationTest {

    @Test
    void writesOpenApiSpecForMobileTypes() throws Exception {
        String json = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Path out = Path.of("target", "openapi", "openapi.json");
        Files.createDirectories(out.getParent());
        Files.writeString(out, json);
        assertThat(Files.readString(out)).contains("\"/api/rooms\"", "\"bearerAuth\"");
    }
}
