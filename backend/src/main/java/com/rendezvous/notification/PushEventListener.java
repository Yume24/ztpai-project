package com.rendezvous.notification;

import com.rendezvous.alert.event.AlertCreatedEvent;
import com.rendezvous.room.event.MemberRemovedEvent;
import com.rendezvous.room.event.RoomStartedEvent;
import com.rendezvous.room.service.RoomAudienceService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PushEventListener {

    private final RoomAudienceService audience;
    private final PushNotificationService notifications;

    public PushEventListener(RoomAudienceService audience, PushNotificationService notifications) {
        this.audience = audience;
        this.notifications = notifications;
    }

    @Async("eventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRoomStarted(RoomStartedEvent event) {
        notifications.notify(audience.activeMemberIds(event.roomId()),
                PushTexts.roomStarted(event.roomName()), data("ROOM_STARTED", event.roomId()));
    }

    @Async("eventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAlertCreated(AlertCreatedEvent event) {
        notifications.notify(audience.guardianIds(event.roomId()),
                PushTexts.alert(event.type(), event.subjectDisplayName(), event.roomName()),
                data("ALERT", event.roomId()));
    }

    @Async("eventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMemberRemoved(MemberRemovedEvent event) {
        notifications.notify(List.of(event.userId()),
                PushTexts.removed(event.roomName()), data("MEMBER_REMOVED", event.roomId()));
    }

    private static Map<String, String> data(String type, UUID roomId) {
        return Map.of("type", type, "roomId", roomId.toString());
    }
}
