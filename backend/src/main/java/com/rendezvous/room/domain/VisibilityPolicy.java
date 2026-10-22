package com.rendezvous.room.domain;

public final class VisibilityPolicy {

    private VisibilityPolicy() {
    }

    public static boolean canSeePosition(VisibilityMode mode, MemberRef viewer, MemberRef subject) {
        if (viewer.userId().equals(subject.userId()) || viewer.role().isGuardian()) {
            return true;
        }
        if (mode == VisibilityMode.ALL) {
            return true;
        }
        return subject.role().isGuardian();
    }

    public static boolean canSeeTrail(VisibilityMode mode, boolean membersSeeTrails,
            MemberRef viewer, MemberRef subject) {
        if (viewer.userId().equals(subject.userId()) || viewer.role().isGuardian()) {
            return true;
        }
        return membersSeeTrails && canSeePosition(mode, viewer, subject);
    }
}
