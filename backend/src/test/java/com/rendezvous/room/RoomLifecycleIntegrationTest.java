package com.rendezvous.room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rendezvous.auth.dto.AuthResponse;
import com.rendezvous.common.geo.GeoPoints;
import com.rendezvous.location.domain.LocationPoint;
import com.rendezvous.location.repository.LocationPointRepository;
import com.rendezvous.room.domain.Room;
import com.rendezvous.room.domain.RoomMember;
import com.rendezvous.room.domain.RoomRole;
import com.rendezvous.room.dto.RoomDetailsResponse;
import com.rendezvous.room.repository.RoomMemberRepository;
import com.rendezvous.room.repository.RoomRepository;
import com.rendezvous.room.service.RoomCodeGenerator;
import com.rendezvous.room.service.RoomLifecycleService;
import com.rendezvous.support.IntegrationTest;
import com.rendezvous.user.domain.User;
import com.rendezvous.user.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class RoomLifecycleIntegrationTest extends IntegrationTest {

    @Autowired
    private UserRepository users;

    @Autowired
    private RoomRepository rooms;

    @Autowired
    private RoomMemberRepository members;

    @Autowired
    private LocationPointRepository points;

    @Autowired
    private RoomCodeGenerator codes;

    @Autowired
    private RoomLifecycleService lifecycle;

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private RoomMember ownerOfRoom(String email, Instant startsAt, Instant endsAt) {
        User owner = users.findById(api.register(email).user().id()).orElseThrow();
        Room room = rooms.save(new Room("Due", codes.generate(), owner, startsAt, endsAt, startsAt));
        return members.save(new RoomMember(room, owner, RoomRole.OWNER, startsAt));
    }

    @Test
    void endDueRoomsProcessesRoomPastItsWindowAndDeletesItsTrail() {
        Instant now = Instant.now();
        RoomMember owner = ownerOfRoom("lc-due-end@x.pl", now.minus(Duration.ofHours(2)), now.minusSeconds(60));
        Instant recordedAt = now.minus(Duration.ofHours(1));
        points.save(new LocationPoint(owner, GeoPoints.point(50.0614, 19.9366), 10f, recordedAt, recordedAt));

        lifecycle.endDueRooms();

        assertThat(rooms.findById(owner.getRoom().getId()).orElseThrow().getEndProcessedAt()).isNotNull();
        assertThat(points.findByMemberIdOrderByRecordedAtAsc(owner.getId())).isEmpty();
    }

    @Test
    void startDueRoomsMarksRoomWhoseStartHasCome() {
        Instant now = Instant.now();
        RoomMember owner = ownerOfRoom("lc-due-start@x.pl", now.minusSeconds(60), now.plus(Duration.ofHours(1)));

        lifecycle.startDueRooms();

        assertThat(rooms.findById(owner.getRoom().getId()).orElseThrow().getStartNotifiedAt()).isNotNull();
    }

    @Test
    void ownerClosesRoomAndItBecomesEnded() throws Exception {
        String owner = api.token("lc-close-owner@x.pl");
        RoomDetailsResponse room = api.createActiveRoom(owner);

        mvc.perform(post("/api/rooms/{id}/close", room.id()).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/rooms/{id}", room.id()).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENDED"))
                .andExpect(jsonPath("$.closedAt").isNotEmpty());
        mvc.perform(post("/api/rooms/{id}/close", room.id()).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROOM_ENDED"));
    }

    @Test
    void adminOfRoomCannotClose() throws Exception {
        String owner = api.token("lc-admin-owner@x.pl");
        AuthResponse admin = api.register("lc-admin@x.pl");
        RoomDetailsResponse room = api.createActiveRoom(owner);
        api.join(admin.accessToken(), room.code());
        mvc.perform(patch("/api/rooms/{id}/members/{userId}", room.id(), admin.user().id())
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/rooms/{id}/close", room.id())
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin.accessToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ROOM_ACTION_FORBIDDEN"));
    }

    @Test
    void ownerDeletesActiveRoom() throws Exception {
        String owner = api.token("lc-del-owner@x.pl");
        String guest = api.token("lc-del-guest@x.pl");
        RoomDetailsResponse room = api.createActiveRoom(owner);
        api.join(guest, room.code());

        mvc.perform(delete("/api/rooms/{id}", room.id()).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/rooms/{id}", room.id()).header(HttpHeaders.AUTHORIZATION, bearer(guest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void joiningClosedRoomIsRoomEnded() throws Exception {
        String owner = api.token("lc-join-owner@x.pl");
        String guest = api.token("lc-join-guest@x.pl");
        RoomDetailsResponse room = api.createActiveRoom(owner);
        mvc.perform(post("/api/rooms/{id}/close", room.id()).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/rooms/join").header(HttpHeaders.AUTHORIZATION, bearer(guest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + room.code() + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROOM_ENDED"));
    }
}
