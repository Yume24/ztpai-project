package com.rendezvous.room.domain;

public enum RoomRole {
    OWNER, ADMIN, MEMBER;

    public boolean isGuardian() {
        return this != MEMBER;
    }
}
