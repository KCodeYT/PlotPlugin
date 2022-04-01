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
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.blockstate.BlockState;
import cn.nukkit.entity.Entity;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.format.generic.BaseFullChunk;
import cn.nukkit.math.BlockVector3;
import cn.nukkit.math.Vector2;
import cn.nukkit.math.Vector3;
import lombok.Getter;
import ms.kevi.plotplugin.PlotPlugin;
import ms.kevi.plotplugin.event.PlotClearEvent;
import ms.kevi.plotplugin.generator.PlotGenerator;
import ms.kevi.plotplugin.util.*;
import ms.kevi.plotplugin.util.async.AsyncLevelWorker;
import ms.kevi.plotplugin.util.async.TaskExecutor;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
public class PlotManager {

    private final PlotPlugin plugin;
    @Getter
    private final PlotSchematic plotSchematic;

    @Getter
    private final String levelName;

    @Getter
    private final PlotLevelSettings levelSettings;

    private final Map<PlotId, Plot> plotsCache;

    @Getter
    private Level level;
    private PlotGenerator plotGenerator;

    public PlotManager(PlotPlugin plugin, String levelName) {
        this(plugin, levelName, new PlotLevelSettings());
    }

    public PlotManager(PlotPlugin plugin, String levelName, PlotLevelSettings levelSettings) {
        this.plugin = plugin;
        this.levelName = levelName;
        this.plotSchematic = new PlotSchematic(this.plugin, this);
        this.plotsCache = Collections.synchronizedMap(new ConcurrentHashMap<>());
        this.levelSettings = levelSettings;
    }

    public void initLevel(Level level) {
        this.level = level;
        this.plotGenerator = (PlotGenerator) level.getGenerator();
    }

    public void savePlots() {
        this.plugin.getProvider().savePlots(this.levelName, new ArrayList<>(this.plotsCache.values()));
    }

    public CompletableFuture<Void> addPlot(Plot plot) {
        this.plotsCache.put(plot.getId(), plot);
        if(plot.isDefault()) return CompletableFuture.completedFuture(null);
        return this.plugin.getProvider().addPlot(this.levelName, plot);
    }

    private void removePlot(Plot plot) {
        this.plugin.getProvider().removePlot(this.levelName, plot);
        this.plotsCache.remove(plot.getId());
    }

    public CompletableFuture<Plot> getMergedPlot(int x, int z) {
        final PlotId plotId = this.getPlotIdByPos(x, z);
        if(plotId != null) return this.getPlotById(plotId);

        return CompletableFuture.supplyAsync(() -> {
            Plot plot;

            final int roadSize = this.levelSettings.getRoadSize();
            if((plot = this.getPlot(x - roadSize, z).join()) != null && plot.isMerged(1)) return plot;
            if((plot = this.getPlot(x, z - roadSize).join()) != null && plot.isMerged(2)) return plot;
            if((plot = this.getPlot(x - roadSize, z - roadSize).join()) != null && plot.isMerged(5))
                return plot;

            return null;
        });
    }

    public CompletableFuture<Plot> getPlot(int x, int z) {
        final PlotId plotId = this.getPlotIdByPos(x, z);
        if(plotId == null) return CompletableFuture.completedFuture(null);

        return this.getPlotById(plotId.getX(), plotId.getZ());
    }

    private PlotId getPlotIdByPos(int x, int z) {
        final int plotSize = this.levelSettings.getPlotSize();
        final int totalSize = plotSize + this.levelSettings.getRoadSize();
        final int idX, idZ, difX, difZ;

        if(x >= 0) {
            idX = (int) Math.floor((float) x / totalSize);
            difX = x % totalSize;
        } else {
            idX = (int) Math.ceil((float) (x - plotSize + 1) / totalSize);
            difX = Math.abs((x - plotSize + 1) % totalSize);
        }

        if(z >= 0) {
            idZ = (int) Math.floor((float) z / totalSize);
            difZ = z % totalSize;
        } else {
            idZ = (int) Math.ceil((float) (z - plotSize + 1) / totalSize);
            difZ = Math.abs((z - plotSize + 1) % totalSize);
        }

        if((difX > plotSize - 1) || (difZ > plotSize - 1))
            return null;
        return PlotId.of(idX, idZ);
    }

    public CompletableFuture<Plot> getPlotById(int plotX, int plotZ) {
        return this.getPlotById(PlotId.of(plotX, plotZ));
    }

    public CompletableFuture<Plot> getPlotById(PlotId plotId) {
        if(this.plotsCache.containsKey(plotId)) return CompletableFuture.completedFuture(this.plotsCache.get(plotId));
        return this.plugin.getProvider().getPlot(this.levelName, plotId).
                whenComplete((plot, throwable) -> {
                    if(plot != null) this.addPlot(plot).join();
                });
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

    public CompletableFuture<Plot> getNextFreePlot() {
        return this.plugin.getProvider().searchNextFreePlot(this.levelName);
    }

    public CompletableFuture<List<Plot>> getPlotsByOwner(UUID ownerId) {
        return this.plugin.getProvider().getPlotsByOwner(this.levelName, ownerId);
    }

    public CompletableFuture<Set<Plot>> getConnectedPlots(Plot plot) {
        if(plot.hasNoMerges()) return CompletableFuture.completedFuture(Collections.singleton(plot));

        return CompletableFuture.supplyAsync(() -> {
            final Set<Plot> tmpSet = new HashSet<>();
            final Queue<Plot> frontier = new ArrayDeque<>();
            final Set<Object> queueCache = new HashSet<>();
            tmpSet.add(plot);
            Plot tmp;
            final int[] opposites = new int[]{2, 3, 0, 1};
            for(int iDir = 0; iDir < 4; iDir++) {
                if(plot.isMerged(iDir)) {
                    tmp = this.getPlotById(plot.getRelative(iDir)).join();
                    if(!tmp.isMerged(opposites[iDir]))
                        tmp.setMerged(opposites[iDir], true);
                    queueCache.add(tmp);
                    frontier.add(tmp);
                }
            }

            Plot current;
            while((current = frontier.poll()) != null) {
                tmpSet.add(current);
                queueCache.remove(current);
                for(int iDir = 0; iDir < 4; iDir++) {
                    if(current.isMerged(iDir)) {
                        tmp = this.getPlotById(current.getRelative(iDir)).join();
                        if(tmp != null && !queueCache.contains(tmp) && !tmpSet.contains(tmp)) {
                            queueCache.add(tmp);
                            frontier.add(tmp);
                        }
                    }
                }
            }

            return tmpSet;
        });
    }

    public CompletableFuture<Boolean> startMerge(Plot plot, int dir) {
        return CompletableFuture.supplyAsync(() -> {
            final Plot relativePlot = this.getPlotById(plot.getRelative(dir)).join();

            final List<Plot> plots = new ArrayList<>();
            plots.addAll(this.getConnectedPlots(plot).join());
            plots.addAll(this.getConnectedPlots(relativePlot).join());

            final WhenDone whenDone = new WhenDone(() -> {
                this.finishPlotMerge(plots);
                plot.recalculateOrigin();

                for(Plot other : plots)
                    if(!other.equals(plot)) this.mergePlotData(plot, other);

                this.savePlots();
            });

            int relativeDir;
            for(Plot toMerge0 : plots) {
                for(Plot toMerge1 : plots) {
                    if(toMerge0.equals(toMerge1)) continue;

                    relativeDir = toMerge0.getRelativeDir(toMerge1.getId());
                    if(relativeDir != -1 && !toMerge0.isMerged(relativeDir))
                        this.mergePlot(toMerge0, toMerge1, whenDone);

                    relativeDir = toMerge1.getRelativeDir(toMerge0.getId());
                    if(relativeDir != -1 && !toMerge1.isMerged(relativeDir))
                        this.mergePlot(toMerge1, toMerge0, whenDone);
                }
            }

            whenDone.start();
            return true;
        });
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
        if((!plotAConfig.isEmpty() || !plotBConfig.isEmpty()) && !plotAConfig.equals(plotBConfig)) {
            final boolean greater = plotAConfig.size() > plotBConfig.size();

            if(greater) plotAConfig.putAll(plotBConfig);
            else plotBConfig.putAll(plotAConfig);

            final Map<String, Object> config = greater ? new HashMap<>(plotAConfig) : new HashMap<>(plotBConfig);
            plotAConfig.clear();
            plotAConfig.putAll(config);
            plotBConfig.clear();
            plotBConfig.putAll(config);
        }
    }

    private void finishPlotMerge(List<Plot> plots) {
        final BlockState claimBlock = this.levelSettings.getClaimPlotState();
        final BlockState wallBlock = this.levelSettings.getWallPlotState();
        final BlockState wallFillingBlock = this.levelSettings.getWallFillingState();

        for(Plot plot : plots) {
            this.changeBorder(plot, plot.hasOwner() ? claimBlock : wallBlock).join();
            this.changeWall(plot, wallFillingBlock).join();
        }
    }

    private void mergePlot(Plot lesserPlot, Plot greaterPlot, WhenDone whenDone) {
        if(lesserPlot.getId().getX() == greaterPlot.getId().getX()) {
            if(lesserPlot.getId().getZ() > greaterPlot.getId().getZ()) {
                final Plot tmp = lesserPlot;
                lesserPlot = greaterPlot;
                greaterPlot = tmp;
            }

            if(!lesserPlot.isMerged(2)) {
                lesserPlot.setMerged(2, true);
                greaterPlot.setMerged(0, true);
                this.addPlot(lesserPlot);
                this.addPlot(greaterPlot);

                this.removeRoadSouth(lesserPlot, whenDone);
                final Plot diagonal = this.getPlotById(greaterPlot.getRelative(1)).join();
                if(diagonal.isMerged(7))
                    this.removeRoadSouthEast(lesserPlot, whenDone);
                final Plot below = this.getPlotById(greaterPlot.getRelative(3)).join();
                if(below.isMerged(4))
                    this.removeRoadSouthEast(this.getPlotById(below.getRelative(0)).join(), whenDone);
            }
        } else {
            if(lesserPlot.getId().getX() > greaterPlot.getId().getX()) {
                Plot tmp = lesserPlot;
                lesserPlot = greaterPlot;
                greaterPlot = tmp;
            }

            if(!lesserPlot.isMerged(1)) {
                lesserPlot.setMerged(1, true);
                greaterPlot.setMerged(3, true);
                this.addPlot(lesserPlot);
                this.addPlot(greaterPlot);

                final Plot diagonal = this.getPlotById(greaterPlot.getRelative(2)).join();
                if(diagonal.isMerged(7))
                    this.removeRoadSouthEast(lesserPlot, whenDone);
                this.removeRoadEast(lesserPlot, whenDone);
                final Plot below = this.getPlotById(greaterPlot.getRelative(0)).join();
                if(below.isMerged(6))
                    this.removeRoadSouthEast(this.getPlotById(below.getRelative(3)).join(), whenDone);
            }
        }
    }

    private void removeRoadEast(Plot plot, WhenDone whenDone) {
        final int groundHeight = this.levelSettings.getGroundHeight();
        final int roadSize = this.levelSettings.getRoadSize();
        final int minY = LevelUtils.getChunkMinY(this.levelSettings.getDimension());
        final int maxY = LevelUtils.getChunkMaxY(this.levelSettings.getDimension());

        final BlockVector3 pos1 = this.getBottomPlotPos(plot);
        final BlockVector3 pos2 = this.getTopPlotPos(plot);

        final int xStart = pos2.getX() + 1;
        final int xEnd = xStart + roadSize - 1;
        final int zStart = pos1.getZ() - 1;
        final int zEnd = pos2.getZ() + 1;

        final AsyncLevelWorker asyncLevelWorker = new AsyncLevelWorker(this.level);
        asyncLevelWorker.queueFill(
                new BlockVector3(xStart, minY + groundHeight + 1, zStart + 1),
                new BlockVector3(xEnd, maxY, zEnd - 1),
                BlockState.AIR
        );
        asyncLevelWorker.queueFill(
                new BlockVector3(xStart, minY + 1, zStart + 1),
                new BlockVector3(xEnd, minY + groundHeight - 1, zEnd - 1),
                this.levelSettings.getMiddleLayerState()
        );
        asyncLevelWorker.queueFill(
                new BlockVector3(xStart, minY + groundHeight, zStart + 1),
                new BlockVector3(xEnd, minY + groundHeight, zEnd - 1),
                this.levelSettings.getLastLayerState()
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

        final int xStart = pos1.getX() - 1;
        final int xEnd = pos2.getX() + 1;
        final int zStart = pos2.getZ() + 1;
        final int zEnd = zStart + roadSize - 1;

        final AsyncLevelWorker asyncLevelWorker = new AsyncLevelWorker(this.level);
        asyncLevelWorker.queueFill(
                new BlockVector3(xStart + 1, minY + groundHeight + 1, zStart),
                new BlockVector3(xEnd - 1, maxY, zEnd),
                BlockState.AIR
        );
        asyncLevelWorker.queueFill(
                new BlockVector3(xStart + 1, minY + 1, zStart),
                new BlockVector3(xEnd - 1, minY + groundHeight - 1, zEnd),
                this.levelSettings.getMiddleLayerState()
        );
        asyncLevelWorker.queueFill(
                new BlockVector3(xStart + 1, minY + groundHeight, zStart),
                new BlockVector3(xEnd - 1, minY + groundHeight, zEnd),
                this.levelSettings.getLastLayerState()
        );
        asyncLevelWorker.runQueue(whenDone);
    }

    private void removeRoadSouthEast(Plot plot, WhenDone whenDone) {
        final int groundHeight = this.levelSettings.getGroundHeight();
        final int roadSize = this.levelSettings.getRoadSize();
        final int minY = LevelUtils.getChunkMinY(this.levelSettings.getDimension());
        final int maxY = LevelUtils.getChunkMaxY(this.levelSettings.getDimension());

        final BlockVector3 pos = this.getTopPlotPos(plot);

        final int xStart = pos.getX() + 1;
        final int xEnd = xStart + roadSize - 1;
        final int zStart = pos.getZ() + 1;
        final int zEnd = zStart + roadSize - 1;

        final AsyncLevelWorker asyncLevelWorker = new AsyncLevelWorker(this.level);
        asyncLevelWorker.queueFill(
                new BlockVector3(xStart, minY + groundHeight + 1, zStart),
                new BlockVector3(xEnd, maxY, zEnd),
                BlockState.AIR
        );
        asyncLevelWorker.queueFill(
                new BlockVector3(xStart, minY + 1, zStart),
                new BlockVector3(xEnd, minY + groundHeight - 1, zEnd),
                this.levelSettings.getMiddleLayerState()
        );
        asyncLevelWorker.queueFill(
                new BlockVector3(xStart, minY + groundHeight, zStart),
                new BlockVector3(xEnd, minY + groundHeight, zEnd),
                this.levelSettings.getLastLayerState()
        );
        asyncLevelWorker.runQueue(whenDone);
    }

    public CompletableFuture<Void> unlinkPlot(Plot plot) {
        return this.unlinkPlot(plot, null);
    }

    private CompletableFuture<Void> unlinkPlot(Plot plot, WhenDone finishDone) {
        if(plot.hasNoMerges()) return CompletableFuture.completedFuture(null);

        return CompletableFuture.runAsync(() -> {
            final Set<Plot> plots = this.getConnectedPlots(plot).join();
            final List<PlotId> vectors = new ArrayList<>();
            for(Plot current : plots) vectors.add(current.getId());

            if(finishDone != null) finishDone.addTask();

            final WhenDone whenDone = new WhenDone(() -> {
                this.finishPlotUnlink(vectors);
                if(finishDone != null) finishDone.done();
            });

            for(Plot current : plots) {
                if(current.isMerged(1)) {
                    this.createRoadEast(current, whenDone);
                    if(current.isMerged(2)) {
                        this.createRoadSouth(current, whenDone);
                        if(current.isMerged(5))
                            this.createRoadSouthEast(current, whenDone);
                    }
                } else if(current.isMerged(2))
                    this.createRoadSouth(current, whenDone);
            }

            for(Plot current : plots)
                for(int iDir = 0; iDir < 4; iDir++)
                    current.setMerged(iDir, false);

            whenDone.start();
        });
    }

    private void finishPlotUnlink(List<PlotId> plots) {
        final BlockState claimBlock = BlockState.of(this.levelSettings.getClaimPlotBlockId(), this.levelSettings.getClaimPlotBlockMeta());
        final BlockState wallBlock = BlockState.of(this.levelSettings.getWallPlotBlockId(), this.levelSettings.getWallPlotBlockMeta());
        final BlockState wallFillingBlock = BlockState.of(this.levelSettings.getWallFillingBlockId(), this.levelSettings.getWallFillingBlockMeta());

        for(PlotId plotId : plots) {
            final Plot plot = this.getPlotById(plotId).join();
            this.changeBorder(plot, plot.hasOwner() ? claimBlock : wallBlock);
            this.changeWall(plot, wallFillingBlock);
            plot.recalculateOrigin();
            this.savePlots();
        }
    }

    private Set<FullChunk> pasteRoadSchematic(Vector2 start, Vector2 end, Vector2 startOffset, Vector2 endOffset) {
        final Set<FullChunk> visited = new HashSet<>();

        for(int x = start.getFloorX() + startOffset.getFloorX(); x <= end.getFloorX() + endOffset.getFloorX(); x++) {
            for(int z = start.getFloorY() + startOffset.getFloorY(); z <= end.getFloorY() + endOffset.getFloorY(); z++) {
                final BaseFullChunk fullChunk = this.level.getChunk(x >> 4, z >> 4);
                if(visited.contains(fullChunk)) continue;

                visited.add(fullChunk);
                this.plotGenerator.regenerateChunk(this, fullChunk, false);
            }
        }

        for(int x = start.getFloorX() + 1; x <= end.getFloorX() - 1; x++) {
            for(int z = start.getFloorY() + 1; z <= end.getFloorY() - 1; z++) {
                final BaseFullChunk fullChunk = this.level.getChunk(x >> 4, z >> 4);
                if(visited.contains(fullChunk)) continue;

                visited.add(fullChunk);
                this.plotGenerator.regenerateChunk(this, fullChunk, false);
            }
        }

        return visited;
    }

    private void createRoadEast(Plot plot, WhenDone whenDone) {
        final int groundHeight = this.levelSettings.getGroundHeight();
        final int roadSize = this.levelSettings.getRoadSize();
        final int minY = LevelUtils.getChunkMinY(this.levelSettings.getDimension());
        final int maxY = LevelUtils.getChunkMaxY(this.levelSettings.getDimension());

        final BlockVector3 pos1 = this.getBottomPlotPos(plot);
        final BlockVector3 pos2 = this.getTopPlotPos(plot);

        final int xStart = pos2.getX() + 1;
        final int xEnd = xStart + roadSize - 1;
        final int zStart = pos1.getZ() - 2;
        final int zEnd = pos2.getZ() + 2;

        final AsyncLevelWorker asyncLevelWorker = new AsyncLevelWorker(this.level);
        asyncLevelWorker.queueFill(
                new BlockVector3(xStart, minY + groundHeight + 1, zStart + 2),
                new BlockVector3(xEnd, maxY, zEnd - 1),
                BlockState.AIR
        );
        asyncLevelWorker.queueFill(
                new BlockVector3(xStart, minY, zStart + 2),
                new BlockVector3(xEnd, minY, zEnd - 1),
                this.levelSettings.getFirstLayerState()
        );
        asyncLevelWorker.queueFill(
                new BlockVector3(xStart, minY + 1, zStart + 2),
                new BlockVector3(xEnd, minY + groundHeight, zEnd - 1),
                this.levelSettings.getWallFillingState()
        );
        asyncLevelWorker.queueFill(
                new BlockVector3(xStart + 1, minY + 1, zStart + 1),
                new BlockVector3(xEnd - 1, minY + groundHeight - 1, zEnd - 1),
                this.levelSettings.getRoadFillingState()
        );
        asyncLevelWorker.queueFill(
                new BlockVector3(xStart + 1, minY + groundHeight, zStart + 1),
                new BlockVector3(xEnd - 1, minY + groundHeight, zEnd - 1),
                this.levelSettings.getRoadState()
        );

        if(this.plotSchematic.getSchematic() != null)
            asyncLevelWorker.addTask(() -> this.pasteRoadSchematic(new Vector2(xStart, zStart), new Vector2(xEnd, zEnd), new Vector2(0, 2), new Vector2(0, -1)));

        asyncLevelWorker.runQueue(whenDone);
    }

    private void createRoadSouth(Plot plot, WhenDone whenDone) {
        final int groundHeight = this.levelSettings.getGroundHeight();
        final int roadSize = this.levelSettings.getRoadSize();
        final int minY = LevelUtils.getChunkMinY(this.levelSettings.getDimension());
        final int maxY = LevelUtils.getChunkMaxY(this.levelSettings.getDimension());

        final BlockVector3 pos1 = this.getBottomPlotPos(plot);
        final BlockVector3 pos2 = this.getTopPlotPos(plot);

        final int xStart = pos1.getX() - 2;
        final int xEnd = pos2.getX() + 2;
        final int zStart = pos2.getZ() + 1;
        final int zEnd = zStart + roadSize - 1;

        final AsyncLevelWorker asyncLevelWorker = new AsyncLevelWorker(this.level);
        asyncLevelWorker.queueFill(
                new BlockVector3(xStart + 2, minY + groundHeight + 1, zStart + 1),
                new BlockVector3(xEnd - 1, maxY, zEnd),
                BlockState.AIR
        );
        asyncLevelWorker.queueFill(
                new BlockVector3(xStart + 2, minY, zStart + 1),
                new BlockVector3(xEnd - 1, minY, zEnd),
                this.levelSettings.getFirstLayerState()
        );
        asyncLevelWorker.queueFill(
                new BlockVector3(xStart + 2, minY + 1, zStart + 1),
                new BlockVector3(xEnd - 1, minY + groundHeight, zEnd),
                this.levelSettings.getWallFillingState()
        );
        asyncLevelWorker.queueFill(
                new BlockVector3(xStart + 1, minY + 1, zStart + 1),
                new BlockVector3(xEnd - 1, minY + groundHeight - 1, zEnd - 1),
                this.levelSettings.getRoadFillingState()
        );
        asyncLevelWorker.queueFill(
                new BlockVector3(xStart + 1, minY + groundHeight, zStart + 1),
                new BlockVector3(xEnd - 1, minY + groundHeight, zEnd - 1),
                this.levelSettings.getRoadState()
        );

        if(this.plotSchematic.getSchematic() != null)
            asyncLevelWorker.addTask(() -> this.pasteRoadSchematic(new Vector2(xStart, zStart), new Vector2(xEnd, zEnd), new Vector2(2, 1), new Vector2(-1, 0)));

        asyncLevelWorker.runQueue(whenDone);
    }

    private void createRoadSouthEast(Plot plot, WhenDone whenDone) {
        final int groundHeight = this.levelSettings.getGroundHeight();
        final int roadSize = this.levelSettings.getRoadSize();
        final int minY = LevelUtils.getChunkMinY(this.levelSettings.getDimension());
        final int maxY = LevelUtils.getChunkMaxY(this.levelSettings.getDimension());

        final BlockVector3 pos = this.getTopPlotPos(plot);

        final int xStart = pos.getX() + 1;
        final int xEnd = xStart + roadSize - 1;
        final int zStart = pos.getZ() + 1;
        final int zEnd = zStart + roadSize - 1;

        final AsyncLevelWorker asyncLevelWorker = new AsyncLevelWorker(this.level);
        asyncLevelWorker.queueFill(
                new BlockVector3(xStart + 1, minY + groundHeight + 1, zStart + 1),
                new BlockVector3(xEnd - 1, maxY, zEnd - 1),
                BlockState.AIR
        );
        asyncLevelWorker.queueFill(
                new BlockVector3(xStart + 1, minY, zStart + 1),
                new BlockVector3(xEnd - 1, minY, zEnd - 1),
                this.levelSettings.getFirstLayerState()
        );
        asyncLevelWorker.queueFill(
                new BlockVector3(xStart + 1, minY + 1, zStart + 1),
                new BlockVector3(xEnd - 1, minY + groundHeight - 1, zEnd - 1),
                this.levelSettings.getRoadFillingState()
        );
        asyncLevelWorker.queueFill(
                new BlockVector3(xStart + 1, minY + groundHeight, zStart + 1),
                new BlockVector3(xEnd - 1, minY + groundHeight, zEnd - 1),
                this.levelSettings.getRoadState()
        );

        if(this.plotSchematic.getSchematic() != null)
            asyncLevelWorker.addTask(() -> this.pasteRoadSchematic(new Vector2(xStart, zStart), new Vector2(xEnd, zEnd), new Vector2(1, 1), new Vector2(-1, -1)));

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
        if(plot.hasNoMerges()) return top;

        if(plot.isMerged(2))
            top.z = this.getBottomPlotPos(this.getPlotById(plot.getRelative(2)).join()).getZ();
        if(plot.isMerged(1))
            top.x = this.getBottomPlotPos(this.getPlotById(plot.getRelative(1)).join()).getX();
        return top;
    }

    private BlockVector3 getExtendedBottomPlotPos(Plot plot) {
        final BlockVector3 bottom = this.getBottomPlotPos(plot);
        if(plot.hasNoMerges()) return bottom;

        if(plot.isMerged(0))
            bottom.z = this.getTopPlotPos(this.getPlotById(plot.getRelative(0)).join()).getZ() + 2;
        if(plot.isMerged(3))
            bottom.x = this.getTopPlotPos(this.getPlotById(plot.getRelative(3)).join()).getX() + 2;
        return bottom;
    }

    public CompletableFuture<Void> changeBorder(Plot plot, BlockState blockState) {
        return CompletableFuture.runAsync(() -> {
            final BlockVector3 bottom = this.getExtendedBottomPlotPos(plot).subtract(plot.isMerged(3) ? 1 : 0, 0, plot.isMerged(0) ? 1 : 0);
            final BlockVector3 top = this.getExtendedTopPlotPos(plot).add(1, 0, 1);
            final AsyncLevelWorker asyncLevelWorker = new AsyncLevelWorker(this.level);
            final int minY = LevelUtils.getChunkMinY(this.levelSettings.getDimension());
            final int y = minY + this.levelSettings.getGroundHeight() + 1;

            if(!plot.isMerged(0)) {
                final int z = bottom.getZ();
                asyncLevelWorker.queueFill(
                        new BlockVector3(bottom.getX(), y, z),
                        new BlockVector3(top.getX() - 1, y, z),
                        blockState
                );
            } else {
                final Plot rPlot = this.getPlotById(plot.getRelative(0)).join();
                if(rPlot.isMerged(1) && !plot.isMerged(1)) {
                    final int z = bottom.getZ();
                    asyncLevelWorker.queueFill(
                            new BlockVector3(top.getX(), y, z),
                            new BlockVector3(this.getExtendedTopPlotPos(rPlot).getX() - 1, y, z),
                            blockState
                    );
                }
            }

            if(!plot.isMerged(3)) {
                final int x = bottom.getX();
                asyncLevelWorker.queueFill(
                        new BlockVector3(x, y, bottom.getZ()),
                        new BlockVector3(x, y, top.getZ() - 1),
                        blockState
                );
            } else {
                final Plot rPlot = this.getPlotById(plot.getRelative(3)).join();
                if(rPlot.isMerged(0) && !plot.isMerged(0)) {
                    final int z = this.getBottomPlotPos(plot).getZ();
                    asyncLevelWorker.queueFill(
                            new BlockVector3(this.getBottomPlotPos(plot).getX(), y, z),
                            new BlockVector3(bottom.getX() - 1, y, z),
                            blockState
                    );
                }
            }

            if(!plot.isMerged(2)) {
                final int z = top.getZ();
                asyncLevelWorker.queueFill(
                        new BlockVector3(bottom.getX(), y, z),
                        new BlockVector3(top.getX() + (plot.isMerged(1) ? -1 : 0), y, z),
                        blockState
                );
            } else {
                final Plot rPlot = this.getPlotById(plot.getRelative(2)).join();
                if(rPlot.isMerged(3) && !plot.isMerged(3)) {
                    final int z = top.getZ() - 1;
                    asyncLevelWorker.queueFill(
                            new BlockVector3(this.getExtendedBottomPlotPos(rPlot).getX() - 1, y, z),
                            new BlockVector3(bottom.getX() - 1, y, z),
                            blockState
                    );
                }
            }

            if(!plot.isMerged(1)) {
                final int x = top.getX();
                asyncLevelWorker.queueFill(
                        new BlockVector3(x, y, bottom.getZ()),
                        new BlockVector3(x, y, top.getZ() + (plot.isMerged(2) ? -1 : 0)),
                        blockState
                );
            } else {
                final Plot rPlot = this.getPlotById(plot.getRelative(1)).join();
                if(rPlot.isMerged(2) && !plot.isMerged(2)) {
                    final int x = top.getX() - 1;
                    asyncLevelWorker.queueFill(
                            new BlockVector3(x, y, top.getZ()),
                            new BlockVector3(x, y, this.getExtendedTopPlotPos(rPlot).getZ() - 1),
                            blockState
                    );
                }
            }

            asyncLevelWorker.runQueue();
        });
    }

    public CompletableFuture<Void> changeWall(Plot plot, BlockState blockState) {
        return CompletableFuture.runAsync(() -> {
            final BlockVector3 bottom = this.getExtendedBottomPlotPos(plot).subtract(plot.isMerged(3) ? 1 : 0, 0, plot.isMerged(0) ? 1 : 0);
            final BlockVector3 top = this.getExtendedTopPlotPos(plot).add(1, 0, 1);
            final int minY = LevelUtils.getChunkMinY(this.levelSettings.getDimension());

            final AsyncLevelWorker asyncLevelWorker = new AsyncLevelWorker(this.level);

            if(!plot.isMerged(0)) {
                final int z = bottom.getZ();
                asyncLevelWorker.queueFill(
                        new BlockVector3(bottom.getX(), minY + 1, z),
                        new BlockVector3(top.getX() - 1, minY + this.levelSettings.getGroundHeight(), z),
                        blockState
                );
            } else {
                final Plot rPlot = this.getPlotById(plot.getRelative(0)).join();
                if(rPlot.isMerged(1) && !plot.isMerged(1)) {
                    final int z = bottom.getZ();
                    asyncLevelWorker.queueFill(
                            new BlockVector3(top.getX(), minY + 1, z),
                            new BlockVector3(this.getExtendedTopPlotPos(rPlot).getX() - 1, minY + this.levelSettings.getGroundHeight(), z),
                            blockState
                    );
                }
            }

            if(!plot.isMerged(3)) {
                final int x = bottom.getX();
                asyncLevelWorker.queueFill(
                        new BlockVector3(x, minY + 1, bottom.getZ()),
                        new BlockVector3(x, minY + this.levelSettings.getGroundHeight(), top.getZ() - 1),
                        blockState
                );
            } else {
                final Plot rPlot = this.getPlotById(plot.getRelative(3)).join();
                if(rPlot.isMerged(0) && !plot.isMerged(0)) {
                    final int z = this.getBottomPlotPos(plot).getZ();
                    asyncLevelWorker.queueFill(
                            new BlockVector3(this.getBottomPlotPos(plot).getX(), minY + 1, z),
                            new BlockVector3(bottom.getX() - 1, minY + this.levelSettings.getGroundHeight(), z),
                            blockState
                    );
                }
            }

            if(!plot.isMerged(2)) {
                final int z = top.getZ();
                asyncLevelWorker.queueFill(
                        new BlockVector3(bottom.getX(), minY + 1, z),
                        new BlockVector3(top.getX() + (plot.isMerged(1) ? -1 : 0), minY + this.levelSettings.getGroundHeight(), z),
                        blockState
                );
            } else {
                final Plot rPlot = this.getPlotById(plot.getRelative(2)).join();
                if(rPlot.isMerged(3) && !plot.isMerged(3)) {
                    final int z = top.getZ() - 1;
                    asyncLevelWorker.queueFill(
                            new BlockVector3(this.getExtendedBottomPlotPos(rPlot).getX() - 1, minY + 1, z),
                            new BlockVector3(bottom.getX() - 1, minY + this.levelSettings.getGroundHeight(), z),
                            blockState
                    );
                }
            }

            if(!plot.isMerged(1)) {
                final int x = top.getX();
                asyncLevelWorker.queueFill(
                        new BlockVector3(x, minY + 1, bottom.getZ()),
                        new BlockVector3(x, minY + this.levelSettings.getGroundHeight(), top.getZ() + (plot.isMerged(2) ? -1 : 0)),
                        blockState
                );
            } else {
                final Plot rPlot = this.getPlotById(plot.getRelative(1)).join();
                if(rPlot.isMerged(2) && !plot.isMerged(2)) {
                    final int x = top.getX() - 1;
                    asyncLevelWorker.queueFill(
                            new BlockVector3(x, minY + 1, top.getZ()),
                            new BlockVector3(x, minY + this.levelSettings.getGroundHeight(), this.getExtendedTopPlotPos(rPlot).getZ() - 1),
                            blockState
                    );
                }
            }

            asyncLevelWorker.runQueue();
        });
    }

    public CompletableFuture<Boolean> clearPlot(Plot plot) {
        return this.clearPlot(plot, null);
    }

    private CompletableFuture<Boolean> clearPlot(Plot plot, WhenDone finishDone) {
        final PlotClearEvent plotClearEvent = new PlotClearEvent(plot);
        this.plugin.getServer().getPluginManager().callEvent(plotClearEvent);
        if(plotClearEvent.isCancelled()) return CompletableFuture.completedFuture(false);

        return CompletableFuture.supplyAsync(() -> {
            final Set<Plot> visited = new HashSet<>();
            if(finishDone != null) finishDone.addTask();

            final WhenDone whenDone = new WhenDone(() -> {
                for(Plot visit : visited) this.finishClearPlot(visit);
                if(finishDone != null) finishDone.done();
            });

            final Set<Plot> connectedPlots = this.getConnectedPlots(plot).join();
            visited.addAll(connectedPlots);
            for(Plot connectedPlot : connectedPlots) this.unlinkPlot(connectedPlot, whenDone).join();
            whenDone.start();

            return true;
        });
    }

    private void finishClearPlot(Plot plot) {
        final Vector3 vector = this.getPosByPlot(plot);

        final int xMax = vector.getFloorX() + this.levelSettings.getPlotSize();
        final int zMax = vector.getFloorZ() + this.levelSettings.getPlotSize();
        final int minY = LevelUtils.getChunkMinY(this.levelSettings.getDimension());
        final int maxY = LevelUtils.getChunkMaxY(this.levelSettings.getDimension());

        TaskExecutor.executeAsync(() -> {
            List<BaseFullChunk> fullChunks = new ArrayList<>();
            final Vector3 cPos = new Vector3(0, 0, 0);
            for(int x = vector.getFloorX(); x < xMax; ++x) {
                for(int z = vector.getFloorZ(); z < zMax; ++z) {
                    cPos.setComponents(x, 0, z);
                    final BaseFullChunk fullChunk = this.level.getChunk(cPos.getChunkX(), cPos.getChunkZ());
                    if(fullChunk == null) continue;
                    if(!fullChunks.contains(fullChunk)) fullChunks.add(fullChunk);

                    final int floorX = x;
                    final int floorZ = z;

                    TaskExecutor.execute(() -> {
                        if(!fullChunk.getBlockEntities().isEmpty()) {
                            for(BlockEntity blockEntity : new ArrayList<>(fullChunk.getBlockEntities().values())) {
                                try {
                                    if(blockEntity.getFloorX() == floorX && blockEntity.getFloorZ() == floorZ)
                                        blockEntity.close();
                                } catch(Exception e) {
                                    this.plugin.getLogger().warning(
                                            "Could not close block entity in plot " + plot.getId() + " and position x: " + floorX + " z:" + floorZ,
                                            e
                                    );
                                }
                            }
                        }

                        if(!fullChunk.getEntities().isEmpty()) {
                            for(Entity entity : new ArrayList<>(fullChunk.getEntities().values())) {
                                try {
                                    if(!(entity instanceof Player) && entity.getFloorX() == floorX && entity.getFloorZ() == floorZ)
                                        entity.close();
                                } catch(Exception e) {
                                    this.plugin.getLogger().warning(
                                            "Could not close entity in plot " + plot.getId() + " and position x: " + floorX + " z:" + floorZ,
                                            e
                                    );
                                }
                            }
                        }
                    });

                    for(int y = minY; y <= maxY; ++y) {
                        if(y == minY)
                            fullChunk.setBlockState(floorX & 15, y, floorZ & 15, this.levelSettings.getFirstLayerState());
                        else if(y < minY + this.levelSettings.getGroundHeight())
                            fullChunk.setBlockState(floorX & 15, y, floorZ & 15, this.levelSettings.getMiddleLayerState());
                        else if(y == minY + this.levelSettings.getGroundHeight())
                            fullChunk.setBlockState(floorX & 15, y, floorZ & 15, this.levelSettings.getLastLayerState());
                        else
                            fullChunk.setBlock(floorX & 15, y, floorZ & 15, Block.AIR);
                        fullChunk.setBlockAtLayer(floorX & 15, y, floorZ & 15, 1, Block.AIR);
                    }
                }
            }

            fullChunks.forEach(fullChunk -> Server.getInstance().getOnlinePlayers().values().forEach(player -> {
                if(fullChunk.getProvider() == null || fullChunk.getProvider().getLevel() == null || !fullChunk.getProvider().getLevel().equals(player.getLevel()))
                    return;
                if(player.getLevel().getChunkPlayers(fullChunk.getX(), fullChunk.getZ()).containsValue(player))
                    player.getLevel().requestChunk(fullChunk.getX(), fullChunk.getZ(), player);
            }));
        });
    }

    public ShapeType[] getShapes(int x, int z) {
        final int totalSize = this.levelSettings.getTotalSize();
        final int plotSize = this.levelSettings.getPlotSize();
        final ShapeType[] shapes = new ShapeType[256];

        int posX;
        if(x >= 0) posX = x % totalSize;
        else posX = totalSize - Math.abs(x % totalSize);

        int posZ;
        if(z >= 0) posZ = z % totalSize;
        else posZ = totalSize - Math.abs(z % totalSize);

        int startX = posX;
        for(int zBlock = 0; zBlock < 16; zBlock++, posZ++) {
            if(posZ == totalSize) posZ = 0;

            final ShapeType typeZ;
            if(posZ < plotSize) typeZ = ShapeType.PLOT;
            else if(posZ == plotSize || posZ == (totalSize - 1)) typeZ = ShapeType.WALL;
            else typeZ = ShapeType.ROAD;

            posX = startX;
            for(int xBlock = 0; xBlock < 16; xBlock++, posX++) {
                if(posX == totalSize) posX = 0;

                final ShapeType typeX;
                if(posX < plotSize) typeX = ShapeType.PLOT;
                else if(posX == plotSize || posX == (totalSize - 1)) typeX = ShapeType.WALL;
                else typeX = ShapeType.ROAD;

                final ShapeType type;
                if(typeX == typeZ) type = typeX;
                else if(typeX == ShapeType.PLOT) type = typeZ;
                else if(typeZ == ShapeType.PLOT) type = typeX;
                else type = ShapeType.ROAD;

                shapes[(zBlock << 4) | xBlock] = type;
            }
        }

        return shapes;
    }

    public CompletableFuture<Void> disposePlot(Plot plot) {
        return CompletableFuture.runAsync(() -> {
            final WhenDone whenDone = new WhenDone(() -> {
                this.changeWall(plot, BlockState.of(this.levelSettings.getWallFillingBlockId(), this.levelSettings.getWallFillingBlockMeta()));
                this.changeBorder(plot, BlockState.of(this.levelSettings.getWallPlotBlockId(), this.levelSettings.getWallPlotBlockMeta()));
                this.removePlot(plot);
                this.savePlots();
            });

            if(!this.clearPlot(plot, whenDone).join()) return;
            whenDone.start();
        });
    }

    public void teleportPlayerToPlot(Player player, Plot plot) {
        final Vector3 plotVec = this.getPosByPlot(plot.getBasePlot());
        player.teleport(new Location(plotVec.getX() + ((float) this.levelSettings.getPlotSize() / 2), plotVec.getY() + 1f, plotVec.getZ() - 1.5f, this.level));
    }

}
