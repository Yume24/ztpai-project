package com.rendezvous.room.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import com.rendezvous.room.domain.RoomMember;
import com.rendezvous.room.domain.RoomRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record MemberResponse(
        @Schema(requiredMode = REQUIRED) UUID userId,
        @Schema(requiredMode = REQUIRED) String displayName,
        @Schema(requiredMode = REQUIRED) RoomRole role,
        @Schema(requiredMode = REQUIRED) Instant joinedAt) {

    public static MemberResponse from(RoomMember member) {
        return new MemberResponse(member.getUser().getId(), member.getUser().getDisplayName(), member.getRole(),
                member.getJoinedAt());
    }
}
