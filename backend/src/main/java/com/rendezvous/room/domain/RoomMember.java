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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "room_members", uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "user_id"}))
public class RoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "geofence_state", nullable = false)
    private GeofenceState geofenceState;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    protected RoomMember() {
    }

    public RoomMember(Room room, User user, RoomRole role, Instant joinedAt) {
        this.room = room;
        this.user = user;
        this.role = role;
        this.joinedAt = joinedAt;
        this.status = MemberStatus.ACTIVE;
        this.geofenceState = GeofenceState.UNKNOWN;
    }

    public void leave(Instant at) {
        this.status = MemberStatus.LEFT;
        this.leftAt = at;
    }

    public void remove(Instant at) {
        this.status = MemberStatus.REMOVED;
        this.leftAt = at;
    }

    public void rejoin(Instant at) {
        this.status = MemberStatus.ACTIVE;
        this.role = RoomRole.MEMBER;
        this.leftAt = null;
        this.geofenceState = GeofenceState.UNKNOWN;
        this.joinedAt = at;
    }

    public void changeRole(RoomRole role) {
        this.role = role;
        this.geofenceState = GeofenceState.UNKNOWN;
    }

    public void setGeofenceState(GeofenceState state) {
        this.geofenceState = state;
    }

    public MemberRef ref() {
        return new MemberRef(user.getId(), role);
    }

    public UUID getId() {
        return id;
    }

    public Room getRoom() {
        return room;
    }

    public User getUser() {
        return user;
    }

    public RoomRole getRole() {
        return role;
    }

    public MemberStatus getStatus() {
        return status;
    }

    public GeofenceState getGeofenceState() {
        return geofenceState;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public Instant getLeftAt() {
        return leftAt;
    }
}
