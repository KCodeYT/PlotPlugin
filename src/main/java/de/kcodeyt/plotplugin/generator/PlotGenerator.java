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

package de.kcodeyt.plotplugin.generator;

import cn.nukkit.Player;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.blockstate.BlockState;
import cn.nukkit.entity.Entity;
import cn.nukkit.level.ChunkManager;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.generator.Generator;
import cn.nukkit.math.NukkitRandom;
import cn.nukkit.math.Vector3;
import de.kcodeyt.plotplugin.PlotPlugin;
import de.kcodeyt.plotplugin.manager.PlotManager;
import de.kcodeyt.plotplugin.schematic.Schematic;
import de.kcodeyt.plotplugin.util.Allowed;
import de.kcodeyt.plotplugin.util.PlotLevelSettings;
import de.kcodeyt.plotplugin.util.ShapeType;
import de.kcodeyt.plotplugin.util.async.TaskExecutor;
import de.kcodeyt.plotplugin.util.async.TaskHelper;

import java.util.*;

public class PlotGenerator extends Generator {

    private static final Allowed<ShapeType> GENERATE_ALLOWED = new Allowed<>(ShapeType.values());
    private static final Allowed<ShapeType> REGENERATE_ALLOWED = new Allowed<>(ShapeType.WALL, ShapeType.ROAD);

    private final PlotPlugin plugin;

    private Level level;
    private ChunkManager chunkManager;

    public PlotGenerator() {
        this(new HashMap<>());
    }

    public PlotGenerator(Map<String, Object> ignored) {
        this.plugin = PlotPlugin.INSTANCE;
    }

    @Override
    public int getDimension() {
        final PlotManager plotManager;
        if(this.level != null && (plotManager = this.plugin.getPlotManager(this.level)) != null)
            return plotManager.getLevelSettings().getDimension();
        return Level.DIMENSION_OVERWORLD;
    }

    @Override
    public int getId() {
        return Generator.TYPE_FLAT;
    }

    @Override
    public void init(ChunkManager chunkManager, NukkitRandom nukkitRandom) {
        if(chunkManager instanceof Level) this.level = (Level) chunkManager;
        this.chunkManager = chunkManager;
    }

    @Override
    public void generateChunk(int chunkX, int chunkZ) {
        final long startMs = System.currentTimeMillis();
        final FullChunk fullChunk = this.chunkManager.getChunk(chunkX, chunkZ);
        fullChunk.setGenerated();

        if(fullChunk.getProvider() == null || fullChunk.getProvider().getLevel() == null) return;

        final Level level = fullChunk.getProvider().getLevel();
        final PlotManager plotManager = this.plugin.getPlotManager(level);
        if(plotManager == null) return;

        final ShapeType[] shapes = plotManager.getShapes(fullChunk.getX() << 4, fullChunk.getZ() << 4);

        this.preGenerateChunk(plotManager, fullChunk, shapes, GENERATE_ALLOWED, true);
        final Schematic schematic = plotManager.getPlotChunk().getSchematic();
        if(schematic != null) this.placeChunkSchematic(plotManager, schematic, fullChunk, shapes, GENERATE_ALLOWED);
        System.out.println("Es dauerte " + (System.currentTimeMillis() - startMs) + "ms!");
    }

    public void regenerateChunk(PlotManager plotManager, FullChunk fullChunk, boolean resend) {
        final ShapeType[] shapes = plotManager.getShapes(fullChunk.getX() << 4, fullChunk.getZ() << 4);

        final List<Entity> toClose0 = new ArrayList<>();
        final List<BlockEntity> toClose1 = new ArrayList<>();

        for(Entity entity : new ArrayList<>(fullChunk.getEntities().values())) {
            if(entity instanceof Player) continue;

            if(!REGENERATE_ALLOWED.isAllowed(shapes[((entity.getFloorZ() & 15) << 4) | (entity.getFloorX() & 15)]))
                continue;
            toClose0.add(entity);
        }

        for(BlockEntity blockEntity : new ArrayList<>(fullChunk.getBlockEntities().values())) {
            if(!REGENERATE_ALLOWED.isAllowed(shapes[((blockEntity.getFloorZ() & 15) << 4) | (blockEntity.getFloorX() & 15)]))
                continue;
            toClose1.add(blockEntity);
        }

        TaskExecutor.execute(() -> {
            for(Entity entity : toClose0) entity.close();
            for(BlockEntity blockEntity : toClose1) blockEntity.close();
        });

        this.preGenerateChunk(plotManager, fullChunk, shapes, REGENERATE_ALLOWED, false);

        final Schematic schematic = plotManager.getPlotChunk().getSchematic();
        if(schematic != null) this.placeChunkSchematic(plotManager, schematic, fullChunk, shapes, REGENERATE_ALLOWED);

        if(resend) {
            final Level level = fullChunk.getProvider().getLevel();
            level.getChunkPlayers(fullChunk.getX(), fullChunk.getZ()).values().forEach(player -> level.requestChunk(fullChunk.getX(), fullChunk.getZ(), player));
        }
    }

    private void preGenerateChunk(PlotManager plotManager, FullChunk fullChunk, ShapeType[] shapes,
                                  Allowed<ShapeType> allowedShapes, boolean ignoreAir) {
        final PlotLevelSettings levelSettings = plotManager.getLevelSettings();

        final BlockState firstLayerState = levelSettings.getFirstLayerState();
        final BlockState middleLayerState = levelSettings.getMiddleLayerState();
        final BlockState wallFillingState = levelSettings.getWallFillingState();
        final BlockState roadFillingState = levelSettings.getRoadFillingState();
        final BlockState lastLayerState = levelSettings.getLastLayerState();
        final BlockState wallPlotState = levelSettings.getWallPlotState();
        final BlockState roadState = levelSettings.getRoadState();

        for(int xBlock = 0; xBlock < 16; ++xBlock) {
            for(int zBlock = 0; zBlock < 16; ++zBlock) {
                final ShapeType shapeType = shapes[(zBlock << 4) | xBlock];
                if(!allowedShapes.isAllowed(shapeType)) continue;

                if(shapeType == ShapeType.PLOT) fullChunk.setBiomeId(xBlock, zBlock, levelSettings.getPlotBiome());
                else fullChunk.setBiomeId(xBlock, zBlock, levelSettings.getRoadBiome());

                if(!ignoreAir) for(int yBlock = 0; yBlock < 256; ++yBlock)
                    fullChunk.setBlockStateAtLayer(xBlock, yBlock, zBlock, 1, BlockState.AIR);

                fullChunk.setBlockState(xBlock, 0, zBlock, firstLayerState);

                final BlockState currentState;
                if(shapeType == ShapeType.PLOT) currentState = middleLayerState;
                else if(shapeType == ShapeType.WALL) currentState = wallFillingState;
                else currentState = roadFillingState;

                for(int yBlock = 1; yBlock < levelSettings.getGroundHeight(); ++yBlock)
                    fullChunk.setBlockState(xBlock, yBlock, zBlock, currentState);

                if(shapeType == ShapeType.PLOT) {
                    fullChunk.setBlockState(xBlock, levelSettings.getGroundHeight(), zBlock, lastLayerState);
                    if(!ignoreAir) for(int yBlock = levelSettings.getGroundHeight() + 1; yBlock < 256; ++yBlock)
                        fullChunk.setBlockState(xBlock, yBlock, zBlock, BlockState.AIR);
                } else {
                    if(shapeType == ShapeType.WALL) {
                        fullChunk.setBlockState(xBlock, levelSettings.getGroundHeight() + 1, zBlock, wallPlotState);
                        fullChunk.setBlockState(xBlock, levelSettings.getGroundHeight(), zBlock, wallFillingState);
                        if(!ignoreAir) for(int yBlock = levelSettings.getGroundHeight() + 2; yBlock < 256; ++yBlock)
                            fullChunk.setBlockState(xBlock, yBlock, zBlock, BlockState.AIR);
                    } else {
                        fullChunk.setBlockState(xBlock, levelSettings.getGroundHeight(), zBlock, roadState);
                        if(!ignoreAir) for(int yBlock = levelSettings.getGroundHeight() + 1; yBlock < 256; ++yBlock)
                            fullChunk.setBlockState(xBlock, yBlock, zBlock, BlockState.AIR);
                    }
                }
            }
        }
    }

    public BlockState getDefaultBlockStateAt(PlotManager plotManager, ShapeType[] shapes, Vector3 blockVector) {
        final PlotLevelSettings levelSettings = plotManager.getLevelSettings();
        final int xBlock = blockVector.getFloorX() & 15;
        final int yBlock = blockVector.getFloorY();
        final int zBlock = blockVector.getFloorZ() & 15;
        final ShapeType shapeType = shapes[(zBlock << 4) | xBlock];

        if(yBlock == 0) {
            return levelSettings.getFirstLayerState();
        } else if(yBlock < levelSettings.getGroundHeight()) {
            switch(shapeType) {
                case PLOT:
                    return levelSettings.getMiddleLayerState();
                case WALL:
                    return levelSettings.getWallFillingState();
                case ROAD:
                    return levelSettings.getRoadFillingState();
            }
        } else if(yBlock == levelSettings.getGroundHeight()) {
            switch(shapeType) {
                case PLOT:
                    return levelSettings.getLastLayerState();
                case WALL:
                    return levelSettings.getWallFillingState();
                case ROAD:
                    return levelSettings.getRoadState();
            }
        } else if(yBlock == levelSettings.getGroundHeight() + 1) {
            if(shapeType == ShapeType.WALL) return levelSettings.getWallPlotState();
        }

        return BlockState.AIR;
    }

    @Override
    public void populateChunk(int chunkX, int chunkZ) {
        final FullChunk fullChunk = this.chunkManager.getChunk(chunkX, chunkZ);
        fullChunk.setPopulated();
    }

    private void placeChunkSchematic(PlotManager plotManager, Schematic schematic, FullChunk fullChunk,
                                     ShapeType[] shapes, Allowed<ShapeType> allowedShapes) {
        final Set<Vector3> handledVectors = new HashSet<>();
        final int fullX = fullChunk.getX() << 4;
        final int fullZ = fullChunk.getZ() << 4;
        final TaskHelper taskHelper = new TaskHelper();

        for(int blockX = fullX; blockX < fullX + 16; blockX++) {
            for(int blockZ = fullZ; blockZ < fullZ + 16; blockZ++) {
                final Vector3 plotArea = this.getPlotAreaStart(plotManager, blockX, blockZ);
                if(handledVectors.contains(plotArea)) continue;

                handledVectors.add(plotArea);
                schematic.buildInChunk(taskHelper, plotArea, fullChunk, shapes, allowedShapes);
            }
        }

        taskHelper.runAsyncTasks();
        TaskExecutor.execute(taskHelper::runSyncTasks);
    }

    @Override
    public Map<String, Object> getSettings() {
        return new HashMap<>();
    }

    @Override
    public String getName() {
        return "Plot";
    }

    @Override
    public Vector3 getSpawn() {
        return new Vector3(0.5, 128, 0.5);
    }

    @Override
    public ChunkManager getChunkManager() {
        return this.chunkManager;
    }

    public Vector3 getPlotAreaStart(PlotManager plotManager, int x, int z) {
        final int totalSize = plotManager.getLevelSettings().getTotalSize();
        final int xPart = x / totalSize;
        final int zPart = z / totalSize;
        return new Vector3((x < 0 ? xPart - 1 : xPart) * totalSize, 0, (z < 0 ? zPart - 1 : zPart) * totalSize);
    }

    public Vector3 getPlotAreaEnd(PlotManager plotManager, int x, int z) {
        final int totalSize = plotManager.getLevelSettings().getTotalSize();
        final int xPart = x / totalSize;
        final int zPart = z / totalSize;
        return new Vector3((x < 0 ? xPart : xPart + 1) * totalSize, 255, (z < 0 ? zPart : zPart + 1) * totalSize);
    }

    public Vector3[] getPlotArea(PlotManager plotManager, int x, int z) {
        return new Vector3[]{this.getPlotAreaStart(plotManager, x, z), this.getPlotAreaEnd(plotManager, x, z)};
    }

}
