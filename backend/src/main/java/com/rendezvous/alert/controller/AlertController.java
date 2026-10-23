package com.rendezvous.alert.controller;

import com.rendezvous.alert.domain.AlertType;
import com.rendezvous.alert.dto.AlertResponse;
import com.rendezvous.alert.service.AlertService;
import com.rendezvous.common.security.CurrentUserId;
import com.rendezvous.common.web.PageResponse;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AlertController {

    private final AlertService alerts;

    public AlertController(AlertService alerts) {
        this.alerts = alerts;
    }

    @GetMapping("/api/rooms/{id}/alerts")
    public PageResponse<AlertResponse> list(
            @CurrentUserId UUID userId,
            @PathVariable UUID id,
            @RequestParam(required = false) AlertType type,
            @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return alerts.list(id, userId, type, pageable);
    }
}
