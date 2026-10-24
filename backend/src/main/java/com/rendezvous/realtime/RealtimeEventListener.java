package com.rendezvous.realtime;

import com.rendezvous.admin.event.UserBlockedEvent;
import com.rendezvous.alert.event.AlertCreatedEvent;
import com.rendezvous.geofence.event.GeofenceChangedEvent;
import com.rendezvous.location.event.LocationsRecordedEvent;
import com.rendezvous.room.event.MemberJoinedEvent;
import com.rendezvous.room.event.MemberLeftEvent;
import com.rendezvous.room.event.MemberRemovedEvent;
import com.rendezvous.room.event.MemberRoleChangedEvent;
import com.rendezvous.room.event.RoomEndedEvent;
import com.rendezvous.room.event.RoomStartedEvent;
import com.rendezvous.room.event.RoomUpdatedEvent;
import com.rendezvous.room.service.RoomAudienceService;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RealtimeEventListener {

    private static final Map<String, Object> EMPTY = Map.of();

    private final RoomAudienceService audience;
    private final RealtimePublisher publisher;
    private final SessionRegistry sessions;

    public RealtimeEventListener(RoomAudienceService audience, RealtimePublisher publisher,
                                 SessionRegistry sessions) {
        this.audience = audience;
        this.publisher = publisher;
        this.sessions = sessions;
    }

    @Async("eventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLocationsRecorded(LocationsRecordedEvent event) {
        UUID subject = event.latest().userId();
        publisher.send(audience.positionViewerIds(event.roomId(), subject),
                new RealtimeMessage(RealtimeEventType.LOCATION_UPDATED, event.roomId(), event.latest()));
    }

    @Async("eventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAlertCreated(AlertCreatedEvent event) {
        publisher.send(audience.guardianIds(event.roomId()),
                new RealtimeMessage(RealtimeEventType.ALERT_CREATED, event.roomId(),
                        Map.of("alertId", event.alertId(), "type", event.type(), "userId", event.subjectUserId())));
    }

    @Async("eventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMemberJoined(MemberJoinedEvent event) {
        toActiveMembers(RealtimeEventType.MEMBER_JOINED, event.roomId(), Map.of("userId", event.userId()));
    }

    @Async("eventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMemberLeft(MemberLeftEvent event) {
        toActiveMembers(RealtimeEventType.MEMBER_LEFT, event.roomId(), Map.of("userId", event.userId()));
    }

    @Async("eventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMemberRemoved(MemberRemovedEvent event) {
        Set<UUID> recipients = new HashSet<>(audience.activeMemberIds(event.roomId()));
        recipients.add(event.userId());
        publisher.send(recipients, new RealtimeMessage(RealtimeEventType.MEMBER_REMOVED, event.roomId(),
                Map.of("userId", event.userId())));
    }

    @Async("eventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMemberRoleChanged(MemberRoleChangedEvent event) {
        toActiveMembers(RealtimeEventType.MEMBER_ROLE_CHANGED, event.roomId(), Map.of("userId", event.userId()));
    }

    @Async("eventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRoomUpdated(RoomUpdatedEvent event) {
        toActiveMembers(RealtimeEventType.ROOM_UPDATED, event.roomId(), EMPTY);
    }

    @Async("eventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGeofenceChanged(GeofenceChangedEvent event) {
        toActiveMembers(RealtimeEventType.GEOFENCE_UPDATED, event.roomId(), EMPTY);
    }

    @Async("eventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRoomStarted(RoomStartedEvent event) {
        toActiveMembers(RealtimeEventType.ROOM_STARTED, event.roomId(), EMPTY);
    }

    @Async("eventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRoomEnded(RoomEndedEvent event) {
        publisher.send(event.memberUserIds(),
                new RealtimeMessage(RealtimeEventType.ROOM_ENDED, event.roomId(), EMPTY));
    }

    @Async("eventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserBlocked(UserBlockedEvent event) {
        sessions.closeAll(event.userId());
    }

    private void toActiveMembers(RealtimeEventType type, UUID roomId, Object payload) {
        publisher.send(audience.activeMemberIds(roomId), new RealtimeMessage(type, roomId, payload));
    }
}
