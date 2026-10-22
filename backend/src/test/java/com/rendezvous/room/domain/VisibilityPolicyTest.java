package com.rendezvous.room.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class VisibilityPolicyTest {

    private static MemberRef member(RoomRole role) {
        return new MemberRef(UUID.randomUUID(), role);
    }

    @ParameterizedTest
    @CsvSource({
            "ALL,MEMBER,MEMBER,true", "ALL,MEMBER,OWNER,true",
            "GUARDIANS_ONLY,MEMBER,MEMBER,false", "GUARDIANS_ONLY,MEMBER,ADMIN,true", "GUARDIANS_ONLY,MEMBER,OWNER,true",
            "GUARDIANS_ONLY,ADMIN,MEMBER,true", "GUARDIANS_ONLY,OWNER,MEMBER,true"})
    void positionVisibility(VisibilityMode mode, RoomRole viewer, RoomRole subject, boolean expected) {
        assertThat(VisibilityPolicy.canSeePosition(mode, member(viewer), member(subject))).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "ALL,false,MEMBER,MEMBER,false", "ALL,true,MEMBER,MEMBER,true",
            "GUARDIANS_ONLY,true,MEMBER,MEMBER,false", "GUARDIANS_ONLY,true,MEMBER,OWNER,true",
            "GUARDIANS_ONLY,false,ADMIN,MEMBER,true"})
    void trailVisibility(VisibilityMode mode, boolean membersSeeTrails, RoomRole viewer, RoomRole subject,
            boolean expected) {
        assertThat(VisibilityPolicy.canSeeTrail(mode, membersSeeTrails, member(viewer), member(subject)))
                .isEqualTo(expected);
    }

    @Test
    void everyoneSeesOwnPositionAndTrail() {
        UUID userId = UUID.randomUUID();
        MemberRef self = new MemberRef(userId, RoomRole.MEMBER);

        assertThat(VisibilityPolicy.canSeePosition(VisibilityMode.GUARDIANS_ONLY, self, self)).isTrue();
        assertThat(VisibilityPolicy.canSeeTrail(VisibilityMode.GUARDIANS_ONLY, false, self, self)).isTrue();
    }
}
