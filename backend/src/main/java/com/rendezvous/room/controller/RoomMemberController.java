package com.rendezvous.room.controller;

import com.rendezvous.common.security.CurrentUserId;
import com.rendezvous.room.dto.ChangeRoleRequest;
import com.rendezvous.room.dto.MemberResponse;
import com.rendezvous.room.service.RoomMembershipService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoomMemberController {

    private final RoomMembershipService membership;

    public RoomMemberController(RoomMembershipService membership) {
        this.membership = membership;
    }

    @GetMapping("/api/rooms/{id}/members")
    public List<MemberResponse> members(@CurrentUserId UUID userId, @PathVariable UUID id) {
        return membership.members(id, userId);
    }

    @PatchMapping("/api/rooms/{id}/members/{userId}")
    public MemberResponse changeRole(@CurrentUserId UUID actorId, @PathVariable UUID id,
                                     @PathVariable UUID userId, @Valid @RequestBody ChangeRoleRequest request) {
        return membership.changeRole(id, actorId, userId, request.role());
    }

    @DeleteMapping("/api/rooms/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@CurrentUserId UUID actorId, @PathVariable UUID id, @PathVariable UUID userId) {
        membership.remove(id, actorId, userId);
    }
}
