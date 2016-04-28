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

package com.github.metricscore.hdrhistogram.accumulator;


import com.codahale.metrics.Clock;
import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import com.github.metricscore.hdrhistogram.HdrBuilder;
import com.github.metricscore.hdrhistogram.MockClock;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static junit.framework.TestCase.assertEquals;

public class ResetPeriodicallyAccumulatorTest {

    @Test
    public void test() {
        AtomicLong time = new AtomicLong(System.currentTimeMillis());
        Clock wallClock = MockClock.mock(time);
        Reservoir reservoir = new HdrBuilder(wallClock)
                .resetReservoirPeriodically(Duration.ofMillis(1000))
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

    @Test(timeout = 12000)
    public void testThatConcurrentThreadsNotHungWithOneChunk() throws InterruptedException {
        Reservoir reservoir = new HdrBuilder()
                .resetReservoirPeriodically(Duration.ofSeconds(1))
                .buildReservoir();

        Util.runInParallel(reservoir, Duration.ofSeconds(10));
    }

}
