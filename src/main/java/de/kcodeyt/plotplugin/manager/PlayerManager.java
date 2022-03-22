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

package de.kcodeyt.plotplugin.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Kevims KCodeYT
 */
public class PlayerManager {

    private final Map<String, String> nameMap;
    private final List<String> names;

    public PlayerManager(List<String> names) {
        this.nameMap = new HashMap<>();
        this.names = new ArrayList<>(names);
    }

    public void load(PlayerNameFunction nameFunction) {
        for(String name : this.names)
            nameFunction.execute(name, correctName -> this.add(name, correctName));
    }

    public String get(String name) {
        return name == null ? "N/A" : this.nameMap.getOrDefault(name, name);
    }

    public void add(String name, String displayName) {
        synchronized(this.nameMap) {
            this.nameMap.put(name, displayName);
        }
    }

}
