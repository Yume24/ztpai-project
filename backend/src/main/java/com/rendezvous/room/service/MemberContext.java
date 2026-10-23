package com.rendezvous.room.service;

import com.rendezvous.room.domain.Room;
import com.rendezvous.room.domain.RoomMember;
import com.rendezvous.room.domain.RoomRole;

public record MemberContext(Room room, RoomMember member) {

    public RoomRole role() {
        return member.getRole();
    }
}
