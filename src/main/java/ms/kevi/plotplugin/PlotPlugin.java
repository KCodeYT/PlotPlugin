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
import cn.nukkit.level.DimensionEnum;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.LevelConfig;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.registry.RegisterException;
import cn.nukkit.registry.Registries;
import cn.nukkit.utils.Config;
import lombok.Getter;
import lombok.Setter;
import ms.kevi.plotplugin.command.PlotCommand;
import ms.kevi.plotplugin.database.Database;
import ms.kevi.plotplugin.generator.PlotGenerator;
import ms.kevi.plotplugin.generator.PlotStage;
import ms.kevi.plotplugin.lang.Language;
import ms.kevi.plotplugin.listener.PlotLevelRegistrationListener;
import ms.kevi.plotplugin.listener.PlotListener;
import ms.kevi.plotplugin.manager.PlayerManager;
import ms.kevi.plotplugin.manager.PlayerNameFunction;
import ms.kevi.plotplugin.manager.PlotManager;
import ms.kevi.plotplugin.util.*;
import ms.kevi.plotplugin.util.async.TaskExecutor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
public class PlotPlugin extends PluginBase {

    private static final String DEFAULT_LANGUAGE = "en_US";

    public static PlotPlugin INSTANCE;

    private Config worldsConfig;

    @Getter
    private Language language;

    @Getter
    private Database database;

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
        this.plotManagerMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        INSTANCE = this;
        try {
            Registries.GENERATE_STAGE.register(PlotStage.NAME, PlotStage.class);
            Registries.GENERATOR.register("plot", PlotGenerator.class);
        } catch (RegisterException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onEnable() {
        this.worldsConfig = new Config(new File(this.getDataFolder(), "worlds.yml"), Config.YAML);

        final File langDir = new File(this.getDataFolder(), "lang");
        final File[] files = langDir.listFiles();
        if (!langDir.exists() || files == null || files.length == 0) {
            if (!langDir.exists() && !langDir.mkdirs()) {
                this.getLogger().error("Could not create the language directory for this plugin!");
                return;
            }

            try (final InputStreamReader inputReader = new InputStreamReader(this.getResource("lang/lang_list.txt"));
                 final BufferedReader bufferedReader = new BufferedReader(inputReader)) {
                String line;
                while ((line = bufferedReader.readLine()) != null)
                    this.saveResource("lang/" + line + ".txt");
            } catch (Exception e) {
                this.getLogger().error("Could not find the language resources of this plugin!", e);
                return;
            }
        }

        this.saveResource("config.yml");
        final Config config = this.getConfig();
        boolean configNeedsToBeSaved = false;

        if (!config.exists("default_lang")) {
            config.set("default_lang", DEFAULT_LANGUAGE);
            configNeedsToBeSaved = true;
        }

        if (!config.exists("mysql")) {
            config.set("mysql.host", "127.0.0.1");
            config.set("mysql.port", 3306);
            config.set("mysql.parameters", Collections.emptyList());
            config.set("mysql.user", "root");
            config.set("mysql.password", "insert_your_password_here");
            config.set("mysql.database", "plot_plugin");
            configNeedsToBeSaved = true;
        }

        this.database = Database.builder().
                host(config.getString("mysql.host")).
                port(config.getInt("mysql.port")).
                parameters(config.getStringList("mysql.parameters")).
                user(config.getString("mysql.user")).
                password(config.getString("mysql.password")).
                database(config.getString("mysql.database")).
                build();

        if (!config.exists("plots_per_page")) {
            config.set("plots_per_page", this.plotsPerPage);
            configNeedsToBeSaved = true;
        }

        this.plotsPerPage = config.getInt("plots_per_page");

        if (!config.exists("show_command_params")) {
            config.set("show_command_params", this.showCommandParams);
            configNeedsToBeSaved = true;
        }

        this.showCommandParams = config.getBoolean("show_command_params");

        if (!config.exists("add_other_commands")) {
            config.set("add_other_commands", this.addOtherCommands);
            configNeedsToBeSaved = true;
        }

        this.addOtherCommands = config.getBoolean("add_other_commands");

        if (!config.exists("borders")) {
            final List<Map<String, Object>> defaultWalls = new ArrayList<>();
            defaultWalls.add(Utils.createMap(List.of("name", "image_type", "image_data"), List.of("reset_to_default", "PATH", "textures/ui/undoArrow")));
            defaultWalls.add(Utils.createMap(List.of("name", "block_id", "block_data", "image_type", "image_data"), List.of("Diamond", 57, 0, "URL", "https://static.wikia.nocookie.net/minecraft_gamepedia/images/c/c8/Block_of_Diamond_JE5_BE3.png")));
            defaultWalls.add(Utils.createMap(List.of("name", "block_id", "block_data", "image_type", "image_data", "permission"), List.of("Emerald", 133, 0, "plot.wall.emerald", "URL", "https://static.wikia.nocookie.net/minecraft_gamepedia/images/0/0b/Block_of_Emerald_JE4_BE3.png")));
            defaultWalls.add(Utils.createMap(List.of("name", "block_id", "block_data", "image_type", "image_data", "permission"), List.of("Gold", 41, 0, "plot.wall.gold", "URL", "https://static.wikia.nocookie.net/minecraft_gamepedia/images/7/72/Block_of_Gold_JE6_BE3.png")));

            config.set("borders", defaultWalls);
            configNeedsToBeSaved = true;
        }

        if (!config.exists("walls")) {
            final List<Map<String, Object>> defaultWalls = new ArrayList<>();
            defaultWalls.add(Utils.createMap(List.of("name", "image_type", "image_data"), List.of("reset_to_default", "PATH", "textures/ui/undoArrow")));
            defaultWalls.add(Utils.createMap(List.of("name", "block_id", "block_data", "image_type", "image_data"), List.of("Diamond", 57, 0, "URL", "https://static.wikia.nocookie.net/minecraft_gamepedia/images/c/c8/Block_of_Diamond_JE5_BE3.png")));
            defaultWalls.add(Utils.createMap(List.of("name", "block_id", "block_data", "image_type", "image_data", "permission"), List.of("Emerald", 133, 0, "plot.wall.emerald", "URL", "https://static.wikia.nocookie.net/minecraft_gamepedia/images/0/0b/Block_of_Emerald_JE4_BE3.png")));
            defaultWalls.add(Utils.createMap(List.of("name", "block_id", "block_data", "image_type", "image_data", "permission"), List.of("Gold", 41, 0, "plot.wall.gold", "URL", "https://static.wikia.nocookie.net/minecraft_gamepedia/images/7/72/Block_of_Gold_JE6_BE3.png")));

            config.set("walls", defaultWalls);
            configNeedsToBeSaved = true;
        }

        if (configNeedsToBeSaved) {
            config.save();
        }

        this.borderEntries.addAll(config.getMapList("borders").stream().map(BlockEntry::of).toList());
        this.wallEntries.addAll(config.getMapList("walls").stream().map(BlockEntry::of).toList());

        try {
            final String defaultLang = config.getString("default_lang");

            this.language = new Language(langDir, defaultLang);
            this.getLogger().info("This plugin is using the " + this.language.getDefaultLang() + " as default language file!");
        } catch (IOException e) {
            this.getLogger().error(e.getMessage(), e);
            return;
        }

        this.levelRegistrationMap = new HashMap<>();

        final Server server = this.getServer();

        try {
            if (config.getString("mysql.password").equalsIgnoreCase("insert_your_password_here")) {
                this.getLogger().info("This plugin has switched support from YAML to MySQL.");
                this.getLogger().info("Please insert your MySQL Connection information in the config.yml!");
                this.getLogger().info("All previous plots will be restored into this database, so you don't have to worry about losing them.");
                this.getLogger().info("Shutting down the server...");
                this.getServer().getScheduler().scheduleDelayedTask(this, server::shutdown, 1);
                return;
            }

            this.database.createDatabase();
        } catch (RuntimeException e) {
            this.getLogger().error("Could not connect to the database!", e.getCause());
            return;
        }

        for (Object o : this.worldsConfig.getList("levels", new ArrayList<>())) {
            if (o instanceof final String levelName) {
                final PlotManager plotManager = new PlotManager(this, levelName, true);
                this.plotManagerMap.put(levelName, plotManager);

                if (!server.isLevelLoaded(levelName)) {
                    LevelConfig.GeneratorConfig plot = new LevelConfig.GeneratorConfig("plot", ThreadLocalRandom.current().nextLong(),
                            DimensionEnum.OVERWORLD.getDimensionData(), Collections.emptyMap());
                    LevelConfig levelConfig = new LevelConfig("leveldb", Map.of(0, plot));
                    server.generateLevel(levelName, levelConfig);
                }

                Level level;
                if ((level = server.getLevelByName(levelName)) == null) {
                    server.loadLevel(levelName);
                    level = server.getLevelByName(levelName);
                }

                if (level == null) {
                    this.plotManagerMap.remove(levelName);
                    continue;
                }

                if (this.worldsConfig.getString("default", "").equalsIgnoreCase(levelName))
                    this.defaultPlotLevel = level;
                plotManager.initLevel(level);
            }
        }

        this.playerManager = new PlayerManager(this);
        if (PlayersMigrator.shouldMigrate(this)) {
            PlayersMigrator.migratePlayers(this);
        }

        server.getScheduler().scheduleDelayedTask(this, this::loadPlayerNames, 1);

        server.getPluginManager().registerEvents(new PlotListener(this), this);
        server.getPluginManager().registerEvents(new PlotLevelRegistrationListener(this), this);

        server.getCommandMap().register("plot", new PlotCommand(this));

        server.getScheduler().scheduleDelayedTask(this, () -> {
            for (PlotManager plotManager : this.plotManagerMap.values()) {
                plotManager.savePlots();
            }
        }, 6000);
    }

    private void loadPlayerNames() {
        if (!this.namesLoad) {
            this.namesLoad = true;
            this.playerManager.load(this.nameFunction == null ? this.defaultNameFunction : this.nameFunction);
        }
    }

    @Override
    public void onDisable() {

    }

    public PlotManager getPlotManager(Level level) {
        if (level == null) return null;
        return this.getPlotManager(new File(level.getFolderPath()).getName());
    }

    public PlotManager getPlotManager(String levelName) {
        return this.plotManagerMap.getOrDefault(levelName, null);
    }

    public Level createLevel(String levelName, boolean defaultLevel, PlotLevelSettings levelSettings) {
        if (this.getServer().isLevelLoaded(levelName)) this.getServer().getLevelByName(levelName);

        TaskExecutor.executeAsync(() -> this.database.createPlotsTable(levelName));

        final PlotManager plotManager = new PlotManager(this, levelName, levelSettings, false);
        this.plotManagerMap.put(levelName, plotManager);

        int dimension = levelSettings.getDimension();
        LevelConfig.GeneratorConfig plot = new LevelConfig.GeneratorConfig("plot", ThreadLocalRandom.current().nextLong(),
                DimensionEnum.getDataFromId(dimension), Collections.emptyMap());
        LevelConfig levelConfig = new LevelConfig("leveldb", Map.of(dimension, plot));
        this.getServer().generateLevel(levelName, levelConfig);
        final Level level = this.getServer().getLevelByName(levelName);

        if (level == null) return null;

        plotManager.initLevel(level);

        final List<String> levels = this.worldsConfig.get("levels", new ArrayList<>());
        levels.add(levelName);
        this.worldsConfig.set("levels", levels);

        if (defaultLevel) {
            this.worldsConfig.set("default", levelName);
            this.defaultPlotLevel = level;
        }

        this.worldsConfig.save(true);
        return level;
    }

    public void registerPlayer(Player player) {
        final String playerName = player.getName();

        TaskExecutor.executeAsync(() -> {
            if (!this.playerManager.has(player.getUniqueId())) {
                this.database.executeActions(Collections.singletonList(
                        this.database.addPlayer(player.getUniqueId(), playerName)
                ));
            }

            (this.nameFunction == null ? this.defaultNameFunction : this.nameFunction)
                    .execute(player.getName(), displayName -> this.playerManager.add(player.getUniqueId(), playerName, displayName));
        });
    }

    public UUID getUniqueIdByName(String playerName) {
        return this.getUniqueIdByName(playerName, true);
    }

    public UUID getUniqueIdByName(String playerName, boolean allowEveryone) {
        final String firstPlayerName = playerName.trim();
        if (allowEveryone && firstPlayerName.equals(Utils.STRING_EVERYONE))
            return Utils.UUID_EVERYONE;

        for (Map.Entry<UUID, String> entry : this.playerManager.getPlayers()) {
            if (entry.getValue().equalsIgnoreCase(firstPlayerName))
                return entry.getKey();
        }

        return null;
    }

    public String getCorrectName(UUID playerId) {
        return playerId == Utils.UUID_EVERYONE ? Utils.STRING_EVERYONE : this.playerManager.get(this.getNameByUniqueId(playerId));
    }

    public String findPlayerName(String playerName) {
        final Collection<String> playerNames = this.playerManager.getPlayerNames();
        for (String name : playerNames) {
            if (name.equalsIgnoreCase(playerName))
                return name;
        }

        for (String name : playerNames) {
            if (name.toLowerCase().startsWith(playerName.toLowerCase()))
                return name;
        }

        return playerName;
    }

    private String getNameByUniqueId(UUID uniqueId) {
        return this.playerManager.get(uniqueId);
    }

}
