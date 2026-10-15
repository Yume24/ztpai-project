package com.rendezvous.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Request validation failed."),
    INVALID_TIME_WINDOW(HttpStatus.BAD_REQUEST, "Room time window is invalid."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication is required."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid email or password."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Refresh token is invalid or expired."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Access denied."),
    USER_BLOCKED(HttpStatus.FORBIDDEN, "User account is blocked."),
    MEMBER_REMOVED(HttpStatus.FORBIDDEN, "Member was removed from the room."),
    ROOM_ACTION_FORBIDDEN(HttpStatus.FORBIDDEN, "Action is not allowed in this room."),
    ADMIN_ACTION_FORBIDDEN(HttpStatus.FORBIDDEN, "Action is not allowed for this user."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found."),
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "Room not found."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "Member not found."),
    EMAIL_TAKEN(HttpStatus.CONFLICT, "Email is already registered."),
    ROOM_ENDED(HttpStatus.CONFLICT, "Room has already ended."),
    ROOM_FULL(HttpStatus.CONFLICT, "Room is full."),
    ROOM_VERSION_CONFLICT(HttpStatus.CONFLICT, "Room was modified concurrently, retry.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
