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

package de.kcodeyt.plotplugin.util;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WhenDone {

    private final Runnable runnable;
    private boolean start = false;
    private int executeAt = 0;
    private boolean executed = false;
    private int runs = 0;

    public void addTask() {
        this.executeAt++;
    }

    public void done() {
        if(this.runs++ >= this.executeAt && !this.executed && this.start) {
            this.executed = true;
            this.runnable.run();
        }
    }

    public void start() {
        this.start = true;
        if(this.runs++ >= this.executeAt && !this.executed) {
            this.executed = true;
            this.runnable.run();
        }
    }

}
