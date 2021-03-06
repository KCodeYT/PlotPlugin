/*
 * Copyright 2022 KCodeYT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ms.kevi.plotplugin.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
public class Waiter {

    private final List<Runnable> tasks = new ArrayList<>();
    private final AtomicInteger waitingCounter = new AtomicInteger(0);

    public synchronized void addTask(Runnable task) {
        if(this.waitingCounter.get() > 0)
            this.tasks.add(task);
        else
            task.run();
    }

    public synchronized void waitOn() {
        this.waitingCounter.set(this.waitingCounter.get() + 1);
    }

    public synchronized void finish() {
        this.waitingCounter.set(this.waitingCounter.get() - 1);
        if(this.waitingCounter.get() == 0) {
            this.tasks.forEach(Runnable::run);
            this.tasks.clear();
        }
    }

}
