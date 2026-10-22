package com.rendezvous.room.domain;

public final class RoomPermissions {

    private RoomPermissions() {
    }

    public static boolean canEditRoom(RoomRole actor) {
        return actor.isGuardian();
    }

    public static boolean canManageGeofence(RoomRole actor) {
        return actor.isGuardian();
    }

    public static boolean canViewAlerts(RoomRole actor) {
        return actor.isGuardian();
    }

    public static boolean canChangeRoles(RoomRole actor) {
        return actor == RoomRole.OWNER;
    }

    public static boolean canRemove(RoomRole actor, RoomRole target) {
        if (target == RoomRole.OWNER) {
            return false;
        }
        return actor == RoomRole.OWNER || (actor == RoomRole.ADMIN && target == RoomRole.MEMBER);
    }

    public static boolean canLeave(RoomRole actor) {
        return actor != RoomRole.OWNER;
    }

    public static boolean canCloseOrDelete(RoomRole actor) {
        return actor == RoomRole.OWNER;
    }
}
