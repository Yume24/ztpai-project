package com.rendezvous.room.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rendezvous.room.domain.MemberStatus;
import com.rendezvous.room.domain.Room;
import com.rendezvous.room.domain.RoomMember;
import com.rendezvous.room.domain.RoomRole;
import com.rendezvous.room.domain.VisibilityMode;
import com.rendezvous.room.repository.RoomMemberRepository;
import com.rendezvous.room.repository.RoomRepository;
import com.rendezvous.user.domain.User;
import com.rendezvous.user.domain.UserRole;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RoomAudienceServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");

    private final RoomRepository rooms = mock(RoomRepository.class);
    private final RoomMemberRepository members = mock(RoomMemberRepository.class);
    private final RoomAudienceService service = new RoomAudienceService(rooms, members);

    private final UUID roomId = UUID.randomUUID();
    private final User ownerUser = user("owner");
    private final User adminUser = user("admin");
    private final User subjectUser = user("subject");
    private final User otherUser = user("other");

    private static User user(String name) {
        User user = new User(name + "@x.pl", "hash", name, UserRole.USER, NOW);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    private void setUp(VisibilityMode mode) {
        Room room = new Room("Trip", "ABC234", ownerUser, NOW, NOW.plus(Duration.ofHours(1)), NOW);
        ReflectionTestUtils.setField(room, "id", roomId);
        room.changeSettings(mode, false);
        when(rooms.findById(roomId)).thenReturn(Optional.of(room));
        when(members.findByRoomIdAndStatus(roomId, MemberStatus.ACTIVE)).thenReturn(List.of(
                new RoomMember(room, ownerUser, RoomRole.OWNER, NOW),
                new RoomMember(room, adminUser, RoomRole.ADMIN, NOW),
                new RoomMember(room, subjectUser, RoomRole.MEMBER, NOW),
                new RoomMember(room, otherUser, RoomRole.MEMBER, NOW)));
    }

    @Test
    void guardiansOnlyModeHidesMemberFromOtherMembers() {
        setUp(VisibilityMode.GUARDIANS_ONLY);

        assertThat(service.positionViewerIds(roomId, subjectUser.getId()))
                .containsExactlyInAnyOrder(ownerUser.getId(), adminUser.getId());
    }

    @Test
    void allModeSendsToEveryoneExceptSubject() {
        setUp(VisibilityMode.ALL);

        assertThat(service.positionViewerIds(roomId, subjectUser.getId()))
                .containsExactlyInAnyOrder(ownerUser.getId(), adminUser.getId(), otherUser.getId());
    }

    @Test
    void guardianIdsAreOwnerAndAdmin() {
        setUp(VisibilityMode.ALL);

        assertThat(service.guardianIds(roomId)).containsExactlyInAnyOrder(ownerUser.getId(), adminUser.getId());
        assertThat(service.activeMemberIds(roomId)).hasSize(4);
    }
}
