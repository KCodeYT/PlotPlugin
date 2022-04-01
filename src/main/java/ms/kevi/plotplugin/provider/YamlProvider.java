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

package ms.kevi.plotplugin.provider;

import cn.nukkit.utils.Config;
import ms.kevi.plotplugin.PlotPlugin;
import ms.kevi.plotplugin.manager.PlotManager;
import ms.kevi.plotplugin.schematic.Schematic;
import ms.kevi.plotplugin.util.Plot;
import ms.kevi.plotplugin.util.PlotId;
import ms.kevi.plotplugin.util.PlotLevelSettings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
public class YamlProvider implements Provider {

    private final PlotPlugin plugin;

    private final File schematicsFolder;
    private final File worldsFolder;

    private final Map<String, Config> configs = Collections.synchronizedMap(new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
    private final Map<String, PlotManager> managers = Collections.synchronizedMap(new TreeMap<>(String.CASE_INSENSITIVE_ORDER));

    public YamlProvider(PlotPlugin plugin) {
        this.plugin = plugin;
        this.schematicsFolder = new File(this.plugin.getDataFolder(), "schems");
        this.worldsFolder = new File(this.plugin.getDataFolder(), "worlds");
    }

    private Config getConfig(String levelName) {
        return this.configs.computeIfAbsent(levelName, s -> new Config(new File(this.worldsFolder, levelName + ".yml"), Config.YAML));
    }

    @Override
    public CompletableFuture<PlotManager> loadOrCreateManager(String levelName, PlotLevelSettings levelSettings) {
        return CompletableFuture.completedFuture(this.managers.computeIfAbsent(levelName, s -> {
            final Config config = this.getConfig(levelName);
            if(!config.exists("Settings")) {
                config.set("Settings", levelSettings.toMap());
                config.save();
            }

            levelSettings.fromMap(config.get("Settings", new HashMap<>()));
            return new PlotManager(this.plugin, levelName, levelSettings);
        }));
    }

    @Override
    public CompletableFuture<List<PlotManager>> getAllPlotManagers() {
        return CompletableFuture.completedFuture(new ArrayList<>(this.managers.values()));
    }

    @Override
    public CompletableFuture<Boolean> existSchematic(String name) {
        return CompletableFuture.completedFuture(new File(this.schematicsFolder, name + ".road").exists());
    }

    @Override
    public CompletableFuture<Schematic> loadSchematic(String name) {
        return this.existSchematic(name).thenCompose(exist -> {
            if(exist) {
                final Schematic schematic = new Schematic();
                try(final FileInputStream inputStream = new FileInputStream(new File(this.schematicsFolder, name + ".road"))) {
                    schematic.load(inputStream);
                    return CompletableFuture.completedFuture(schematic);
                } catch(IOException | DataFormatException e) {
                    throw new CompletionException(e);
                }
            } else return CompletableFuture.completedFuture(null);
        });
    }

    @Override
    public CompletableFuture<Boolean> saveSchematic(String name, Schematic schematic) {
        try(final FileOutputStream outputStream = new FileOutputStream(new File(this.schematicsFolder, name + ".road"))) {
            schematic.save(outputStream);
            return CompletableFuture.completedFuture(true);
        } catch(IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public CompletableFuture<Boolean> deleteSchematic(String name) {
        return CompletableFuture.completedFuture(new File(this.schematicsFolder, name + ".road").delete());
    }

    @Override
    public CompletableFuture<List<Plot>> getPlots(String levelName) {
        final PlotManager plotManager = this.managers.get(levelName);
        //noinspection unchecked
        final List<Map<String, Object>> plotsFromConfig = this.getConfig(levelName).getList("plots", new ArrayList<>());
        final List<Plot> plots = new ArrayList<>();
        for(Map<String, Object> plotMap : plotsFromConfig) {
            final String ownerString = (String) plotMap.getOrDefault("owner", "null");
            final Plot plot = new Plot(plotManager, Plot.getPlotVectorFromConfig(plotMap), ownerString.equals("null") ? null : UUID.fromString(ownerString));
            plot.getHelpers().addAll((this.<Collection<? extends String>>getOrDefault(plotMap.get("helpers"), new ArrayList<>())).stream().map(UUID::fromString).toList());
            plot.getDeniedPlayers().addAll((this.<Collection<? extends String>>getOrDefault(plotMap.get("denied"), new ArrayList<>())).stream().map(UUID::fromString).toList());
            plot.getConfig().putAll(this.<Map<String, Object>>getOrDefault(plotMap.get("config"), new HashMap<>()));
            for(int i = 0; i < plot.getMergedPlots().length; i++)
                plot.getMergedPlots()[i] = (Boolean) ((List<?>) plotMap.getOrDefault("merges", new ArrayList<>())).get(i);

            plots.add(plot);
        }

        return CompletableFuture.completedFuture(plots);
    }

    private <C> C getOrDefault(Object o, C defaultValue) {
        //noinspection unchecked
        return o == null ? defaultValue : (C) o;
    }

    @Override
    public CompletableFuture<Boolean> savePlots(String levelName, List<Plot> plots) {
        final Config config = this.getConfig(levelName);

        //noinspection unchecked
        final List<Map<String, Object>> plotsFromConfig = this.getConfig(levelName).getList("plots", new ArrayList<>());
        plotsFromConfig.removeIf(plotMap -> plots.stream().anyMatch(plot -> plot.getId().equals(Plot.getPlotVectorFromConfig(plotMap))));
        plotsFromConfig.addAll(plots.stream().map(plot -> {
            if(plot.isDefault()) return Collections.<String, Object>emptyMap();

            final Map<String, Object> plotMap = new HashMap<>();

            plotMap.put("x", plot.getId().getX());
            plotMap.put("z", plot.getId().getZ());
            plotMap.put("owner", plot.getOwner() == null ? "null" : plot.getOwner().toString());
            plotMap.put("helpers", plot.getHelpers().stream().map(UUID::toString).collect(Collectors.toList()));
            plotMap.put("denied", plot.getDeniedPlayers().stream().map(UUID::toString).collect(Collectors.toList()));
            plotMap.put("config", plot.getConfig());
            plotMap.put("merges", Arrays.stream(plot.getMergedPlots()).collect(Collectors.toList()));

            return plotMap;
        }).toList());

        plotsFromConfig.removeIf(Map::isEmpty);

        config.set("plots", plotsFromConfig);
        config.save();

        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Void> addPlot(String levelName, Plot plot) {
        final Config config = this.getConfig(levelName);

        //noinspection unchecked
        final List<Map<String, Object>> plotsFromConfig = this.getConfig(levelName).getList("plots", new ArrayList<>());
        plotsFromConfig.removeIf(plotMap -> plot.getId().equals(Plot.getPlotVectorFromConfig(plotMap)));
        plotsFromConfig.add(plot.getConfig());
        config.set("plots", plotsFromConfig);
        config.save();

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> removePlot(String levelName, Plot plot) {
        final Config config = this.getConfig(levelName);

        //noinspection unchecked
        final List<Map<String, Object>> plotsFromConfig = this.getConfig(levelName).getList("plots", new ArrayList<>());
        plotsFromConfig.removeIf(plotMap -> plot.getId().equals(Plot.getPlotVectorFromConfig(plotMap)));
        config.set("plots", plotsFromConfig);
        config.save();

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Plot> getPlot(String levelName, PlotId id) {
        final PlotManager plotManager = this.managers.get(levelName);

        //noinspection unchecked
        final List<Map<String, Object>> plotsFromConfig = this.getConfig(levelName).getList("plots", new ArrayList<>());
        for(Map<String, Object> plotMap : plotsFromConfig) {
            if(id.equals(Plot.getPlotVectorFromConfig(plotMap))) {
                final String ownerString = (String) plotMap.getOrDefault("owner", "null");
                final Plot plot = new Plot(plotManager, Plot.getPlotVectorFromConfig(plotMap), ownerString.equals("null") ? null : UUID.fromString(ownerString));
                plot.getHelpers().addAll((this.<Collection<? extends String>>getOrDefault(plotMap.get("helpers"), new ArrayList<>())).stream().map(UUID::fromString).toList());
                plot.getDeniedPlayers().addAll((this.<Collection<? extends String>>getOrDefault(plotMap.get("denied"), new ArrayList<>())).stream().map(UUID::fromString).toList());
                plot.getConfig().putAll(this.<Map<String, Object>>getOrDefault(plotMap.get("config"), new HashMap<>()));
                final List<?> merges = (List<?>) plotMap.getOrDefault("merges", new ArrayList<>());
                for(int i = 0; i < plot.getMergedPlots().length; i++)
                    plot.getMergedPlots()[i] = (Boolean) merges.get(i);

                return CompletableFuture.completedFuture(plot);
            }
        }

        return CompletableFuture.completedFuture(new Plot(plotManager, id, null));
    }

    @Override
    public CompletableFuture<Boolean> hasPlot(String levelName, PlotId id) {
        //noinspection unchecked
        return CompletableFuture.completedFuture(this.getConfig(levelName).getList("plots", new ArrayList<>()).stream().
                anyMatch(plotMap -> id.equals(Plot.getPlotVectorFromConfig((Map<String, Object>) plotMap))));
    }

    @Override
    public CompletableFuture<Plot> searchNextFreePlot(String levelName) {
        int i = 0;
        while(true) {
            for(int x = -i; x <= i; x++) {
                for(int z = -i; z <= i; z++) {
                    if((x != i && x != -i) && (z != i && z != -i)) continue;

                    final Plot plot;
                    if((plot = this.getPlot(levelName, PlotId.of(x, z)).join()) != null && !plot.hasOwner())
                        return CompletableFuture.completedFuture(plot);
                }
            }

            i++;
        }
    }

    @Override
    public CompletableFuture<List<Plot>> getPlotsByOwner(String levelName, UUID owner) {
        return this.getPlots(levelName).thenApply(plots -> plots.stream().filter(plot -> plot.getOwner().equals(owner)).collect(Collectors.toList()));
    }

}
