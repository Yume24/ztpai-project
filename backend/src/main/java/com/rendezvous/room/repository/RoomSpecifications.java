package com.rendezvous.room.repository;

import com.rendezvous.room.domain.MemberStatus;
import com.rendezvous.room.domain.Room;
import com.rendezvous.room.domain.RoomMember;
import com.rendezvous.room.domain.RoomStatus;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class RoomSpecifications {

    private RoomSpecifications() {
    }

    public static Specification<Room> hasStatus(RoomStatus status, Instant now) {
        return (root, query, cb) -> {
            if (status == null) {
                return null;
            }
            Predicate open = cb.isNull(root.get("closedAt"));
            Predicate started = cb.lessThanOrEqualTo(root.get("startsAt"), now);
            Predicate notEnded = cb.greaterThan(root.get("endsAt"), now);
            return switch (status) {
                case SCHEDULED -> cb.and(open, cb.greaterThan(root.get("startsAt"), now));
                case ACTIVE -> cb.and(open, started, notEnded);
                case ENDED -> cb.or(cb.lessThanOrEqualTo(root.get("endsAt"), now), cb.isNotNull(root.get("closedAt")));
            };
        };
    }

    public static Specification<Room> startsBetween(Instant from, Instant to) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (from != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("startsAt"), from));
            }
            if (to != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("startsAt"), to));
            }
            return predicate;
        };
    }

    public static Specification<Room> hasActiveMember(UUID userId) {
        return (root, query, cb) -> {
            if (userId == null) {
                return null;
            }
            var subquery = query.subquery(Integer.class);
            var member = subquery.from(RoomMember.class);
            subquery.select(cb.literal(1)).where(
                    cb.equal(member.get("room"), root),
                    cb.equal(member.get("user").get("id"), userId),
                    cb.equal(member.get("status"), MemberStatus.ACTIVE));
            return cb.exists(subquery);
        };
    }
}
