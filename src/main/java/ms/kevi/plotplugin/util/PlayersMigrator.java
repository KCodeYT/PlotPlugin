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

import ms.kevi.plotplugin.PlotPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A class that migrates the players config into the database
 *
 * @author Kevims KCodeYT
 */
public class PlayersMigrator {

    public static boolean shouldMigrate(PlotPlugin plugin) {
        return new File(plugin.getDataFolder(), "players.yml").exists();
    }

    public static void migratePlayers(PlotPlugin plugin) {
        final File playersFile = new File(plugin.getDataFolder(), "players.yml");
        final File playersBackupFile = new File(plugin.getDataFolder(), "players.yml.bak");

        final Map<UUID, String> players = new HashMap<>();

        try(final BufferedReader bufferedReader = new BufferedReader(new FileReader(playersFile))) {
            String line;
            while((line = bufferedReader.readLine()) != null) {
                final String[] split = line.split(": ");

                final String name = split[0];
                final UUID uniqueId = UUID.fromString(split[1]);

                players.put(uniqueId, name);
            }
        } catch(IOException e) {
            throw new RuntimeException(e);
        }

        plugin.getDatabase().addPlayers(players);

        if(!playersFile.renameTo(playersBackupFile)) {
            throw new RuntimeException("Could not rename players.yml to players.yaml.bak!");
        }
    }

}
