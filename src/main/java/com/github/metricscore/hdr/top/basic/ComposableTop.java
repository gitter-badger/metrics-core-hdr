/*
 *    Copyright 2016 Vladimir Bukhtoyarov
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.github.metricscore.hdr.top.basic;

import com.github.metricscore.hdr.top.Top;


public interface ComposableTop extends Top {

    void reset();

    void addInto(Top other);

    static ComposableTop createNonConcurrentEmptyCopy(ComposableTop top) {
        int size = top.getPositionCount();
        long slowQueryThresholdNanos = top.getSlowQueryThresholdNanos();
        int maxLengthOfQueryDescription = top.getMaxLengthOfQueryDescription();
        if (size == 1) {
            return new SingletonTop(slowQueryThresholdNanos, maxLengthOfQueryDescription);
        } else {
            return new MultiPositionTop(size, slowQueryThresholdNanos, maxLengthOfQueryDescription);
        }
    }

    static ComposableTop createConcurrentTop(int size, long slowQueryThresholdNanos, int maxLengthOfQueryDescription) {
        if (size == 1) {
            return new ConcurrentSingletonTop(slowQueryThresholdNanos, maxLengthOfQueryDescription);
        } else {
            return new ConcurrentMultiPositionTop(size, slowQueryThresholdNanos, maxLengthOfQueryDescription);
        }
    }

}
