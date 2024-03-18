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

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockAir;
import cn.nukkit.block.BlockState;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.entity.Entity;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.level.format.IChunk;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.BlockVector3;
import cn.nukkit.math.SimpleAxisAlignedBB;
import cn.nukkit.math.Vector3;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import ms.kevi.plotplugin.PlotPlugin;
import ms.kevi.plotplugin.database.Database;
import ms.kevi.plotplugin.event.PlotClearEvent;
import ms.kevi.plotplugin.generator.PlotGenerator;
import ms.kevi.plotplugin.util.*;
import ms.kevi.plotplugin.util.async.AsyncLevelWorker;
import ms.kevi.plotplugin.util.async.TaskExecutor;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
public class PlotManager {

    private final PlotPlugin plugin;
    @Getter
    private final String levelName;
    @Getter
    private final PlotSchematic plotSchematic;
    @Getter
    private final File plotSchematicFile;

    @Getter
    private final PlotLevelSettings levelSettings;

    private final Map<PlotId, Plot> plots;

    @Getter
    private Level level;
    private PlotGenerator plotGenerator;

    public PlotManager(PlotPlugin plugin, String levelName, boolean loadPlots) {
        this(plugin, levelName, new PlotLevelSettings(), loadPlots);
    }

    public PlotManager(PlotPlugin plugin, String levelName, PlotLevelSettings levelSettings, boolean loadPlots) {
        this.plugin = plugin;
        this.levelName = levelName;
        this.plotSchematic = new PlotSchematic(this);
        this.plotSchematic.init(this.plotSchematicFile = new File(this.plugin.getDataFolder(), "schems/" + levelName + ".road"));

        final File settingsFile = new File(plugin.getDataFolder(), "worlds/" + levelName + ".yml");
        if (settingsFile.exists()) RewriteUtil.rewriteIfOld(plugin, settingsFile, levelName);
        else {
            new Config(settingsFile, Config.YAML, new ConfigSection(levelSettings.toMap())).save();
        }
        final Config settingsConfig = new Config(settingsFile, Config.YAML);
        this.plots = new ConcurrentHashMap<>();

        if (loadPlots) {
            for (Plot plot : this.plugin.getDatabase().getPlots(this))
                this.plots.put(plot.getId(), plot);

            for (Plot plot : this.plots.values())
                plot.recalculateOrigin();
        }
        this.levelSettings = levelSettings;
        this.levelSettings.fromMap(settingsConfig.getAll());
    }

    public void initLevel(Level level) {
        this.level = level;
        this.plotGenerator = (PlotGenerator) level.getGenerator();
    }

    public void savePlot(Plot plot) {
        this.savePlots(this.getConnectedPlots(plot));
    }

    public void savePlots(Plot... plots) {
        this.savePlots(Arrays.asList(plots));
    }

    public void savePlots(Collection<Plot> plots) {
        final List<Database.DatabaseAction> databaseActions = new ArrayList<>();
        final Database database = this.plugin.getDatabase();

        final Collection<Plot> plotsToSave = plots.isEmpty() ? this.plots.values() : plots;
        for (Plot plot : plotsToSave)
            databaseActions.add(database.updatePlot(plot));

        TaskExecutor.executeAsync(() -> database.executeActions(databaseActions));
    }

    public void removePlot(Plot plot) {
        this.plots.remove(plot.getId());

        TaskExecutor.executeAsync(() -> {
            final Database database = this.plugin.getDatabase();
            database.executeActions(Collections.singletonList(database.deletePlot(plot)));
        });
    }

    public Plot getMergedPlot(int x, int z) {
        final int plotSize = this.levelSettings.getPlotSize();
        final int totalSize = this.levelSettings.getTotalSize();

        final int idX = x >= 0 ? x / totalSize : ((x + 1) / totalSize) - 1;
        final int idZ = z >= 0 ? z / totalSize : ((z + 1) / totalSize) - 1;

        final int difX = x >= 0 ? (x + 1) % totalSize : totalSize + ((x + 1) % totalSize);
        final int difZ = z >= 0 ? (z + 1) % totalSize : totalSize + ((z + 1) % totalSize);

        final boolean xOnRoad = difX > plotSize || difX == 0;
        final boolean zOnRoad = difZ > plotSize || difZ == 0;

        final Plot plot = this.getPlotById(PlotId.of(idX, idZ));
        if (xOnRoad && zOnRoad) {
            if (plot.isMerged(Plot.DIRECTION_SOUTH_EAST)) return plot;
            return null;
        } else if (xOnRoad) {
            if (plot.isMerged(Plot.DIRECTION_EAST)) return plot;
            return null;
        } else if (zOnRoad) {
            if (plot.isMerged(Plot.DIRECTION_SOUTH)) return plot;
            return null;
        } else {
            return plot;
        }
    }

    public Plot getPlot(int x, int z) {
        final PlotId plotId = this.getPlotIdByPos(x, z);
        if (plotId == null) return null;

        return this.getPlotById(plotId.getX(), plotId.getZ());
    }

    private PlotId getPlotIdByPos(int x, int z) {
        final int plotSize = this.levelSettings.getPlotSize();
        final int totalSize = plotSize + this.levelSettings.getRoadSize();

        final int idX = x >= 0 ? x / totalSize : ((x + 1) / totalSize) - 1;
        final int idZ = z >= 0 ? z / totalSize : ((z + 1) / totalSize) - 1;

        final int difX = x >= 0 ? (x + 1) % totalSize : totalSize + ((x + 1) % totalSize);
        final int difZ = z >= 0 ? (z + 1) % totalSize : totalSize + ((z + 1) % totalSize);

        final boolean xOnRoad = difX > plotSize || difX == 0;
        final boolean zOnRoad = difZ > plotSize || difZ == 0;

        if (xOnRoad || zOnRoad) return null;
        return PlotId.of(idX, idZ);
    }

    public Plot getPlotById(PlotId plotId) {
        return this.plots.computeIfAbsent(plotId, id -> new Plot(this, id, null));
    }

    public Plot getPlotById(int plotX, int plotZ) {
        return this.plots.computeIfAbsent(PlotId.of(plotX, plotZ), id -> new Plot(this, id, null));
    }

    private Vector3 getPosByPlot(Plot plot) {
        return this.getPosByPlotId(plot.getId());
    }

    private Vector3 getPosByPlotId(PlotId plotId) {
        final int totalSize = this.levelSettings.getTotalSize();
        final int minY = LevelUtils.getChunkMinY(this.levelSettings.getDimension());
        return new Vector3(
                totalSize * plotId.getX(),
                minY + this.levelSettings.getGroundHeight() + 1,
                totalSize * plotId.getZ()
        );
    }

    public Plot getNextFreePlot() {
        int i = 0;
        while (true) {
            for (int x = -i; x <= i; x++) {
                for (int z = -i; z <= i; z++) {
                    if ((x != i && x != -i) && (z != i && z != -i)) continue;

                    final Plot plot;
                    if ((plot = this.getPlotById(x, z)) != null && !plot.hasOwner())
                        return plot;
                }
            }

            i++;
        }
    }

    public List<Plot> getPlotsByOwner(UUID ownerId) {
        final List<Plot> plots = new ArrayList<>();
        for (Plot plot : this.plots.values())
            if (plot.isOwner(ownerId))
                plots.add(plot);
        return plots;
    }

    public Set<Plot> getConnectedPlots(Plot plot) {
        if (plot.hasNoMerges()) return Collections.singleton(plot);

        final Set<Plot> tmpSet = new HashSet<>();
        final Queue<Plot> frontier = new ArrayDeque<>();
        final Set<Object> queueCache = new HashSet<>();
        tmpSet.add(plot);
        Plot tmp;
        for (int iDir = 0; iDir < 4; iDir++) {
            if (plot.isMerged(iDir)) {
                tmp = this.getPlotById(plot.getRelative(iDir));
                if (!tmp.isMerged(Plot.DIRECTION_OPPOSITES[iDir]))
                    tmp.setMerged(Plot.DIRECTION_OPPOSITES[iDir], true);
                queueCache.add(tmp);
                frontier.add(tmp);
            }
        }

        Plot current;
        while ((current = frontier.poll()) != null) {
            tmpSet.add(current);
            queueCache.remove(current);
            for (int iDir = 0; iDir < 4; iDir++) {
                if (current.isMerged(iDir)) {
                    tmp = this.getPlotById(current.getRelative(iDir));
                    if (tmp != null && !queueCache.contains(tmp) && !tmpSet.contains(tmp)) {
                        queueCache.add(tmp);
                        frontier.add(tmp);
                    }
                }
            }
        }

        return tmpSet;
    }

    public Set<Plot> calculatePlotsToMerge(Plot plot, int dir) {
        final Set<Plot> plots = new LinkedHashSet<>();
        plots.addAll(this.getConnectedPlots(plot));
        plots.addAll(this.getConnectedPlots(this.getPlotById(plot.getRelative(dir))));

        return plots;
    }

    public boolean startMerge(Plot plot, Set<Plot> plots) {
        final WhenDone whenDone = new WhenDone(() -> {
            this.finishPlotMerge(plots);

            for (Plot other : plots) {
                other.recalculateOrigin();
                if (!other.equals(plot)) this.mergePlotData(plot, other);
            }

            this.savePlots(plots);
        });

        int relativeDir;
        for (Plot toMerge0 : plots) {
            for (Plot toMerge1 : plots) {
                if (toMerge0.equals(toMerge1)) continue;

                relativeDir = toMerge0.getRelativeDir(toMerge1.getId());
                if (relativeDir != -1 && !toMerge0.isMerged(relativeDir))
                    this.mergePlot(toMerge0, toMerge1, whenDone);

                relativeDir = toMerge1.getRelativeDir(toMerge0.getId());
                if (relativeDir != -1 && !toMerge1.isMerged(relativeDir))
                    this.mergePlot(toMerge1, toMerge0, whenDone);
            }
        }

        whenDone.start();
        return true;
    }

    private void mergePlotData(Plot plotA, Plot plotB) {
        plotA.getHelpers().addAll(plotB.getHelpers().stream().filter(helperId -> !plotA.isHelper(helperId)).toList());
        plotB.getHelpers().clear();
        plotB.getHelpers().addAll(plotA.getHelpers());

        plotA.getDeniedPlayers().addAll(plotB.getDeniedPlayers().stream().filter(deniedId -> !plotA.isDenied(deniedId)).toList());
        plotB.getDeniedPlayers().clear();
        plotB.getDeniedPlayers().addAll(plotA.getDeniedPlayers());

        final Map<String, Object> plotAConfig = plotA.getConfig();
        final Map<String, Object> plotBConfig = plotB.getConfig();
        if ((!plotAConfig.isEmpty() || !plotBConfig.isEmpty()) && !plotAConfig.equals(plotBConfig)) {
            final boolean greater = plotAConfig.size() > plotBConfig.size();

            if (greater) plotAConfig.putAll(plotBConfig);
            else plotBConfig.putAll(plotAConfig);

            final Map<String, Object> config = greater ? new HashMap<>(plotAConfig) : new HashMap<>(plotBConfig);
            plotAConfig.clear();
            plotAConfig.putAll(config);
            plotBConfig.clear();
            plotBConfig.putAll(config);
        }

        if (plotA.getHomePosition() != null) plotB.setHomePosition(plotA.getHomePosition());
        if (plotB.getHomePosition() != null) plotA.setHomePosition(plotB.getHomePosition());
    }

    private void finishPlotMerge(Set<Plot> plots) {
        final BlockState claimBlock = this.levelSettings.getClaimPlotState();
        final BlockState wallBlock = this.levelSettings.getWallPlotState();
        final BlockState wallFillingBlock = this.levelSettings.getWallFillingState();

        for (Plot plot : plots) {
            this.changeBorder(plot, plot.hasOwner() ? claimBlock : wallBlock);
            this.changeWall(plot, wallFillingBlock);
        }
    }

    private void mergePlot(Plot lesserPlot, Plot greaterPlot, WhenDone whenDone) {
        if (lesserPlot.getId().getX() == greaterPlot.getId().getX()) {
            if (lesserPlot.getId().getZ() > greaterPlot.getId().getZ()) {
                final Plot tmp = lesserPlot;
                lesserPlot = greaterPlot;
                greaterPlot = tmp;
            }

            if (!lesserPlot.isMerged(Plot.DIRECTION_SOUTH)) {
                lesserPlot.setMerged(Plot.DIRECTION_SOUTH, true);
                greaterPlot.setMerged(Plot.DIRECTION_NORTH, true);

                this.removeRoadSouth(lesserPlot, whenDone);
                final Plot diagonal = this.getPlotById(greaterPlot.getRelative(Plot.DIRECTION_EAST));
                if (diagonal.isMerged(Plot.DIRECTION_NORTH_WEST))
                    this.removeRoadSouthEast(lesserPlot, whenDone);
                final Plot below = this.getPlotById(greaterPlot.getRelative(Plot.DIRECTION_WEST));
                if (below.isMerged(Plot.DIRECTION_NORTH_EAST))
                    this.removeRoadSouthEast(this.getPlotById(below.getRelative(Plot.DIRECTION_NORTH)), whenDone);
            }
        } else {
            if (lesserPlot.getId().getX() > greaterPlot.getId().getX()) {
                Plot tmp = lesserPlot;
                lesserPlot = greaterPlot;
                greaterPlot = tmp;
            }

            if (!lesserPlot.isMerged(Plot.DIRECTION_EAST)) {
                lesserPlot.setMerged(Plot.DIRECTION_EAST, true);
                greaterPlot.setMerged(Plot.DIRECTION_WEST, true);

                final Plot diagonal = this.getPlotById(greaterPlot.getRelative(Plot.DIRECTION_SOUTH));
                if (diagonal.isMerged(Plot.DIRECTION_NORTH_WEST))
                    this.removeRoadSouthEast(lesserPlot, whenDone);
                this.removeRoadEast(lesserPlot, whenDone);
                final Plot below = this.getPlotById(greaterPlot.getRelative(Plot.DIRECTION_NORTH));
                if (below.isMerged(Plot.DIRECTION_SOUTH_WEST))
                    this.removeRoadSouthEast(this.getPlotById(below.getRelative(Plot.DIRECTION_WEST)), whenDone);
            }
        }
    }

    public void unlinkPlotFromNeighbors(Plot centerPlot) {
        if (centerPlot.hasNoMerges()) return;

        final WhenDone whenDone = new WhenDone(() -> this.finishPlotUnlinkFromNeighbors(centerPlot));

        final Int2ObjectMap<int[]> plotsToUnlink = new Int2ObjectOpenHashMap<>();
        plotsToUnlink.put(Plot.DIRECTION_SELF, new int[]{Plot.DIRECTION_EAST, Plot.DIRECTION_SOUTH, Plot.DIRECTION_SOUTH_EAST});
        plotsToUnlink.put(Plot.DIRECTION_NORTH, new int[]{Plot.DIRECTION_SOUTH, Plot.DIRECTION_SOUTH_EAST});
        plotsToUnlink.put(Plot.DIRECTION_WEST, new int[]{Plot.DIRECTION_EAST, Plot.DIRECTION_SOUTH_EAST});
        plotsToUnlink.put(Plot.DIRECTION_NORTH_WEST, new int[]{Plot.DIRECTION_SOUTH_EAST});

        for (Int2ObjectMap.Entry<int[]> entry : plotsToUnlink.int2ObjectEntrySet()) {
            final Plot plot = this.getPlotById(centerPlot.getRelative(entry.getIntKey()));

            for (int unlinkDir : entry.getValue()) {
                if (!plot.isMerged(unlinkDir)) continue;

                switch (unlinkDir) {
                    case Plot.DIRECTION_EAST -> this.createRoadEast(plot, whenDone);
                    case Plot.DIRECTION_SOUTH -> this.createRoadSouth(plot, whenDone);
                    case Plot.DIRECTION_SOUTH_EAST -> this.createRoadSouthEast(plot, whenDone);
                }
            }
        }

        whenDone.start();
    }

    public void unlinkPlotFromAll(Plot centerPlot) {
        if (centerPlot.hasNoMerges()) return;

        final Set<Plot> plots = this.getConnectedPlots(centerPlot);
        final WhenDone whenDone = new WhenDone(() -> this.finishPlotUnlinkFromAll(plots));

        for (Plot current : plots) {
            if (current.isMerged(Plot.DIRECTION_EAST)) {
                this.createRoadEast(current, whenDone);
                if (current.isMerged(Plot.DIRECTION_SOUTH)) {
                    this.createRoadSouth(current, whenDone);
                    if (current.isMerged(Plot.DIRECTION_SOUTH_EAST))
                        this.createRoadSouthEast(current, whenDone);
                }
            } else if (current.isMerged(Plot.DIRECTION_SOUTH))
                this.createRoadSouth(current, whenDone);
        }

        whenDone.start();
    }

    private void finishPlotUnlinkFromNeighbors(Plot centerPlot) {
        final BlockState claimBlock = this.levelSettings.getClaimPlotState();
        final BlockState wallBlock = this.levelSettings.getWallPlotState();
        final BlockState wallFillingBlock = this.levelSettings.getWallFillingState();

        final Set<Plot> plots = new HashSet<>(Collections.singleton(centerPlot));
        {
            for (int iDir = 0; iDir < 4; iDir++) {
                final Plot plot = this.getPlotById(centerPlot.getRelative(iDir));
                if (centerPlot.isMerged(iDir) && plot.isMerged(Plot.DIRECTION_OPPOSITES[iDir])) {
                    centerPlot.setMerged(iDir, false);
                    plot.setMerged(Plot.DIRECTION_OPPOSITES[iDir], false);

                    plots.add(plot);
                }
            }
        }

        for (Plot plot : plots) {
            plot.recalculateOrigin();
            this.changeBorder(plot, plot.hasOwner() ? claimBlock : wallBlock);
            this.changeWall(plot, wallFillingBlock);
            this.clearWallAbove(plot);
            this.savePlots();
        }
    }

    private void finishPlotUnlinkFromAll(Set<Plot> plots) {
        final BlockState claimBlock = this.levelSettings.getClaimPlotState();
        final BlockState wallBlock = this.levelSettings.getWallPlotState();
        final BlockState wallFillingBlock = this.levelSettings.getWallFillingState();

        for (Plot plot : plots) {
            for (int iDir = 0; iDir < 4; iDir++)
                plot.setMerged(iDir, false);
            this.changeBorder(plot, plot.hasOwner() ? claimBlock : wallBlock);
            this.changeWall(plot, wallFillingBlock);
            this.clearWallAbove(plot);
            plot.recalculateOrigin();
            this.savePlots();
        }
    }

    private Set<IChunk> pasteRoadSchematic(int minX, int minZ, int maxX, int maxZ) {
        final Set<IChunk> visited = new HashSet<>();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                final IChunk IChunk = this.level.getChunk(x >> 4, z >> 4);
                if (visited.contains(IChunk)) continue;

                visited.add(IChunk);
                this.plotGenerator.regenerateChunkWithin(this, IChunk, minX, minZ, maxX, maxZ);
            }
        }

        return visited;
    }

    private void removeRoadEast(Plot plot, WhenDone whenDone) {
        final int groundHeight = this.levelSettings.getGroundHeight();
        final int roadSize = this.levelSettings.getRoadSize();
        final int minY = LevelUtils.getChunkMinY(this.levelSettings.getDimension());
        final int maxY = LevelUtils.getChunkMaxY(this.levelSettings.getDimension());

        final BlockVector3 pos1 = this.getBottomPlotPos(plot);
        final BlockVector3 pos2 = this.getTopPlotPos(plot);

        final int minX = pos2.getX() + 1;
        final int minZ = pos1.getZ();
        final int maxX = minX + roadSize - 1;
        final int maxZ = pos2.getZ();

        final AxisAlignedBB bb = new SimpleAxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);

        for (Entity entity : this.level.getCollidingEntities(bb))
            if (!(entity instanceof Player)) entity.close();

        for (Block block : this.level.getCollisionBlocks(bb, false, true)) {
            final BlockEntity blockEntity = block.getLevelBlockEntity();
            if (blockEntity != null) blockEntity.close();
        }

        final AsyncLevelWorker asyncLevelWorker = new AsyncLevelWorker(this.level);
        asyncLevelWorker.queueFill(
                new BlockVector3(minX, minY + 1, minZ),
                new BlockVector3(maxX, minY + groundHeight - 1, maxZ),
                this.levelSettings.getMiddleLayerState()
        );
        asyncLevelWorker.queueFill(
                new BlockVector3(minX, minY + groundHeight, minZ),
                new BlockVector3(maxX, minY + groundHeight, maxZ),
                this.levelSettings.getLastLayerState()
        );
        asyncLevelWorker.queueFill(
                new BlockVector3(minX, minY + groundHeight + 1, minZ),
                new BlockVector3(maxX, maxY, maxZ),
                BlockAir.STATE
        );
        asyncLevelWorker.runQueue(whenDone);
    }

    private void removeRoadSouth(Plot plot, WhenDone whenDone) {
        final int groundHeight = this.levelSettings.getGroundHeight();
        final int roadSize = this.levelSettings.getRoadSize();
        final int minY = LevelUtils.getChunkMinY(this.levelSettings.getDimension());
        final int maxY = LevelUtils.getChunkMaxY(this.levelSettings.getDimension());

        final BlockVector3 pos1 = this.getBottomPlotPos(plot);
        final BlockVector3 pos2 = this.getTopPlotPos(plot);

        final int minX = pos1.getX();
        final int minZ = pos2.getZ() + 1;
        final int maxX = pos2.getX();
        final int maxZ = minZ + roadSize - 1;

        final AxisAlignedBB bb = new SimpleAxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);

        for (Entity entity : this.level.getCollidingEntities(bb))
            if (!(entity instanceof Player)) entity.close();

        for (Block block : this.level.getCollisionBlocks(bb, false, true)) {
            final BlockEntity blockEntity = block.getLevelBlockEntity();
            if (blockEntity != null) blockEntity.close();
        }

        final AsyncLevelWorker asyncLevelWorker = new AsyncLevelWorker(this.level);
        asyncLevelWorker.queueFill(
                new BlockVector3(minX, minY + 1, minZ),
                new BlockVector3(maxX, minY + groundHeight - 1, maxZ),
                this.levelSettings.getMiddleLayerState()
        );
        asyncLevelWorker.queueFill(
                new BlockVector3(minX, minY + groundHeight, minZ),
                new BlockVector3(maxX, minY + groundHeight, maxZ),
                this.levelSettings.getLastLayerState()
        );
        asyncLevelWorker.queueFill(
                new BlockVector3(minX, minY + groundHeight + 1, minZ),
                new BlockVector3(maxX, maxY, maxZ),
                BlockAir.STATE
        );
        asyncLevelWorker.runQueue(whenDone);
    }

    private void removeRoadSouthEast(Plot plot, WhenDone whenDone) {
        final int groundHeight = this.levelSettings.getGroundHeight();
        final int roadSize = this.levelSettings.getRoadSize();
        final int minY = LevelUtils.getChunkMinY(this.levelSettings.getDimension());
        final int maxY = LevelUtils.getChunkMaxY(this.levelSettings.getDimension());

        final BlockVector3 pos = this.getTopPlotPos(plot);

        final int minX = pos.getX() + 1;
        final int minZ = pos.getZ() + 1;
        final int maxX = minX + roadSize - 1;
        final int maxZ = minZ + roadSize - 1;

        final AxisAlignedBB bb = new SimpleAxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);

        for (Entity entity : this.level.getCollidingEntities(bb))
            if (!(entity instanceof Player)) entity.close();

        for (Block block : this.level.getCollisionBlocks(bb, false, true)) {
            final BlockEntity blockEntity = block.getLevelBlockEntity();
            if (blockEntity != null) blockEntity.close();
        }

        final AsyncLevelWorker asyncLevelWorker = new AsyncLevelWorker(this.level);
        asyncLevelWorker.queueFill(
                new BlockVector3(minX, minY + 1, minZ),
                new BlockVector3(maxX, minY + groundHeight - 1, maxZ),
                this.levelSettings.getMiddleLayerState()
        );
        asyncLevelWorker.queueFill(
                new BlockVector3(minX, minY + groundHeight, minZ),
                new BlockVector3(maxX, minY + groundHeight, maxZ),
                this.levelSettings.getLastLayerState()
        );
        asyncLevelWorker.queueFill(
                new BlockVector3(minX, minY + groundHeight + 1, minZ),
                new BlockVector3(maxX, maxY, maxZ),
                BlockAir.STATE
        );
        asyncLevelWorker.runQueue(whenDone);
    }

    private void createRoadEast(Plot plot, WhenDone whenDone) {
        final int groundHeight = this.levelSettings.getGroundHeight();
        final int roadSize = this.levelSettings.getRoadSize();
        final int minY = LevelUtils.getChunkMinY(this.levelSettings.getDimension());
        final int maxY = LevelUtils.getChunkMaxY(this.levelSettings.getDimension());

        final BlockVector3 pos1 = this.getBottomPlotPos(plot);
        final BlockVector3 pos2 = this.getTopPlotPos(plot);

        final int minX = pos2.getX() + 2;
        final int minZ = pos1.getZ() - 1;
        final int maxX = minX + roadSize - 2;
        final int maxZ = pos2.getZ() + 1;

        final AxisAlignedBB bb = new SimpleAxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);

        for (Entity entity : this.level.getCollidingEntities(bb))
            if (!(entity instanceof Player)) entity.close();

        for (Block block : this.level.getCollisionBlocks(bb, false, true)) {
            final BlockEntity blockEntity = block.getLevelBlockEntity();
            if (blockEntity != null) blockEntity.close();
        }

        final AsyncLevelWorker asyncLevelWorker = new AsyncLevelWorker(this.level);
        asyncLevelWorker.queueFill(
                new BlockVector3(minX, minY, minZ),
                new BlockVector3(maxX, minY, maxZ),
                this.levelSettings.getFirstLayerState()
        );
        asyncLevelWorker.queueFill(
                new BlockVector3(minX, minY + 1, minZ),
                new BlockVector3(maxX, minY + groundHeight, maxZ),
                this.levelSettings.getWallFillingState()
        );
        asyncLevelWorker.queueFill(
                new BlockVector3(minX, minY + 1, minZ),
                new BlockVector3(maxX, minY + groundHeight - 1, maxZ),
                this.levelSettings.getRoadFillingState()
        );
        asyncLevelWorker.queueFill(
                new BlockVector3(minX, minY + groundHeight, minZ),
                new BlockVector3(maxX, minY + groundHeight, maxZ),
                this.levelSettings.getRoadState()
        );
        asyncLevelWorker.queueFill(
                new BlockVector3(minX, minY + groundHeight + 1, minZ),
                new BlockVector3(maxX, maxY, maxZ),
                BlockAir.STATE
        );

        if (this.plotSchematic.getSchematic() != null)
            asyncLevelWorker.addTask(() -> this.pasteRoadSchematic(minX, minZ, maxX, maxZ));

        asyncLevelWorker.runQueue(whenDone);
    }

    private void createRoadSouth(Plot plot, WhenDone whenDone) {
        final int groundHeight = this.levelSettings.getGroundHeight();
        final int roadSize = this.levelSettings.getRoadSize();
        final int minY = LevelUtils.getChunkMinY(this.levelSettings.getDimension());
        final int maxY = LevelUtils.getChunkMaxY(this.levelSettings.getDimension());

        final BlockVector3 pos1 = this.getBottomPlotPos(plot);
        final BlockVector3 pos2 = this.getTopPlotPos(plot);

        final int minX = pos1.getX() - 1;
        final int minZ = pos2.getZ() + 2;
        final int maxX = pos2.getX() + 1;
        final int maxZ = minZ + roadSize - 2;

        final AxisAlignedBB bb = new SimpleAxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);

        for (Entity entity : this.level.getCollidingEntities(bb))
            if (!(entity instanceof Player)) entity.close();

        for (Block block : this.level.getCollisionBlocks(bb, false, true)) {
            final BlockEntity blockEntity = block.getLevelBlockEntity();
            if (blockEntity != null) blockEntity.close();
        }

        final AsyncLevelWorker asyncLevelWorker = new AsyncLevelWorker(this.level);
        asyncLevelWorker.queueFill(
                new BlockVector3(minX, minY, minZ),
                new BlockVector3(maxX, minY, maxZ),
                this.levelSettings.getFirstLayerState()
        );
        asyncLevelWorker.queueFill(
                new BlockVector3(minX, minY + 1, minZ),
                new BlockVector3(maxX, minY + groundHeight, maxZ),
                this.levelSettings.getWallFillingState()
        );
        asyncLevelWorker.queueFill(
                new BlockVector3(minX, minY + 1, minZ + 1),
                new BlockVector3(maxX, minY + groundHeight - 1, maxZ),
                this.levelSettings.getRoadFillingState()
        );
        asyncLevelWorker.queueFill(
                new BlockVector3(minX, minY + groundHeight, minZ),
                new BlockVector3(maxX, minY + groundHeight, maxZ),
                this.levelSettings.getRoadState()
        );
        asyncLevelWorker.queueFill(
                new BlockVector3(minX, minY + groundHeight + 1, minZ),
                new BlockVector3(maxX, maxY, maxZ),
                BlockAir.STATE
        );

        if (this.plotSchematic.getSchematic() != null)
            asyncLevelWorker.addTask(() -> this.pasteRoadSchematic(minX, minZ, maxX, maxZ));

        asyncLevelWorker.runQueue(whenDone);
    }

    private void createRoadSouthEast(Plot plot, WhenDone whenDone) {
        final int groundHeight = this.levelSettings.getGroundHeight();
        final int roadSize = this.levelSettings.getRoadSize();
        final int minY = LevelUtils.getChunkMinY(this.levelSettings.getDimension());
        final int maxY = LevelUtils.getChunkMaxY(this.levelSettings.getDimension());

        final BlockVector3 pos = this.getTopPlotPos(plot);

        final int minX = pos.getX() + 2;
        final int minZ = pos.getZ() + 2;
        final int maxX = minX + roadSize - 2;
        final int maxZ = minZ + roadSize - 2;

        final AxisAlignedBB bb = new SimpleAxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);

        for (Entity entity : this.level.getCollidingEntities(bb))
            if (!(entity instanceof Player)) entity.close();

        for (Block block : this.level.getCollisionBlocks(bb, false, true)) {
            final BlockEntity blockEntity = block.getLevelBlockEntity();
            if (blockEntity != null) blockEntity.close();
        }

        final AsyncLevelWorker asyncLevelWorker = new AsyncLevelWorker(this.level);
        asyncLevelWorker.queueFill(
                new BlockVector3(minX, minY, minZ),
                new BlockVector3(maxX, minY, maxZ),
                this.levelSettings.getFirstLayerState()
        );
        asyncLevelWorker.queueFill(
                new BlockVector3(minX, minY + 1, minZ),
                new BlockVector3(maxX, minY + groundHeight, maxZ),
                this.levelSettings.getRoadFillingState()
        );
        asyncLevelWorker.queueFill(
                new BlockVector3(minX, minY + groundHeight, minZ),
                new BlockVector3(maxX, minY + groundHeight, maxZ),
                this.levelSettings.getRoadState()
        );
        asyncLevelWorker.queueFill(
                new BlockVector3(minX, minY + groundHeight + 1, minZ),
                new BlockVector3(maxX, maxY, maxZ),
                BlockAir.STATE
        );

        if (this.plotSchematic.getSchematic() != null)
            asyncLevelWorker.addTask(() -> this.pasteRoadSchematic(minX, minZ, maxX, maxZ));

        asyncLevelWorker.runQueue(whenDone);
    }

    private BlockVector3 getTopPlotPos(Plot plot) {
        final int plotSize = this.levelSettings.getPlotSize();
        return this.getBottomPlotPos(plot).add(plotSize, 0, plotSize);
    }

    private BlockVector3 getBottomPlotPos(Plot plot) {
        final int totalSize = this.levelSettings.getTotalSize();
        return new BlockVector3(plot.getId().getX() * totalSize - 1, 0, plot.getId().getZ() * totalSize - 1);
    }

    private BlockVector3 getExtendedTopPlotPos(Plot plot) {
        final BlockVector3 top = this.getTopPlotPos(plot);
        if (plot.hasNoMerges()) return top;

        if (plot.isMerged(Plot.DIRECTION_SOUTH))
            top.z = this.getBottomPlotPos(this.getPlotById(plot.getRelative(Plot.DIRECTION_SOUTH))).getZ();
        if (plot.isMerged(Plot.DIRECTION_EAST))
            top.x = this.getBottomPlotPos(this.getPlotById(plot.getRelative(Plot.DIRECTION_EAST))).getX();
        return top;
    }

    private BlockVector3 getExtendedBottomPlotPos(Plot plot) {
        final BlockVector3 bottom = this.getBottomPlotPos(plot);
        if (plot.hasNoMerges()) return bottom;

        if (plot.isMerged(Plot.DIRECTION_NORTH))
            bottom.z = this.getTopPlotPos(this.getPlotById(plot.getRelative(Plot.DIRECTION_NORTH))).getZ() + 2;
        if (plot.isMerged(Plot.DIRECTION_WEST))
            bottom.x = this.getTopPlotPos(this.getPlotById(plot.getRelative(Plot.DIRECTION_WEST))).getX() + 2;
        return bottom;
    }

    public void changeBorder(Plot plot, BlockState blockState) {
        if (plot.isFullyMerged()) return;

        final BlockVector3 bottom = this.getExtendedBottomPlotPos(plot).subtract(plot.isMerged(Plot.DIRECTION_WEST) ? 1 : 0, 0, plot.isMerged(Plot.DIRECTION_NORTH) ? 1 : 0);
        final BlockVector3 top = this.getExtendedTopPlotPos(plot).add(1, 0, 1);
        final AsyncLevelWorker asyncLevelWorker = new AsyncLevelWorker(this.level);
        final int minY = LevelUtils.getChunkMinY(this.levelSettings.getDimension());
        final int y = minY + this.levelSettings.getGroundHeight() + 1;

        if (!plot.isMerged(Plot.DIRECTION_NORTH)) {
            final int z = bottom.getZ();
            asyncLevelWorker.queueFill(
                    new BlockVector3(bottom.getX(), y, z),
                    new BlockVector3(top.getX() - 1, y, z),
                    blockState
            );
        } else {
            final Plot rPlot = this.getPlotById(plot.getRelative(Plot.DIRECTION_NORTH));
            if (rPlot.isMerged(Plot.DIRECTION_EAST) && !plot.isMerged(Plot.DIRECTION_EAST)) {
                final int z = bottom.getZ();
                asyncLevelWorker.queueFill(
                        new BlockVector3(top.getX(), y, z),
                        new BlockVector3(this.getExtendedTopPlotPos(rPlot).getX() - 1, y, z),
                        blockState
                );
            }
        }

        if (!plot.isMerged(Plot.DIRECTION_WEST)) {
            final int x = bottom.getX();
            asyncLevelWorker.queueFill(
                    new BlockVector3(x, y, bottom.getZ()),
                    new BlockVector3(x, y, top.getZ() - 1),
                    blockState
            );
        } else {
            final Plot rPlot = this.getPlotById(plot.getRelative(Plot.DIRECTION_WEST));
            if (rPlot.isMerged(Plot.DIRECTION_NORTH) && !plot.isMerged(Plot.DIRECTION_NORTH)) {
                final int z = this.getBottomPlotPos(plot).getZ();
                asyncLevelWorker.queueFill(
                        new BlockVector3(this.getBottomPlotPos(plot).getX(), y, z),
                        new BlockVector3(bottom.getX() - 1, y, z),
                        blockState
                );
            }
        }

        if (!plot.isMerged(Plot.DIRECTION_SOUTH)) {
            final int z = top.getZ();
            asyncLevelWorker.queueFill(
                    new BlockVector3(bottom.getX(), y, z),
                    new BlockVector3(top.getX() + (plot.isMerged(1) ? -1 : 0), y, z),
                    blockState
            );
        } else {
            final Plot rPlot = this.getPlotById(plot.getRelative(Plot.DIRECTION_SOUTH));
            if (rPlot.isMerged(Plot.DIRECTION_WEST) && !plot.isMerged(Plot.DIRECTION_WEST)) {
                final int z = top.getZ() - 1;
                asyncLevelWorker.queueFill(
                        new BlockVector3(this.getExtendedBottomPlotPos(rPlot).getX() - 1, y, z),
                        new BlockVector3(bottom.getX() - 1, y, z),
                        blockState
                );
            }
        }

        if (!plot.isMerged(Plot.DIRECTION_EAST)) {
            final int x = top.getX();
            asyncLevelWorker.queueFill(
                    new BlockVector3(x, y, bottom.getZ()),
                    new BlockVector3(x, y, top.getZ() + (plot.isMerged(Plot.DIRECTION_SOUTH) ? -1 : 0)),
                    blockState
            );
        } else {
            final Plot rPlot = this.getPlotById(plot.getRelative(Plot.DIRECTION_EAST));
            if (rPlot.isMerged(Plot.DIRECTION_SOUTH) && !plot.isMerged(Plot.DIRECTION_SOUTH)) {
                final int x = top.getX() - 1;
                asyncLevelWorker.queueFill(
                        new BlockVector3(x, y, top.getZ()),
                        new BlockVector3(x, y, this.getExtendedTopPlotPos(rPlot).getZ() - 1),
                        blockState
                );
            }
        }

        asyncLevelWorker.runQueue();
    }

    public void clearWallAbove(Plot plot) {
        if (plot.isFullyMerged()) return;

        final BlockState blockState = BlockAir.STATE;
        final BlockVector3 bottom = this.getExtendedBottomPlotPos(plot).subtract(plot.isMerged(Plot.DIRECTION_WEST) ? 1 : 0, 0, plot.isMerged(Plot.DIRECTION_NORTH) ? 1 : 0);
        final BlockVector3 top = this.getExtendedTopPlotPos(plot).add(1, 0, 1);
        final AsyncLevelWorker asyncLevelWorker = new AsyncLevelWorker(this.level);
        final int minY = LevelUtils.getChunkMinY(this.levelSettings.getDimension()) + this.levelSettings.getGroundHeight() + 2;
        final int maxY = LevelUtils.getChunkMaxY(this.levelSettings.getDimension());

        if (!plot.isMerged(Plot.DIRECTION_NORTH)) {
            final int z = bottom.getZ();
            asyncLevelWorker.queueFill(
                    new BlockVector3(bottom.getX(), minY, z),
                    new BlockVector3(top.getX() - 1, maxY, z),
                    blockState
            );
        } else {
            final Plot rPlot = this.getPlotById(plot.getRelative(Plot.DIRECTION_NORTH));
            if (rPlot.isMerged(Plot.DIRECTION_EAST) && !plot.isMerged(Plot.DIRECTION_EAST)) {
                final int z = bottom.getZ();
                asyncLevelWorker.queueFill(
                        new BlockVector3(top.getX(), minY, z),
                        new BlockVector3(this.getExtendedTopPlotPos(rPlot).getX() - 1, maxY, z),
                        blockState
                );
            }
        }

        if (!plot.isMerged(Plot.DIRECTION_WEST)) {
            final int x = bottom.getX();
            asyncLevelWorker.queueFill(
                    new BlockVector3(x, minY, bottom.getZ()),
                    new BlockVector3(x, maxY, top.getZ() - 1),
                    blockState
            );
        } else {
            final Plot rPlot = this.getPlotById(plot.getRelative(Plot.DIRECTION_WEST));
            if (rPlot.isMerged(Plot.DIRECTION_NORTH) && !plot.isMerged(Plot.DIRECTION_NORTH)) {
                final int z = this.getBottomPlotPos(plot).getZ();
                asyncLevelWorker.queueFill(
                        new BlockVector3(this.getBottomPlotPos(plot).getX(), minY, z),
                        new BlockVector3(bottom.getX() - 1, maxY, z),
                        blockState
                );
            }
        }

        if (!plot.isMerged(Plot.DIRECTION_SOUTH)) {
            final int z = top.getZ();
            asyncLevelWorker.queueFill(
                    new BlockVector3(bottom.getX(), minY, z),
                    new BlockVector3(top.getX() + (plot.isMerged(Plot.DIRECTION_EAST) ? -1 : 0), maxY, z),
                    blockState
            );
        } else {
            final Plot rPlot = this.getPlotById(plot.getRelative(Plot.DIRECTION_SOUTH));
            if (rPlot.isMerged(Plot.DIRECTION_WEST) && !plot.isMerged(Plot.DIRECTION_WEST)) {
                final int z = top.getZ() - 1;
                asyncLevelWorker.queueFill(
                        new BlockVector3(this.getExtendedBottomPlotPos(rPlot).getX() - 1, minY, z),
                        new BlockVector3(bottom.getX() - 1, maxY, z),
                        blockState
                );
            }
        }

        if (!plot.isMerged(Plot.DIRECTION_EAST)) {
            final int x = top.getX();
            asyncLevelWorker.queueFill(
                    new BlockVector3(x, minY, bottom.getZ()),
                    new BlockVector3(x, maxY, top.getZ() + (plot.isMerged(Plot.DIRECTION_SOUTH) ? -1 : 0)),
                    blockState
            );
        } else {
            final Plot rPlot = this.getPlotById(plot.getRelative(Plot.DIRECTION_EAST));
            if (rPlot.isMerged(Plot.DIRECTION_SOUTH) && !plot.isMerged(Plot.DIRECTION_SOUTH)) {
                final int x = top.getX() - 1;
                asyncLevelWorker.queueFill(
                        new BlockVector3(x, minY, top.getZ()),
                        new BlockVector3(x, maxY, this.getExtendedTopPlotPos(rPlot).getZ() - 1),
                        blockState
                );
            }
        }

        asyncLevelWorker.runQueue();
    }

    public void changeWall(Plot plot, BlockState blockState) {
        if (plot.isFullyMerged()) return;

        final BlockVector3 bottom = this.getExtendedBottomPlotPos(plot).subtract(plot.isMerged(Plot.DIRECTION_WEST) ? 1 : 0, 0, plot.isMerged(Plot.DIRECTION_NORTH) ? 1 : 0);
        final BlockVector3 top = this.getExtendedTopPlotPos(plot).add(1, 0, 1);
        final int minY = LevelUtils.getChunkMinY(this.levelSettings.getDimension());

        final AsyncLevelWorker asyncLevelWorker = new AsyncLevelWorker(this.level);

        if (!plot.isMerged(Plot.DIRECTION_NORTH)) {
            final int z = bottom.getZ();
            asyncLevelWorker.queueFill(
                    new BlockVector3(bottom.getX(), minY + 1, z),
                    new BlockVector3(top.getX() - 1, minY + this.levelSettings.getGroundHeight(), z),
                    blockState
            );
        } else {
            final Plot rPlot = this.getPlotById(plot.getRelative(Plot.DIRECTION_NORTH));
            if (rPlot.isMerged(Plot.DIRECTION_EAST) && !plot.isMerged(Plot.DIRECTION_EAST)) {
                final int z = bottom.getZ();
                asyncLevelWorker.queueFill(
                        new BlockVector3(top.getX(), minY + 1, z),
                        new BlockVector3(this.getExtendedTopPlotPos(rPlot).getX() - 1, minY + this.levelSettings.getGroundHeight(), z),
                        blockState
                );
            }
        }

        if (!plot.isMerged(Plot.DIRECTION_WEST)) {
            final int x = bottom.getX();
            asyncLevelWorker.queueFill(
                    new BlockVector3(x, minY + 1, bottom.getZ()),
                    new BlockVector3(x, minY + this.levelSettings.getGroundHeight(), top.getZ() - 1),
                    blockState
            );
        } else {
            final Plot rPlot = this.getPlotById(plot.getRelative(Plot.DIRECTION_WEST));
            if (rPlot.isMerged(Plot.DIRECTION_NORTH) && !plot.isMerged(Plot.DIRECTION_NORTH)) {
                final int z = this.getBottomPlotPos(plot).getZ();
                asyncLevelWorker.queueFill(
                        new BlockVector3(this.getBottomPlotPos(plot).getX(), minY + 1, z),
                        new BlockVector3(bottom.getX() - 1, minY + this.levelSettings.getGroundHeight(), z),
                        blockState
                );
            }
        }

        if (!plot.isMerged(Plot.DIRECTION_SOUTH)) {
            final int z = top.getZ();
            asyncLevelWorker.queueFill(
                    new BlockVector3(bottom.getX(), minY + 1, z),
                    new BlockVector3(top.getX() + (plot.isMerged(Plot.DIRECTION_EAST) ? -1 : 0), minY + this.levelSettings.getGroundHeight(), z),
                    blockState
            );
        } else {
            final Plot rPlot = this.getPlotById(plot.getRelative(Plot.DIRECTION_SOUTH));
            if (rPlot.isMerged(Plot.DIRECTION_WEST) && !plot.isMerged(Plot.DIRECTION_WEST)) {
                final int z = top.getZ() - 1;
                asyncLevelWorker.queueFill(
                        new BlockVector3(this.getExtendedBottomPlotPos(rPlot).getX() - 1, minY + 1, z),
                        new BlockVector3(bottom.getX() - 1, minY + this.levelSettings.getGroundHeight(), z),
                        blockState
                );
            }
        }

        if (!plot.isMerged(Plot.DIRECTION_EAST)) {
            final int x = top.getX();
            asyncLevelWorker.queueFill(
                    new BlockVector3(x, minY + 1, bottom.getZ()),
                    new BlockVector3(x, minY + this.levelSettings.getGroundHeight(), top.getZ() + (plot.isMerged(Plot.DIRECTION_SOUTH) ? -1 : 0)),
                    blockState
            );
        } else {
            final Plot rPlot = this.getPlotById(plot.getRelative(Plot.DIRECTION_EAST));
            if (rPlot.isMerged(Plot.DIRECTION_SOUTH) && !plot.isMerged(Plot.DIRECTION_SOUTH)) {
                final int x = top.getX() - 1;
                asyncLevelWorker.queueFill(
                        new BlockVector3(x, minY + 1, top.getZ()),
                        new BlockVector3(x, minY + this.levelSettings.getGroundHeight(), this.getExtendedTopPlotPos(rPlot).getZ() - 1),
                        blockState
                );
            }
        }

        asyncLevelWorker.runQueue();
    }

    public boolean clearPlot(Plot plot) {
        return this.clearPlot(plot, null);
    }

    private boolean clearPlot(Plot mainPlot, WhenDone finishDone) {
        final PlotClearEvent plotClearEvent = new PlotClearEvent(mainPlot);
        this.plugin.getServer().getPluginManager().callEvent(plotClearEvent);
        if (plotClearEvent.isCancelled()) return false;

        final Set<Plot> plots = new HashSet<>(this.getConnectedPlots(mainPlot));
        if (finishDone != null) finishDone.addTask();

        final WhenDone whenDone = new WhenDone(() -> {
            if (finishDone != null) finishDone.done();

            this.finishPlotClear(plots);
        });

        final AsyncLevelWorker asyncLevelWorker = new AsyncLevelWorker(this.level);
        for (Plot plot : plots) {
            if (plot.isMerged(Plot.DIRECTION_EAST)) this.removeRoadEast(plot, whenDone);
            if (plot.isMerged(Plot.DIRECTION_SOUTH)) this.removeRoadSouth(plot, whenDone);
            if (plot.isMerged(Plot.DIRECTION_SOUTH_EAST)) this.removeRoadSouthEast(plot, whenDone);

            final Vector3 plotPosition = this.getPosByPlot(plot);

            final int minX = plotPosition.getFloorX();
            final int minZ = plotPosition.getFloorZ();
            final int maxX = minX + this.levelSettings.getPlotSize();
            final int maxZ = minZ + this.levelSettings.getPlotSize();
            final int minY = LevelUtils.getChunkMinY(this.levelSettings.getDimension());
            final int maxY = LevelUtils.getChunkMaxY(this.levelSettings.getDimension());

            final int groundHeight = this.levelSettings.getGroundHeight();

            final AxisAlignedBB bb = new SimpleAxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);

            for (Entity entity : this.level.getCollidingEntities(bb))
                if (!(entity instanceof Player)) entity.close();

            for (Block block : this.level.getCollisionBlocks(bb, false, true)) {
                final BlockEntity blockEntity = block.getLevelBlockEntity();
                if (blockEntity != null) blockEntity.close();
            }

            asyncLevelWorker.queueFill(
                    new BlockVector3(minX, minY, minZ),
                    new BlockVector3(maxX, minY, maxZ),
                    this.levelSettings.getFirstLayerState()
            );
            asyncLevelWorker.queueFill(
                    new BlockVector3(minX, minY + 1, minZ),
                    new BlockVector3(maxX, minY + groundHeight, maxZ),
                    this.levelSettings.getMiddleLayerState()
            );
            asyncLevelWorker.queueFill(
                    new BlockVector3(minX, minY + groundHeight, minZ),
                    new BlockVector3(maxX, minY + groundHeight, maxZ),
                    this.levelSettings.getLastLayerState()
            );
            asyncLevelWorker.queueFill(
                    new BlockVector3(minX, minY + groundHeight + 1, minZ),
                    new BlockVector3(maxX, maxY, maxZ),
                    BlockAir.STATE
            );
        }

        asyncLevelWorker.runQueue(whenDone);

        whenDone.start();
        return true;
    }

    private void finishPlotClear(Set<Plot> plots) {
        final BlockState claimBlock = this.levelSettings.getClaimPlotState();
        final BlockState wallBlock = this.levelSettings.getWallPlotState();
        final BlockState wallFillingBlock = this.levelSettings.getWallFillingState();

        for (Plot plot : plots) {
            this.changeBorder(plot, plot.hasOwner() ? claimBlock : wallBlock);
            this.changeWall(plot, wallFillingBlock);
        }
    }

    public ShapeType[] getShapes(int x, int z) {
        final int totalSize = this.levelSettings.getTotalSize();
        final int plotSize = this.levelSettings.getPlotSize();
        final ShapeType[] shapes = new ShapeType[256];

        int posX = x >= 0 ? x % totalSize : totalSize + (x % totalSize);
        int posZ = z >= 0 ? z % totalSize : totalSize + (z % totalSize);

        final int startX = posX;
        for (int zBlock = 0; zBlock < 16; zBlock++, posZ++) {
            if (posZ == totalSize) posZ = 0;

            final ShapeType typeZ;
            if (posZ < plotSize) typeZ = ShapeType.PLOT;
            else if (posZ == plotSize || posZ == totalSize - 1) typeZ = ShapeType.WALL;
            else typeZ = ShapeType.ROAD;

            posX = startX;
            for (int xBlock = 0; xBlock < 16; xBlock++, posX++) {
                if (posX == totalSize) posX = 0;

                final ShapeType typeX;
                if (posX < plotSize) typeX = ShapeType.PLOT;
                else if (posX == plotSize || posX == totalSize - 1) typeX = ShapeType.WALL;
                else typeX = ShapeType.ROAD;

                final ShapeType type;
                if (typeX == typeZ) type = typeX;
                else if (typeX == ShapeType.PLOT) type = typeZ;
                else if (typeZ == ShapeType.PLOT) type = typeX;
                else type = ShapeType.ROAD;

                shapes[(zBlock << 4) | xBlock] = type;
            }
        }

        return shapes;
    }

    public boolean disposePlot(Plot plot) {
        final WhenDone whenDone = new WhenDone(() -> {
            plot.setOwner(null);
            this.unlinkPlotFromNeighbors(plot);
            this.removePlot(plot);
        });

        if (!this.clearPlot(plot, whenDone)) return false;

        whenDone.start();
        return true;
    }

    public void teleportPlayerToPlot(Player player, Plot plot) {
        this.teleportPlayerToPlot(player, plot, true);
    }

    public void teleportPlayerToPlot(Player player, Plot plot, boolean homeAllowed) {
        Vector3 plotVec = null;

        if (homeAllowed) {
            final BlockVector3 homePosition = plot.getHomePosition();
            if (homePosition != null) plotVec = homePosition.clone().add(0.5, 0.1, 0.5);

            if (plotVec != null) {
                final Plot mergedPlot = this.getMergedPlot(plotVec.getFloorX(), plotVec.getFloorZ());
                if (mergedPlot == null || !plot.getOriginId().equals(mergedPlot.getOriginId())) {
                    plot.setHomePosition(null);
                    this.savePlots();
                    plotVec = null;
                }
            }
        }

        if (plotVec == null)
            plotVec = this.getPosByPlot(plot.getBasePlot()).add(
                    ((float) this.levelSettings.getPlotSize() / 2),
                    1f,
                    -1.5f
            );

        player.teleport(Position.fromObject(plotVec.add(0, 0.1, 0), this.level));
    }

}
