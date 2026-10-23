package com.rendezvous.geofence.controller;

import com.rendezvous.common.security.CurrentUserId;
import com.rendezvous.geofence.dto.GeofenceRequest;
import com.rendezvous.geofence.service.GeofenceService;
import com.rendezvous.room.dto.GeofenceResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GeofenceController {

    private final GeofenceService service;

    public GeofenceController(GeofenceService service) {
        this.service = service;
    }

    @PutMapping("/api/rooms/{id}/geofence")
    public GeofenceResponse upsert(@CurrentUserId UUID userId, @PathVariable UUID id,
                                   @Valid @RequestBody GeofenceRequest request) {
        return service.upsert(id, userId, request);
    }

    @DeleteMapping("/api/rooms/{id}/geofence")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUserId UUID userId, @PathVariable UUID id) {
        service.delete(id, userId);
    }
}
