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

package de.kcodeyt.plotplugin;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.level.Level;
import cn.nukkit.level.generator.Generator;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import de.kcodeyt.plotplugin.command.PlotCommand;
import de.kcodeyt.plotplugin.generator.PlotGenerator;
import de.kcodeyt.plotplugin.listener.PlotLevelRegistrationListener;
import de.kcodeyt.plotplugin.listener.PlotListener;
import de.kcodeyt.plotplugin.manager.PlayerManager;
import de.kcodeyt.plotplugin.manager.PlayerNameFunction;
import de.kcodeyt.plotplugin.manager.PlotManager;
import de.kcodeyt.plotplugin.util.Language;
import de.kcodeyt.plotplugin.util.PlotLevelRegistration;
import de.kcodeyt.plotplugin.util.PlotLevelSettings;
import de.kcodeyt.plotplugin.util.Utils;
import de.kcodeyt.plotplugin.util.async.TaskExecutor;
import lombok.Getter;
import lombok.Setter;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class PlotPlugin extends PluginBase {

    public static PlotPlugin INSTANCE;

    private Config worldsConfig;
    private Config playersConfig;

    @Getter
    private Language language;

    @Getter
    private Map<String, PlotManager> plotManagerMap;
    private PlayerManager playerManager;

    @Getter
    private Level plotLevel;

    @Getter
    private Map<Player, PlotLevelRegistration> levelRegistrationMap;

    private boolean namesLoad;

    @Getter
    @Setter
    private PlayerNameFunction nameFunction;

    private final PlayerNameFunction defaultNameFunction = (name, nameConsumer) -> nameConsumer.accept(name.equalsIgnoreCase("admin") ? "§4Administrator" : name);

    @Override
    public void onLoad() {
        INSTANCE = this;
        Generator.addGenerator(PlotGenerator.class, "Plot", Generator.TYPE_FLAT);
    }

    @Override
    public void onEnable() {
        this.worldsConfig = new Config(new File(this.getDataFolder(), "worlds.yml"), Config.YAML);
        this.playersConfig = new Config(new File(this.getDataFolder(), "players.yml"), Config.YAML);

        final File langDir = new File(this.getDataFolder(), "lang");
        if(!langDir.exists()) {
            if(!langDir.mkdirs()) {
                this.getLogger().error("Could not create the language directory for this plugin!");
                return;
            }

            try(final InputStreamReader inputReader = new InputStreamReader(this.getResource("lang"));
                final BufferedReader bufferedReader = new BufferedReader(inputReader)) {
                String line;
                while((line = bufferedReader.readLine()) != null)
                    this.saveResource("lang/" + line);
            } catch(Exception e) {
                this.getLogger().error("Could not find the language resources of this plugin!", e);
                return;
            }
        }

        this.saveResource("config.yml");
        final Config config = this.getConfig();

        if(!config.exists("lang")) {
            config.set("lang", "de_DE"); // currently, using germany as default
            config.save();
        }

        final String language = config.getString("lang");
        File languageFile = new File(langDir, language + ".txt");

        if(!languageFile.exists()) {
            if(!language.equals("de_DE")) {
                languageFile = new File(langDir, "de_DE.txt");

                if(!languageFile.exists()) {
                    this.getLogger().error("Could not find the default language file!");
                    return;
                }
            } else {
                this.getLogger().error("Could not find the default language file!");
            }

            return;
        }

        this.language = new Language(languageFile);
        this.getLogger().info("This plugin is using the " + languageFile.getName() + " language file!");

        this.plotManagerMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.levelRegistrationMap = new HashMap<>();

        final Server server = this.getServer();

        for(Object o : this.worldsConfig.getList("levels", new ArrayList<>())) {
            if(o instanceof final String levelName) {
                final PlotManager plotManager = new PlotManager(this, levelName);
                this.plotManagerMap.put(levelName, plotManager);

                if(!server.isLevelGenerated(levelName))
                    server.generateLevel(levelName, ThreadLocalRandom.current().nextLong(), PlotGenerator.class);

                Level level;
                if((level = server.getLevelByName(levelName)) == null) {
                    server.loadLevel(levelName);
                    level = server.getLevelByName(levelName);
                }

                if(level == null) {
                    this.plotManagerMap.remove(levelName);
                    continue;
                }

                if(this.worldsConfig.getString("default", "").equalsIgnoreCase(levelName)) this.plotLevel = level;
                plotManager.initLevel(level);
            }
        }

        this.playerManager = new PlayerManager(this.playersConfig.getAll().keySet().stream().map(Object::toString).collect(Collectors.toList()));
        server.getScheduler().scheduleDelayedTask(this, this::loadPlayerNames, 1);

        server.getPluginManager().registerEvents(new PlotListener(this), this);
        server.getPluginManager().registerEvents(new PlotLevelRegistrationListener(this), this);

        server.getCommandMap().register("plot", new PlotCommand(this));

        server.getScheduler().scheduleDelayedTask(this, () -> {
            for(PlotManager plotManager : this.plotManagerMap.values()) {
                plotManager.savePlots();
            }
        }, 6000);
    }

    private void loadPlayerNames() {
        if(!this.namesLoad) {
            this.namesLoad = true;
            this.playerManager.load(this.nameFunction == null ? this.defaultNameFunction : this.nameFunction);
        }
    }

    @Override
    public void onDisable() {

    }

    public void catchException(Exception e) {
        this.getLogger().debug("Got exception!", e);
    }

    public void reloadPlots() {
        for(PlotManager plotManager : this.plotManagerMap.values())
            plotManager.reload();
    }

    public PlotManager getPlotManager(Level level) {
        if(level == null) return null;
        return this.getPlotManager(level.getFolderName());
    }

    public PlotManager getPlotManager(String levelName) {
        return this.plotManagerMap.getOrDefault(levelName, null);
    }

    public Level createLevel(String levelName, boolean defaultLevel, PlotLevelSettings levelSettings) {
        if(this.getServer().isLevelGenerated(levelName)) return null;

        final PlotManager plotManager = new PlotManager(this, levelName, levelSettings);
        this.plotManagerMap.put(levelName, plotManager);

        this.getServer().generateLevel(levelName, ThreadLocalRandom.current().nextLong(), PlotGenerator.class);
        final Level level = this.getServer().getLevelByName(levelName);

        if(level == null) return null;

        plotManager.initLevel(level);

        final List<String> levels = this.worldsConfig.get("levels", new ArrayList<>());
        levels.add(levelName);
        this.worldsConfig.set("levels", levels);

        if(defaultLevel) {
            this.worldsConfig.set("default", levelName);
            this.plotLevel = level;
        }

        this.worldsConfig.save(true);
        return level;
    }

    public void registerPlayer(Player player) {
        this.playersConfig.set(player.getName(), player.getUniqueId().toString());
        this.playersConfig.save(true);

        final String playerName = player.getName();
        TaskExecutor.executeAsync(() -> (this.nameFunction == null ? this.defaultNameFunction : this.nameFunction).execute(player.getName(), displayName -> this.playerManager.add(playerName, displayName)));
    }

    public UUID getUniqueIdByName(String playerName) {
        final String firstPlayerName = playerName.trim();
        if(firstPlayerName.equals(Utils.STRING_EVERYONE))
            return Utils.UUID_EVERYONE;

        for(Map.Entry<String, Object> entry : this.playersConfig.getAll().entrySet()) {
            if(entry.getKey().equalsIgnoreCase(firstPlayerName))
                return UUID.fromString((String) entry.getValue());
        }

        return null;
    }

    public String getCorrectName(UUID playerId) {
        return playerId == Utils.UUID_EVERYONE ? Utils.STRING_EVERYONE : this.playerManager.get(this.getNameByUniqueId(playerId));
    }

    public String findPlayerName(String playerName) {
        final String[] playerNames = this.playersConfig.getAll().keySet().toArray(new String[0]);
        for(String name : playerNames) {
            if(name.equalsIgnoreCase(playerName))
                return name;
        }

        for(String name : playerNames) {
            if(name.toLowerCase().startsWith(playerName.toLowerCase()))
                return name;
        }

        return playerName;
    }

    private String getNameByUniqueId(UUID uniqueId) {
        for(Map.Entry<String, Object> entry : this.playersConfig.getAll().entrySet()) {
            final String playerName = entry.getKey();
            if(UUID.fromString((String) entry.getValue()).equals(uniqueId)) return playerName;
        }

        return null;
    }

}
