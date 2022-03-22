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

package ms.kevi.plotplugin.util.async;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kevims KCodeYT
 */
public class TaskHelper {

    private final List<Runnable> asyncTasks = new ArrayList<>();
    private final List<Runnable> syncTasks = new ArrayList<>();

    public void addAsyncTask(Runnable task) {
        this.asyncTasks.add(task);
    }

    public void addSyncTask(Runnable task) {
        this.syncTasks.add(task);
    }

    public void runAsyncTasks() {
        for(Runnable asyncTask : this.asyncTasks) asyncTask.run();
    }

    public void runSyncTasks() {
        for(Runnable syncTask : this.syncTasks) syncTask.run();
    }

}
