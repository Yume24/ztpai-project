package com.rendezvous.common.error;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestController.class)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.TestController.class})
@WithMockUser
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void apiExceptionBecomesProblemDetailWithCode() throws Exception {
        mvc.perform(get("/test/room-full"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("ROOM_FULL"))
                .andExpect(jsonPath("$.detail").value(ErrorCode.ROOM_FULL.defaultMessage()));
    }

    @Test
    void beanValidationErrorsListFields() throws Exception {
        mvc.perform(post("/test/validated").contentType(APPLICATION_JSON).content("{\"name\":\"\"}").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    void malformedJsonIsValidationFailed() throws Exception {
        mvc.perform(post("/test/validated").contentType(APPLICATION_JSON).content("{").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void optimisticLockFailureIsVersionConflict() throws Exception {
        mvc.perform(get("/test/optimistic-lock"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROOM_VERSION_CONFLICT"));
    }

    @Test
    void unexpectedExceptionIs500WithoutStackTrace() throws Exception {
        mvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void accessDeniedIs403() throws Exception {
        mvc.perform(get("/test/denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void authenticationExceptionIs401() throws Exception {
        mvc.perform(get("/test/unauthenticated"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void invalidEnumRequestParamIsValidationFailed() throws Exception {
        mvc.perform(get("/test/enum").param("kind", "FOO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    enum Kind { A, B }

    record Body(@NotBlank String name) {
    }

    @RestController
    static class TestController {

        @GetMapping("/test/room-full")
        void roomFull() {
            throw new ApiException(ErrorCode.ROOM_FULL);
        }

        @PostMapping("/test/validated")
        void validated(@Valid @RequestBody Body body) {
        }

        @GetMapping("/test/optimistic-lock")
        void optimisticLock() {
            throw new ObjectOptimisticLockingFailureException("Room", 1);
        }

        @GetMapping("/test/denied")
        void denied() {
            throw new AccessDeniedException("no");
        }

        @GetMapping("/test/unauthenticated")
        void unauthenticated() {
            throw new BadCredentialsException("no");
        }

        @GetMapping("/test/enum")
        void enumParam(@RequestParam Kind kind) {
        }

        @GetMapping("/test/boom")
        void boom() {
            throw new IllegalStateException("boom");
        }
    }
}
