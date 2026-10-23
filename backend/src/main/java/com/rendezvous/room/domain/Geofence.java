package com.rendezvous.room.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "geofences")
public class Geofence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false, unique = true)
    private Room room;

    @JdbcTypeCode(SqlTypes.GEOMETRY)
    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point center;

    @Column(name = "radius_m", nullable = false)
    private int radiusM;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Geofence() {
    }

    public Geofence(Room room, Point center, int radiusM, Instant updatedAt) {
        this.room = room;
        this.center = center;
        this.radiusM = radiusM;
        this.updatedAt = updatedAt;
    }

    public void moveTo(Point center, int radiusM, Instant at) {
        this.center = center;
        this.radiusM = radiusM;
        this.updatedAt = at;
    }

    public UUID getId() {
        return id;
    }

    public Room getRoom() {
        return room;
    }

    public Point getCenter() {
        return center;
    }

    public int getRadiusM() {
        return radiusM;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
