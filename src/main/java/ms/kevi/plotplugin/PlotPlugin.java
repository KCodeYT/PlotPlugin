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

package ms.kevi.plotplugin;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.level.Level;
import cn.nukkit.level.generator.Generator;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import lombok.Getter;
import lombok.Setter;
import ms.kevi.plotplugin.command.PlotCommand;
import ms.kevi.plotplugin.generator.PlotGenerator;
import ms.kevi.plotplugin.lang.Language;
import ms.kevi.plotplugin.listener.PlotLevelRegistrationListener;
import ms.kevi.plotplugin.listener.PlotListener;
import ms.kevi.plotplugin.manager.PlayerManager;
import ms.kevi.plotplugin.manager.PlayerNameFunction;
import ms.kevi.plotplugin.manager.PlotManager;
import ms.kevi.plotplugin.util.BlockEntry;
import ms.kevi.plotplugin.util.PlotLevelRegistration;
import ms.kevi.plotplugin.util.PlotLevelSettings;
import ms.kevi.plotplugin.util.Utils;
import ms.kevi.plotplugin.util.async.TaskExecutor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
public class PlotPlugin extends PluginBase {

    private static final String DEFAULT_LANGUAGE = "en_US";
    public static PlotPlugin INSTANCE;

    private Config worldsConfig;
    private Config playersConfig;

    @Getter
    private Language language;

    @Getter
    private Map<String, PlotManager> plotManagerMap;
    private PlayerManager playerManager;

    @Getter
    private Level defaultPlotLevel;

    @Getter
    private Map<Player, PlotLevelRegistration> levelRegistrationMap;

    private boolean namesLoad;

    @Getter
    @Setter
    private PlayerNameFunction nameFunction;

    private final PlayerNameFunction defaultNameFunction = (name, nameConsumer) -> nameConsumer.accept(name.equalsIgnoreCase("admin") ? "§4Administrator" : name);

    @Getter
    private int plotsPerPage = 5;

    @Getter
    private boolean showCommandParams = true;

    @Getter
    private boolean addOtherCommands = true;

    @Getter
    private final List<BlockEntry> borderEntries = new ArrayList<>();

    @Getter
    private final List<BlockEntry> wallEntries = new ArrayList<>();

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
        final File[] files = langDir.listFiles();
        if(!langDir.exists() || files == null || files.length == 0) {
            if(!langDir.exists() && !langDir.mkdirs()) {
                this.getLogger().error("Could not create the language directory for this plugin!");
                return;
            }

            try(final InputStreamReader inputReader = new InputStreamReader(this.getResource("lang/lang_list.txt"));
                final BufferedReader bufferedReader = new BufferedReader(inputReader)) {
                String line;
                while((line = bufferedReader.readLine()) != null)
                    this.saveResource("lang/" + line + ".txt");
            } catch(Exception e) {
                this.getLogger().error("Could not find the language resources of this plugin!", e);
                return;
            }
        }

        this.saveResource("config.yml");
        final Config config = this.getConfig();

        if(!config.exists("default_lang")) {
            config.set("default_lang", DEFAULT_LANGUAGE);
            config.save();
        }

        if(!config.exists("plots_per_page")) {
            config.set("plots_per_page", this.plotsPerPage);
            config.save();
        }

        this.plotsPerPage = config.getInt("plots_per_page");

        if(!config.exists("show_command_params")) {
            config.set("show_command_params", this.showCommandParams);
            config.save();
        }

        this.showCommandParams = config.getBoolean("show_command_params");

        if(!config.exists("add_other_commands")) {
            config.set("add_other_commands", this.addOtherCommands);
            config.save();
        }

        this.addOtherCommands = config.getBoolean("add_other_commands");

        if(!config.exists("borders")) {
            final List<Map<String, Object>> defaultWalls = new ArrayList<>();
            defaultWalls.add(Utils.createMap(List.of("name", "image_type", "image_data"), List.of("reset_to_default", "PATH", "textures/ui/undoArrow")));
            defaultWalls.add(Utils.createMap(List.of("name", "block_id", "block_data", "image_type", "image_data"), List.of("Diamond", 57, 0, "URL", "https://static.wikia.nocookie.net/minecraft_gamepedia/images/c/c8/Block_of_Diamond_JE5_BE3.png")));
            defaultWalls.add(Utils.createMap(List.of("name", "block_id", "block_data", "image_type", "image_data", "permission"), List.of("Emerald", 133, 0, "plot.wall.emerald", "URL", "https://static.wikia.nocookie.net/minecraft_gamepedia/images/0/0b/Block_of_Emerald_JE4_BE3.png")));
            defaultWalls.add(Utils.createMap(List.of("name", "block_id", "block_data", "image_type", "image_data", "permission"), List.of("Gold", 41, 0, "plot.wall.gold", "URL", "https://static.wikia.nocookie.net/minecraft_gamepedia/images/7/72/Block_of_Gold_JE6_BE3.png")));

            config.set("borders", defaultWalls);
            config.save();
        }

        if(!config.exists("walls")) {
            final List<Map<String, Object>> defaultWalls = new ArrayList<>();
            defaultWalls.add(Utils.createMap(List.of("name", "image_type", "image_data"), List.of("reset_to_default", "PATH", "textures/ui/undoArrow")));
            defaultWalls.add(Utils.createMap(List.of("name", "block_id", "block_data", "image_type", "image_data"), List.of("Diamond", 57, 0, "URL", "https://static.wikia.nocookie.net/minecraft_gamepedia/images/c/c8/Block_of_Diamond_JE5_BE3.png")));
            defaultWalls.add(Utils.createMap(List.of("name", "block_id", "block_data", "image_type", "image_data", "permission"), List.of("Emerald", 133, 0, "plot.wall.emerald", "URL", "https://static.wikia.nocookie.net/minecraft_gamepedia/images/0/0b/Block_of_Emerald_JE4_BE3.png")));
            defaultWalls.add(Utils.createMap(List.of("name", "block_id", "block_data", "image_type", "image_data", "permission"), List.of("Gold", 41, 0, "plot.wall.gold", "URL", "https://static.wikia.nocookie.net/minecraft_gamepedia/images/7/72/Block_of_Gold_JE6_BE3.png")));

            config.set("walls", defaultWalls);
            config.save();
        }

        this.borderEntries.addAll(config.getMapList("borders").stream().map(BlockEntry::of).toList());
        this.wallEntries.addAll(config.getMapList("walls").stream().map(BlockEntry::of).toList());

        try {
            final String defaultLang = config.getString("default_lang");

            this.language = new Language(langDir, defaultLang);
            this.getLogger().info("This plugin is using the " + this.language.getDefaultLang() + " as default language file!");
        } catch(IOException e) {
            this.getLogger().error(e.getMessage(), e);
            return;
        }

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

                if(this.worldsConfig.getString("default", "").equalsIgnoreCase(levelName))
                    this.defaultPlotLevel = level;
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
            this.defaultPlotLevel = level;
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
        return this.getUniqueIdByName(playerName, true);
    }

    public UUID getUniqueIdByName(String playerName, boolean allowEveryone) {
        final String firstPlayerName = playerName.trim();
        if(allowEveryone && firstPlayerName.equals(Utils.STRING_EVERYONE))
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
