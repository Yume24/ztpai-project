package com.rendezvous.room.controller;

import com.rendezvous.common.security.CurrentUserId;
import com.rendezvous.common.web.PageResponse;
import com.rendezvous.room.domain.RoomStatus;
import com.rendezvous.room.dto.CreateRoomRequest;
import com.rendezvous.room.dto.JoinRoomRequest;
import com.rendezvous.room.dto.RoomDetailsResponse;
import com.rendezvous.room.dto.RoomSummaryResponse;
import com.rendezvous.room.dto.UpdateRoomRequest;
import com.rendezvous.room.service.RoomMembershipService;
import com.rendezvous.room.service.RoomService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoomController {

    private final RoomService roomService;
    private final RoomMembershipService membership;

    public RoomController(RoomService roomService, RoomMembershipService membership) {
        this.roomService = roomService;
        this.membership = membership;
    }

    @PostMapping("/api/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomDetailsResponse create(@CurrentUserId UUID userId, @Valid @RequestBody CreateRoomRequest request) {
        return roomService.create(userId, request);
    }

    @GetMapping("/api/rooms/{id}")
    public RoomDetailsResponse get(@CurrentUserId UUID userId, @PathVariable UUID id) {
        return roomService.get(id, userId);
    }

    @PatchMapping("/api/rooms/{id}")
    public RoomDetailsResponse update(@CurrentUserId UUID userId, @PathVariable UUID id,
                                      @Valid @RequestBody UpdateRoomRequest request) {
        return roomService.update(id, userId, request);
    }

    @PostMapping("/api/rooms/{id}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void close(@CurrentUserId UUID userId, @PathVariable UUID id) {
        roomService.close(id, userId);
    }

    @DeleteMapping("/api/rooms/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUserId UUID userId, @PathVariable UUID id) {
        roomService.delete(id, userId);
    }

    @PostMapping("/api/rooms/join")
    public RoomDetailsResponse join(@CurrentUserId UUID userId, @Valid @RequestBody JoinRoomRequest request) {
        return membership.join(userId, request.code());
    }

    @PostMapping("/api/rooms/{id}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@CurrentUserId UUID userId, @PathVariable UUID id) {
        membership.leave(id, userId);
    }

    @GetMapping("/api/me/rooms")
    public PageResponse<RoomSummaryResponse> myRooms(
            @CurrentUserId UUID userId,
            @RequestParam(required = false) RoomStatus status,
            @ParameterObject @PageableDefault(sort = "startsAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return roomService.myRooms(userId, status, pageable);
    }
}
