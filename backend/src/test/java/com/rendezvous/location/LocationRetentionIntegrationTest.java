package com.rendezvous.location;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rendezvous.auth.dto.AuthResponse;
import com.rendezvous.location.dto.LocationPointRequest;
import com.rendezvous.location.service.LocationIngestService;
import com.rendezvous.room.dto.RoomDetailsResponse;
import com.rendezvous.room.repository.RoomRepository;
import com.rendezvous.room.service.RoomLifecycleService;
import com.rendezvous.support.IntegrationTest;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

class LocationRetentionIntegrationTest extends IntegrationTest {

    private static final double LAT = 50.0614;
    private static final double LON = 19.9366;

    @Autowired
    private RoomRepository rooms;

    @Autowired
    private RoomLifecycleService lifecycle;

    @Autowired
    private LocationIngestService ingest;

    @Autowired
    private TransactionTemplate tx;

    @Autowired
    private JdbcTemplate jdbc;

    private int pointCount(RoomDetailsResponse room) {
        return jdbc.queryForObject("""
                select count(*) from location_points p join room_members m on m.id = p.member_id
                where m.room_id = ?""", Integer.class, room.id());
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void ingestSkipsRoomWhoseEndIsAlreadyProcessed() {
        String owner = api.token("ret-processed@x.pl");
        RoomDetailsResponse room = api.createActiveRoom(owner);
        tx.executeWithoutResult(status -> rooms.findById(room.id()).orElseThrow().markEndProcessed(Instant.now()));

        assertThat(api.postLocation(owner, LAT, LON, 10f, Instant.now()).activeRoomCount()).isZero();
        assertThat(pointCount(room)).isZero();
    }

    @Test
    void ingestWaitsForConcurrentEndProcessingAndThenSkipsTheRoom() throws Exception {
        AuthResponse owner = api.register("ret-race@x.pl");
        RoomDetailsResponse room = api.createActiveRoom(owner.accessToken());
        CountDownLatch endProcessed = new CountDownLatch(1);
        CountDownLatch commit = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> ending = executor.submit(() -> tx.executeWithoutResult(status -> {
                lifecycle.processEnd(rooms.findById(room.id()).orElseThrow());
                endProcessed.countDown();
                awaitQuietly(commit);
            }));
            assertThat(endProcessed.await(5, TimeUnit.SECONDS)).isTrue();

            Future<Integer> ingesting = executor.submit(() -> ingest.ingest(owner.user().id(),
                    List.of(new LocationPointRequest(LAT, LON, 10f, Instant.now()))));
            assertThatThrownBy(() -> ingesting.get(500, TimeUnit.MILLISECONDS))
                    .as("ingest waits for the room row locked by end processing")
                    .isInstanceOf(TimeoutException.class);

            commit.countDown();
            ending.get(5, TimeUnit.SECONDS);
            assertThat(ingesting.get(5, TimeUnit.SECONDS)).isZero();
            assertThat(pointCount(room)).isZero();
        } finally {
            commit.countDown();
            executor.shutdownNow();
        }
    }
}
