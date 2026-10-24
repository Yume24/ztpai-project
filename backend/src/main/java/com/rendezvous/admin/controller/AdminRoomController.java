package com.rendezvous.admin.controller;

import com.rendezvous.admin.dto.AdminRoomResponse;
import com.rendezvous.admin.service.AdminRoomService;
import com.rendezvous.common.web.PageResponse;
import com.rendezvous.room.domain.RoomStatus;
import java.time.Instant;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminRoomController {

    private final AdminRoomService service;

    public AdminRoomController(AdminRoomService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/rooms")
    public PageResponse<AdminRoomResponse> search(
            @RequestParam(required = false) RoomStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @ParameterObject @PageableDefault(sort = "startsAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.search(status, from, to, pageable);
    }

    @PostMapping("/api/admin/rooms/{id}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void close(@PathVariable UUID id) {
        service.close(id);
    }
}
