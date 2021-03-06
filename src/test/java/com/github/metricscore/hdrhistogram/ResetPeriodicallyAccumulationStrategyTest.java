/*
 *
 *  Copyright 2016 Vladimir Bukhtoyarov
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.github.metricscore.hdrhistogram;

import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import org.junit.Test;

import java.time.Duration;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static junit.framework.TestCase.assertEquals;

public class ResetPeriodicallyAccumulationStrategyTest {

    @Test(expected = IllegalArgumentException.class)
    public void negativeResetPeriodShouldNotAllowed() {
        new HdrBuilder().resetResevoirPeriodically(Duration.ofMinutes(-5));
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroResetPeriodShouldNotAllowed() {
        new HdrBuilder().resetResevoirPeriodically(Duration.ZERO);
    }

    @Test
    public void shouldCacheSnapshot() {
        AtomicLong time = new AtomicLong(System.currentTimeMillis());
        WallClock wallClock = WallClock.mock(time);
        Reservoir reservoir = new HdrBuilder(wallClock)
                .resetResevoirPeriodically(Duration.ofMillis(1000))
                .buildReservoir();

        reservoir.update(10);
        reservoir.update(20);
        Snapshot firstSnapshot = reservoir.getSnapshot();
        assertEquals(10, firstSnapshot.getMin());
        assertEquals(20, firstSnapshot.getMax());

        time.getAndAdd(900);
        reservoir.update(30);
        reservoir.update(40);
        Snapshot secondSnapshot = reservoir.getSnapshot();
        assertEquals(10, secondSnapshot.getMin());
        assertEquals(40, secondSnapshot.getMax());

        time.getAndAdd(99);
        reservoir.update(8);
        reservoir.update(60);
        Snapshot thirdSnapshot = reservoir.getSnapshot();
        assertEquals(8, thirdSnapshot.getMin());
        assertEquals(60, thirdSnapshot.getMax());

        time.getAndAdd(1);
        reservoir.update(70);
        reservoir.update(80);
        Snapshot firstNewSnapshot = reservoir.getSnapshot();
        assertEquals(70, firstNewSnapshot.getMin());
        assertEquals(80, firstNewSnapshot.getMax());

        time.getAndAdd(1001);
        reservoir.update(90);
        reservoir.update(100);
        Snapshot secondNewSnapshot = reservoir.getSnapshot();
        assertEquals(90, secondNewSnapshot.getMin());
        assertEquals(100, secondNewSnapshot.getMax());
    }

    @Test(timeout = 5000)
    public void testThatConcurrentThreadsNotHung() throws InterruptedException {
        Reservoir reservoir = new HdrBuilder()
                .resetResevoirPeriodically(Duration.ofMillis(1))
                .buildReservoir();

        AtomicBoolean stopFlag = new AtomicBoolean(false);

        // let concurrent threads to work fo 3 seconds
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                stopFlag.set(true);
            }
        }, 3000L);

        Thread[] threads = new Thread[Runtime.getRuntime().availableProcessors() * 2];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                Random random = new Random();
                // update reservoir 100 times and take snapshot on each cycle
                while (!stopFlag.get()) {
                    for (int j = 1; j <= 100; j++) {
                        reservoir.update(ThreadLocalRandom.current().nextInt(j));
                    }
                    reservoir.getSnapshot();
                }
            });
            threads[i].start();
        }
        for (Thread thread: threads) {
            thread.join();
        }
    }

}