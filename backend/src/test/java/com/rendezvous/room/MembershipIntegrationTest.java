package com.rendezvous.room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rendezvous.auth.dto.AuthResponse;
import com.rendezvous.room.domain.MemberStatus;
import com.rendezvous.room.domain.Room;
import com.rendezvous.room.domain.RoomMember;
import com.rendezvous.room.domain.RoomRole;
import com.rendezvous.room.dto.RoomDetailsResponse;
import com.rendezvous.room.repository.RoomMemberRepository;
import com.rendezvous.room.repository.RoomRepository;
import com.rendezvous.support.IntegrationTest;
import com.rendezvous.user.domain.User;
import com.rendezvous.user.domain.UserRole;
import com.rendezvous.user.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class MembershipIntegrationTest extends IntegrationTest {

    @Autowired
    private RoomRepository rooms;

    @Autowired
    private RoomMemberRepository members;

    @Autowired
    private UserRepository users;

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void joinByCodeThenListMembers() throws Exception {
        String owner = api.token("join-owner@x.pl");
        String guest = api.token("join-guest@x.pl");
        RoomDetailsResponse room = api.createActiveRoom(owner);

        RoomDetailsResponse joined = api.join(guest, room.code().toLowerCase());

        assertThat(joined.myRole()).isEqualTo(RoomRole.MEMBER);
        mvc.perform(get("/api/rooms/{id}/members", room.id()).header(HttpHeaders.AUTHORIZATION, bearer(guest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].role").value("OWNER"))
                .andExpect(jsonPath("$[1].displayName").value("join-guest"));
    }

    @Test
    void removedUserGets403OnRejoin() throws Exception {
        String owner = api.token("rm-owner@x.pl");
        AuthResponse guest = api.register("rm-guest@x.pl");
        RoomDetailsResponse room = api.createActiveRoom(owner);
        api.join(guest.accessToken(), room.code());

        mvc.perform(delete("/api/rooms/{id}/members/{userId}", room.id(), guest.user().id())
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/rooms/join").header(HttpHeaders.AUTHORIZATION, bearer(guest.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + room.code() + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MEMBER_REMOVED"));
    }

    @Test
    void ownerPromotesMemberToAdmin() throws Exception {
        String owner = api.token("promo-owner@x.pl");
        AuthResponse guest = api.register("promo-guest@x.pl");
        RoomDetailsResponse room = api.createActiveRoom(owner);
        api.join(guest.accessToken(), room.code());

        mvc.perform(patch("/api/rooms/{id}/members/{userId}", room.id(), guest.user().id())
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        mvc.perform(get("/api/rooms/{id}/members", room.id()).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(jsonPath("$[1].role").value("ADMIN"));
    }

    @Test
    void concurrentJoinsNeverExceedFiftyMembers() throws Exception {
        String owner = api.token("race-owner@x.pl");
        String first = api.token("race-first@x.pl");
        String second = api.token("race-second@x.pl");
        RoomDetailsResponse details = api.createActiveRoom(owner);
        Room room = rooms.findById(details.id()).orElseThrow();
        for (int i = 0; i < 48; i++) {
            User filler = users.save(new User("race-filler-" + i + "@x.pl", "hash", "filler" + i, UserRole.USER,
                    Instant.now()));
            members.save(new RoomMember(room, filler, RoomRole.MEMBER, Instant.now()));
        }
        assertThat(members.countByRoomIdAndStatus(room.getId(), MemberStatus.ACTIVE)).isEqualTo(49);

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<MvcResult>> results = new ArrayList<>();
            for (String token : List.of(first, second)) {
                results.add(pool.submit(() -> {
                    start.await();
                    return mvc.perform(post("/api/rooms/join").header(HttpHeaders.AUTHORIZATION, bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"code\":\"" + details.code() + "\"}")).andReturn();
                }));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<MvcResult> result : results) {
                statuses.add(result.get().getResponse().getStatus());
            }
            assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        } finally {
            pool.shutdownNow();
        }
        assertThat(members.countByRoomIdAndStatus(room.getId(), MemberStatus.ACTIVE)).isEqualTo(50);
    }
}
