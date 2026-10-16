package com.rendezvous.user.service;

import com.rendezvous.common.error.ApiException;
import com.rendezvous.common.error.ErrorCode;
import com.rendezvous.user.domain.User;
import com.rendezvous.user.dto.UserResponse;
import com.rendezvous.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public UserResponse profile(UUID userId) {
        return UserResponse.from(find(userId));
    }

    @Transactional
    public UserResponse rename(UUID userId, String displayName) {
        User user = find(userId);
        user.rename(displayName);
        return UserResponse.from(user);
    }

    private User find(UUID userId) {
        return users.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }
}
