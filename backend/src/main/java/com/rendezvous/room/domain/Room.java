package com.rendezvous.room.domain;

import com.rendezvous.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(nullable = false, unique = true, length = 6)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility_mode", nullable = false)
    private VisibilityMode visibilityMode = VisibilityMode.ALL;

    @Column(name = "members_see_trails", nullable = false)
    private boolean membersSeeTrails = false;

    @Column(name = "start_notified_at")
    private Instant startNotifiedAt;

    @Column(name = "end_processed_at")
    private Instant endProcessedAt;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Room() {
    }

    public Room(String name, String code, User owner, Instant startsAt, Instant endsAt, Instant createdAt) {
        this.name = name;
        this.code = code;
        this.owner = owner;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.createdAt = createdAt;
    }

    public RoomStatus statusAt(Instant now) {
        if (!now.isBefore(effectiveEnd())) {
            return RoomStatus.ENDED;
        }
        return now.isBefore(startsAt) ? RoomStatus.SCHEDULED : RoomStatus.ACTIVE;
    }

    public Instant effectiveEnd() {
        return closedAt != null ? closedAt : endsAt;
    }

    public void close(Instant at) {
        this.closedAt = at;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void reschedule(Instant startsAt, Instant endsAt) {
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    public void changeSettings(VisibilityMode mode, boolean membersSeeTrails) {
        this.visibilityMode = mode;
        this.membersSeeTrails = membersSeeTrails;
    }

    public void markStartNotified(Instant at) {
        this.startNotifiedAt = at;
    }

    public void markEndProcessed(Instant at) {
        this.endProcessedAt = at;
    }

    public boolean isEndProcessed() {
        return endProcessedAt != null;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public User getOwner() {
        return owner;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public VisibilityMode getVisibilityMode() {
        return visibilityMode;
    }

    public boolean isMembersSeeTrails() {
        return membersSeeTrails;
    }

    public Instant getStartNotifiedAt() {
        return startNotifiedAt;
    }

    public Instant getEndProcessedAt() {
        return endProcessedAt;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
