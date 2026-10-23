package com.rendezvous.alert.service;

import com.rendezvous.alert.domain.Alert;
import com.rendezvous.alert.domain.AlertType;
import com.rendezvous.alert.dto.AlertResponse;
import com.rendezvous.alert.dto.SubjectResponse;
import com.rendezvous.alert.event.AlertCreatedEvent;
import com.rendezvous.alert.repository.AlertRepository;
import com.rendezvous.common.error.ApiException;
import com.rendezvous.common.error.ErrorCode;
import com.rendezvous.common.web.PageResponse;
import com.rendezvous.room.domain.Room;
import com.rendezvous.room.domain.RoomPermissions;
import com.rendezvous.room.repository.RoomRepository;
import com.rendezvous.room.service.RoomAccessService;
import com.rendezvous.user.domain.User;
import com.rendezvous.user.repository.UserRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertService {

    private final AlertRepository alerts;
    private final RoomRepository rooms;
    private final UserRepository users;
    private final RoomAccessService access;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public AlertService(AlertRepository alerts, RoomRepository rooms, UserRepository users,
                        RoomAccessService access, ApplicationEventPublisher events, Clock clock) {
        this.alerts = alerts;
        this.rooms = rooms;
        this.users = users;
        this.access = access;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public void record(UUID roomId, UUID subjectUserId, AlertType type) {
        Room room = rooms.findById(roomId).orElseThrow(() -> new ApiException(ErrorCode.ROOM_NOT_FOUND));
        User subject = users.findById(subjectUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        Alert alert = alerts.save(new Alert(room, subject, type, clock.instant()));
        events.publishEvent(new AlertCreatedEvent(alert.getId(), roomId, room.getName(), subjectUserId,
                subject.getDisplayName(), type));
    }

    @Transactional(readOnly = true)
    public PageResponse<AlertResponse> list(UUID roomId, UUID viewerId, AlertType type, Pageable pageable) {
        if (!RoomPermissions.canViewAlerts(access.requireMember(roomId, viewerId).role())) {
            throw new ApiException(ErrorCode.ROOM_ACTION_FORBIDDEN);
        }
        Page<Alert> page = type == null
                ? alerts.findByRoomId(roomId, pageable)
                : alerts.findByRoomIdAndType(roomId, type, pageable);
        return PageResponse.from(page.map(AlertService::toResponse));
    }

    private static AlertResponse toResponse(Alert alert) {
        User subject = alert.getSubject();
        return new AlertResponse(alert.getId(), alert.getType(),
                new SubjectResponse(subject.getId(), subject.getDisplayName()), alert.getCreatedAt());
    }
}
