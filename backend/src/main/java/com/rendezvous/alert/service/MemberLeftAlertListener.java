package com.rendezvous.alert.service;

import com.rendezvous.alert.domain.AlertType;
import com.rendezvous.room.event.MemberLeftEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class MemberLeftAlertListener {

    private final AlertService alerts;

    public MemberLeftAlertListener(AlertService alerts) {
        this.alerts = alerts;
    }

    @EventListener
    public void onMemberLeft(MemberLeftEvent event) {
        if (!event.roomEnded()) {
            alerts.record(event.roomId(), event.userId(), AlertType.MEMBER_LEFT);
        }
    }
}
