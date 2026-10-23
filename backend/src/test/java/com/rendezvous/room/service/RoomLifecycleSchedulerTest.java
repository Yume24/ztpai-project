package com.rendezvous.room.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.rendezvous.common.config.SchedulingConfig;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.config.FixedDelayTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;

class RoomLifecycleSchedulerTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(SchedulingConfig.class, RoomLifecycleScheduler.class)
            .withBean(RoomLifecycleService.class, () -> mock(RoomLifecycleService.class));

    @Test
    void enabledSchedulerRegistersTickEverySixtySeconds() {
        runner.withPropertyValues("app.scheduler.enabled=true").run(context ->
                assertThat(context.getBean(ScheduledTaskHolder.class).getScheduledTasks()).singleElement()
                        .satisfies(task -> {
                            assertThat(task.toString()).isEqualTo(RoomLifecycleScheduler.class.getName() + ".run");
                            assertThat(task.getTask()).isInstanceOfSatisfying(FixedDelayTask.class, fixedDelay ->
                                    assertThat(fixedDelay.getIntervalDuration()).isEqualTo(Duration.ofSeconds(60)));
                        }));
    }

    @Test
    void disabledSchedulerIsNotCreated() {
        runner.withPropertyValues("app.scheduler.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(RoomLifecycleScheduler.class));
    }
}
