package com.rendezvous.user.controller;

import com.rendezvous.common.security.CurrentUserId;
import com.rendezvous.user.dto.UpdateProfileRequest;
import com.rendezvous.user.dto.UserResponse;
import com.rendezvous.user.service.PushTokenService;
import com.rendezvous.user.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserService userService;
    private final PushTokenService pushTokenService;

    public MeController(UserService userService, PushTokenService pushTokenService) {
        this.userService = userService;
        this.pushTokenService = pushTokenService;
    }

    @GetMapping
    public UserResponse profile(@CurrentUserId UUID userId) {
        return userService.profile(userId);
    }

    @PatchMapping
    public UserResponse rename(@CurrentUserId UUID userId, @Valid @RequestBody UpdateProfileRequest request) {
        return userService.rename(userId, request.displayName());
    }

    @PutMapping("/push-tokens/{token}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registerPushToken(@CurrentUserId UUID userId, @PathVariable String token) {
        pushTokenService.register(userId, token);
    }

    @DeleteMapping("/push-tokens/{token}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removePushToken(@CurrentUserId UUID userId, @PathVariable String token) {
        pushTokenService.remove(userId, token);
    }
}
