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

package ms.kevi.plotplugin.generator;

import cn.nukkit.Player;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.blockstate.BlockState;
import cn.nukkit.entity.Entity;
import cn.nukkit.level.ChunkManager;
import cn.nukkit.level.DimensionData;
import cn.nukkit.level.DimensionEnum;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.generator.Generator;
import cn.nukkit.math.NukkitRandom;
import cn.nukkit.math.Vector3;
import ms.kevi.plotplugin.PlotPlugin;
import ms.kevi.plotplugin.manager.PlotManager;
import ms.kevi.plotplugin.schematic.Schematic;
import ms.kevi.plotplugin.util.Allowed;
import ms.kevi.plotplugin.util.LevelUtils;
import ms.kevi.plotplugin.util.PlotLevelSettings;
import ms.kevi.plotplugin.util.ShapeType;
import ms.kevi.plotplugin.util.async.TaskExecutor;

import java.util.*;

/**
 * A basic plot generator for the PowerNukkitX environment,
 * which supports the world heights introduced in the 1.18
 * caves and cliffs update.
 *
 * @author Kevims KCodeYT
 * @version 1.0
 */
public class PlotGenerator extends Generator {

    private static final Allowed<ShapeType> GENERATE_ALLOWED = new Allowed<>(ShapeType.values());
    private static final Allowed<ShapeType> REGENERATE_ALLOWED = new Allowed<>(ShapeType.WALL, ShapeType.ROAD);

    private final PlotPlugin plugin;

    private Level level;
    private ChunkManager chunkManager;

    @SuppressWarnings("unused")
    public PlotGenerator() {
        this(new HashMap<>());
    }

    public PlotGenerator(Map<String, Object> ignored) {
        this.plugin = PlotPlugin.INSTANCE;
    }

    @Override
    public DimensionData getDimensionData() {
        final PlotManager plotManager;
        if(this.level != null && (plotManager = this.plugin.getPlotManager(this.level)) != null)
            return DimensionEnum.getDataFromId(plotManager.getLevelSettings().getDimension());
        return DimensionEnum.OVERWORLD.getDimensionData();
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
        final FullChunk fullChunk = this.chunkManager.getChunk(chunkX, chunkZ);
        fullChunk.setGenerated();

        if(fullChunk.getProvider() == null || fullChunk.getProvider().getLevel() == null) return;

        this.level = fullChunk.getProvider().getLevel();
        final PlotManager plotManager = this.plugin.getPlotManager(this.level);
        if(plotManager == null) return;

        final ShapeType[] shapes = plotManager.getShapes(fullChunk.getX() << 4, fullChunk.getZ() << 4);

        this.preGenerateChunk(plotManager, fullChunk, shapes, GENERATE_ALLOWED, true, null, null, null, null);
        final Schematic schematic = plotManager.getPlotSchematic().getSchematic();
        if(schematic != null) this.placeChunkSchematic(plotManager, schematic, fullChunk, shapes, GENERATE_ALLOWED, null, null, null, null);
    }

    public void regenerateChunk(PlotManager plotManager, FullChunk fullChunk) {
        final ShapeType[] shapes = plotManager.getShapes(fullChunk.getX() << 4, fullChunk.getZ() << 4);

        final List<Entity> toClose0 = new ArrayList<>();
        final List<BlockEntity> toClose1 = new ArrayList<>();

        for(Entity entity : new ArrayList<>(fullChunk.getEntities().values())) {
            if(entity instanceof Player) continue;

            if(REGENERATE_ALLOWED.isDisallowed(shapes[((entity.getFloorZ() & 15) << 4) | (entity.getFloorX() & 15)]))
                continue;

            toClose0.add(entity);
        }

        for(BlockEntity blockEntity : new ArrayList<>(fullChunk.getBlockEntities().values())) {
            if(REGENERATE_ALLOWED.isDisallowed(shapes[((blockEntity.getFloorZ() & 15) << 4) | (blockEntity.getFloorX() & 15)]))
                continue;

            toClose1.add(blockEntity);
        }

        TaskExecutor.execute(() -> {
            for(Entity entity : toClose0) entity.close();
            for(BlockEntity blockEntity : toClose1) blockEntity.close();
        });

        this.preGenerateChunk(plotManager, fullChunk, shapes, REGENERATE_ALLOWED, false, null, null, null, null);

        final Schematic schematic = plotManager.getPlotSchematic().getSchematic();
        if(schematic != null) this.placeChunkSchematic(plotManager, schematic, fullChunk, shapes, REGENERATE_ALLOWED, null, null, null, null);

        final Level level = fullChunk.getProvider().getLevel();
        level.getChunkPlayers(fullChunk.getX(), fullChunk.getZ()).values().forEach(player -> level.requestChunk(fullChunk.getX(), fullChunk.getZ(), player));
    }

    public void regenerateChunkWithin(PlotManager plotManager, FullChunk fullChunk, int minX, int minZ, int maxX, int maxZ) {
        final ShapeType[] shapes = plotManager.getShapes(fullChunk.getX() << 4, fullChunk.getZ() << 4);

        final List<Entity> toClose0 = new ArrayList<>();
        final List<BlockEntity> toClose1 = new ArrayList<>();

        for(Entity entity : new ArrayList<>(fullChunk.getEntities().values())) {
            if(entity instanceof Player) continue;
            if(entity.getFloorX() < minX || entity.getFloorX() > maxX || entity.getFloorZ() < minZ || entity.getFloorZ() > maxZ)
                continue;

            if(REGENERATE_ALLOWED.isDisallowed(shapes[((entity.getFloorZ() & 15) << 4) | (entity.getFloorX() & 15)]))
                continue;

            toClose0.add(entity);
        }

        for(BlockEntity blockEntity : new ArrayList<>(fullChunk.getBlockEntities().values())) {
            if(blockEntity.getFloorX() < minX || blockEntity.getFloorX() > maxX || blockEntity.getFloorZ() < minZ || blockEntity.getFloorZ() > maxZ)
                continue;
            if(REGENERATE_ALLOWED.isDisallowed(shapes[((blockEntity.getFloorZ() & 15) << 4) | (blockEntity.getFloorX() & 15)]))
                continue;

            toClose1.add(blockEntity);
        }

        TaskExecutor.execute(() -> {
            for(Entity entity : toClose0) entity.close();
            for(BlockEntity blockEntity : toClose1) blockEntity.close();
        });

        this.preGenerateChunk(plotManager, fullChunk, shapes, REGENERATE_ALLOWED, false, minX, minZ, maxX, maxZ);

        final Schematic schematic = plotManager.getPlotSchematic().getSchematic();
        if(schematic != null) this.placeChunkSchematic(plotManager, schematic, fullChunk, shapes, REGENERATE_ALLOWED, minX, minZ, maxX, maxZ);
    }

    private void preGenerateChunk(PlotManager plotManager, FullChunk fullChunk, ShapeType[] shapes,
                                  Allowed<ShapeType> allowedShapes, boolean ignoreAir, Integer minX, Integer minZ, Integer maxX, Integer maxZ) {
        final PlotLevelSettings levelSettings = plotManager.getLevelSettings();

        final BlockState firstLayerState = levelSettings.getFirstLayerState();
        final BlockState middleLayerState = levelSettings.getMiddleLayerState();
        final BlockState wallFillingState = levelSettings.getWallFillingState();
        final BlockState roadFillingState = levelSettings.getRoadFillingState();
        final BlockState lastLayerState = levelSettings.getLastLayerState();
        final BlockState wallPlotState = levelSettings.getWallPlotState();
        final BlockState roadState = levelSettings.getRoadState();

        final int dimension = plotManager.getLevelSettings().getDimension();
        final int chunkMinY = LevelUtils.getChunkMinY(dimension);
        final int chunkMaxY = LevelUtils.getChunkMaxY(dimension);

        for(int xBlock = 0; xBlock < 16; ++xBlock) {
            if(minX != null && (xBlock + (fullChunk.getX() << 4) < minX || xBlock + (fullChunk.getX() << 4) > maxX)) continue;
            for(int zBlock = 0; zBlock < 16; ++zBlock) {
                if(minZ != null && (zBlock + (fullChunk.getZ() << 4) < minZ || zBlock + (fullChunk.getZ() << 4) > maxZ)) continue;

                final ShapeType shapeType = shapes[(zBlock << 4) | xBlock];
                if(allowedShapes.isDisallowed(shapeType)) continue;

                if(shapeType == ShapeType.PLOT) fullChunk.setBiomeId(xBlock, zBlock, levelSettings.getPlotBiome());
                else fullChunk.setBiomeId(xBlock, zBlock, levelSettings.getRoadBiome());

                if(!ignoreAir) for(int yBlock = chunkMinY; yBlock <= chunkMaxY; ++yBlock)
                    fullChunk.setBlockStateAtLayer(xBlock, yBlock, zBlock, 1, BlockState.AIR);

                fullChunk.setBlockState(xBlock, chunkMinY, zBlock, firstLayerState);

                final BlockState currentState;
                if(shapeType == ShapeType.PLOT) currentState = middleLayerState;
                else if(shapeType == ShapeType.WALL) currentState = wallFillingState;
                else currentState = roadFillingState;

                for(int yBlock = 1; yBlock < levelSettings.getGroundHeight(); ++yBlock)
                    fullChunk.setBlockState(xBlock, yBlock + chunkMinY, zBlock, currentState);

                if(shapeType == ShapeType.PLOT) {
                    fullChunk.setBlockState(xBlock, levelSettings.getGroundHeight() + chunkMinY, zBlock, lastLayerState);
                    if(!ignoreAir) for(int yBlock = levelSettings.getGroundHeight() + 1; yBlock <= chunkMaxY; ++yBlock)
                        fullChunk.setBlockState(xBlock, yBlock, zBlock, BlockState.AIR);
                } else {
                    if(shapeType == ShapeType.WALL) {
                        fullChunk.setBlockState(xBlock, levelSettings.getGroundHeight() + 1 + chunkMinY, zBlock, wallPlotState);
                        fullChunk.setBlockState(xBlock, levelSettings.getGroundHeight() + chunkMinY, zBlock, wallFillingState);
                        if(!ignoreAir)
                            for(int yBlock = levelSettings.getGroundHeight() + 2; yBlock <= chunkMaxY; ++yBlock)
                                fullChunk.setBlockState(xBlock, yBlock, zBlock, BlockState.AIR);
                    } else {
                        fullChunk.setBlockState(xBlock, levelSettings.getGroundHeight() + chunkMinY, zBlock, roadState);
                        if(!ignoreAir)
                            for(int yBlock = levelSettings.getGroundHeight() + 1; yBlock <= chunkMaxY; ++yBlock)
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

        final int dimension = plotManager.getLevelSettings().getDimension();
        final int chunkMinY = LevelUtils.getChunkMinY(dimension);

        if(yBlock == chunkMinY)
            return levelSettings.getFirstLayerState();
        else if(yBlock < levelSettings.getGroundHeight() + chunkMinY)
            return switch(shapeType) {
                case PLOT -> levelSettings.getMiddleLayerState();
                case WALL -> levelSettings.getWallFillingState();
                case ROAD -> levelSettings.getRoadFillingState();
            };
        else if(yBlock == levelSettings.getGroundHeight() + chunkMinY)
            return switch(shapeType) {
                case PLOT -> levelSettings.getLastLayerState();
                case WALL -> levelSettings.getWallFillingState();
                case ROAD -> levelSettings.getRoadState();
            };
        else if(yBlock == levelSettings.getGroundHeight() + 1 + chunkMinY)
            if(shapeType == ShapeType.WALL) return levelSettings.getWallPlotState();

        return BlockState.AIR;
    }

    @Override
    public void populateChunk(int chunkX, int chunkZ) {
        final FullChunk fullChunk = this.chunkManager.getChunk(chunkX, chunkZ);
        fullChunk.setPopulated();
    }

    private void placeChunkSchematic(PlotManager plotManager, Schematic schematic, FullChunk fullChunk,
                                     ShapeType[] shapes, Allowed<ShapeType> allowedShapes, Integer minX, Integer minZ, Integer maxX, Integer maxZ) {
        final Set<Vector3> handledVectors = new HashSet<>();
        final int fullX = fullChunk.getX() << 4;
        final int fullZ = fullChunk.getZ() << 4;

        for(int blockX = fullX; blockX < fullX + 16; blockX++) {
            if(minX != null && (blockX < minX || blockX > maxX)) continue;
            for(int blockZ = fullZ; blockZ < fullZ + 16; blockZ++) {
                if(minZ != null && (blockZ < minZ || blockZ > maxZ)) continue;

                final Vector3 plotArea = this.getPlotAreaStart(plotManager, blockX, blockZ);
                if(handledVectors.contains(plotArea)) continue;

                handledVectors.add(plotArea);
                schematic.buildInChunk(plotArea, fullChunk, shapes, allowedShapes, minX, minZ, maxX, maxZ);
            }
        }
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
        final PlotLevelSettings levelSettings = plotManager.getLevelSettings();
        final int totalSize = levelSettings.getTotalSize();
        final int xPart = x / totalSize;
        final int zPart = z / totalSize;
        return new Vector3(
                (x < 0 ? xPart - 1 : xPart) * totalSize,
                LevelUtils.getChunkMinY(levelSettings.getDimension()),
                (z < 0 ? zPart - 1 : zPart) * totalSize
        );
    }

    public Vector3 getPlotAreaEnd(PlotManager plotManager, int x, int z) {
        final PlotLevelSettings levelSettings = plotManager.getLevelSettings();
        final int totalSize = levelSettings.getTotalSize();
        final int xPart = x / totalSize;
        final int zPart = z / totalSize;
        return new Vector3(
                (x < 0 ? xPart : xPart + 1) * totalSize,
                LevelUtils.getChunkMaxY(levelSettings.getDimension()),
                (z < 0 ? zPart : zPart + 1) * totalSize
        );
    }

    public Vector3[] getPlotArea(PlotManager plotManager, int x, int z) {
        return new Vector3[]{this.getPlotAreaStart(plotManager, x, z), this.getPlotAreaEnd(plotManager, x, z)};
    }

}
