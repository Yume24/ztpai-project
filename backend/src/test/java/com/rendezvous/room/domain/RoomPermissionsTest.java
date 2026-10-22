package com.rendezvous.room.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RoomPermissionsTest {

    @ParameterizedTest
    @CsvSource({"OWNER,ADMIN,true", "OWNER,MEMBER,true", "ADMIN,MEMBER,true", "ADMIN,ADMIN,false",
            "ADMIN,OWNER,false", "MEMBER,MEMBER,false", "OWNER,OWNER,false"})
    void canRemove(RoomRole actor, RoomRole target, boolean expected) {
        assertThat(RoomPermissions.canRemove(actor, target)).isEqualTo(expected);
    }

    @Test
    void ownerCannotLeaveButOthersCan() {
        assertThat(RoomPermissions.canLeave(RoomRole.OWNER)).isFalse();
        assertThat(RoomPermissions.canLeave(RoomRole.ADMIN)).isTrue();
        assertThat(RoomPermissions.canLeave(RoomRole.MEMBER)).isTrue();
    }

    @Test
    void onlyOwnerChangesRolesAndClosesRoom() {
        assertThat(RoomPermissions.canChangeRoles(RoomRole.OWNER)).isTrue();
        assertThat(RoomPermissions.canChangeRoles(RoomRole.ADMIN)).isFalse();
        assertThat(RoomPermissions.canChangeRoles(RoomRole.MEMBER)).isFalse();

        assertThat(RoomPermissions.canCloseOrDelete(RoomRole.OWNER)).isTrue();
        assertThat(RoomPermissions.canCloseOrDelete(RoomRole.ADMIN)).isFalse();
        assertThat(RoomPermissions.canCloseOrDelete(RoomRole.MEMBER)).isFalse();
    }

    @Test
    void guardiansEditRoomManageGeofenceAndViewAlerts() {
        assertThat(RoomPermissions.canEditRoom(RoomRole.OWNER)).isTrue();
        assertThat(RoomPermissions.canEditRoom(RoomRole.ADMIN)).isTrue();
        assertThat(RoomPermissions.canEditRoom(RoomRole.MEMBER)).isFalse();

        assertThat(RoomPermissions.canManageGeofence(RoomRole.OWNER)).isTrue();
        assertThat(RoomPermissions.canManageGeofence(RoomRole.ADMIN)).isTrue();
        assertThat(RoomPermissions.canManageGeofence(RoomRole.MEMBER)).isFalse();

        assertThat(RoomPermissions.canViewAlerts(RoomRole.OWNER)).isTrue();
        assertThat(RoomPermissions.canViewAlerts(RoomRole.ADMIN)).isTrue();
        assertThat(RoomPermissions.canViewAlerts(RoomRole.MEMBER)).isFalse();
    }
}
