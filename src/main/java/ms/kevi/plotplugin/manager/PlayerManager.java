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

package ms.kevi.plotplugin.manager;

import ms.kevi.plotplugin.PlotPlugin;

import java.util.*;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
public class PlayerManager {

    private final Map<UUID, String> players;
    private final Map<String, String> nameMap;

    public PlayerManager(PlotPlugin plugin) {
        this.players = plugin.getDatabase().getPlayers();
        this.nameMap = new HashMap<>();

        plugin.getDatabase().createPlayersTable();
    }

    public void load(PlayerNameFunction nameFunction) {
        for(String name : this.players.values())
            nameFunction.execute(name, displayName -> this.nameMap.put(name, displayName));
    }

    public boolean has(UUID uniqueId) {
        return this.players.containsKey(uniqueId);
    }

    public String get(String name) {
        return name == null ? "N/A" : this.nameMap.getOrDefault(name, name);
    }

    public String get(UUID uniqueId) {
        return this.players.get(uniqueId);
    }

    public void add(UUID uniqueId, String name, String displayName) {
        synchronized(this.nameMap) {
            this.nameMap.put(name, displayName);
        }

        this.players.put(uniqueId, name);
    }

    public Set<Map.Entry<UUID, String>> getPlayers() {
        return this.players.entrySet();
    }

    public Collection<String> getPlayerNames() {
        return this.players.values();
    }

}
