package com.rendezvous.location.controller;

import com.rendezvous.common.security.CurrentUserId;
import com.rendezvous.location.dto.IngestResponse;
import com.rendezvous.location.dto.LocationBatchRequest;
import com.rendezvous.location.dto.PositionResponse;
import com.rendezvous.location.dto.TrailPointResponse;
import com.rendezvous.location.service.LocationIngestService;
import com.rendezvous.location.service.LocationQueryService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LocationController {

    private final LocationIngestService ingest;
    private final LocationQueryService query;

    public LocationController(LocationIngestService ingest, LocationQueryService query) {
        this.ingest = ingest;
        this.query = query;
    }

    @PostMapping("/api/me/locations")
    public IngestResponse ingest(@CurrentUserId UUID userId, @Valid @RequestBody LocationBatchRequest request) {
        return new IngestResponse(ingest.ingest(userId, request.points()));
    }

    @GetMapping("/api/rooms/{id}/locations/latest")
    public List<PositionResponse> latest(@CurrentUserId UUID userId, @PathVariable UUID id) {
        return query.latest(id, userId);
    }

    @GetMapping("/api/rooms/{id}/members/{userId}/trail")
    public List<TrailPointResponse> trail(@CurrentUserId UUID viewerId, @PathVariable UUID id,
                                          @PathVariable UUID userId) {
        return query.trail(id, viewerId, userId);
    }
}
