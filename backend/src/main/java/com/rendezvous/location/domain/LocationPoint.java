package com.rendezvous.location.domain;

import com.rendezvous.room.domain.RoomMember;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "location_points")
public class LocationPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private RoomMember member;

    @JdbcTypeCode(SqlTypes.GEOMETRY)
    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point position;

    @Column(name = "accuracy_m", nullable = false)
    private float accuracyM;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected LocationPoint() {
    }

    public LocationPoint(RoomMember member, Point position, float accuracyM, Instant recordedAt, Instant receivedAt) {
        this.member = member;
        this.position = position;
        this.accuracyM = accuracyM;
        this.recordedAt = recordedAt;
        this.receivedAt = receivedAt;
    }

    public Long getId() {
        return id;
    }

    public RoomMember getMember() {
        return member;
    }

    public Point getPosition() {
        return position;
    }

    public float getAccuracyM() {
        return accuracyM;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}
