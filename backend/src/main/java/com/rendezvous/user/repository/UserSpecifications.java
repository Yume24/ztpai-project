package com.rendezvous.user.repository;

import com.rendezvous.user.domain.User;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> matchesQuery(String query) {
        return (root, cq, cb) -> {
            if (query == null || query.isBlank()) {
                return null;
            }
            String pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
            return cb.or(cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(root.get("displayName")), pattern));
        };
    }

    public static Specification<User> isBlocked(Boolean blocked) {
        return (root, cq, cb) -> {
            if (blocked == null) {
                return null;
            }
            return blocked ? cb.isNotNull(root.get("blockedAt")) : cb.isNull(root.get("blockedAt"));
        };
    }
}
