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

import cn.nukkit.block.Block;
import cn.nukkit.block.BlockState;
import cn.nukkit.level.Level;
import cn.nukkit.level.biome.BiomeID;
import cn.nukkit.registry.Registries;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
@Getter
@Setter
public class PlotLevelSettings {

    private int dimension = Level.DIMENSION_OVERWORLD;

    private int plotBiome = BiomeID.PLAINS;
    private int roadBiome = BiomeID.PLAINS;

    private int groundHeight = 64;
    private int plotSize = 35;
    private int roadSize = 7;

    private String firstLayerBlockId = Block.BEDROCK;
    private short firstLayerBlockMeta = 0;

    private String middleLayerBlockId = Block.DIRT;
    private short middleLayerBlockMeta = 0;

    private String lastLayerBlockId = Block.GRASS;
    private short lastLayerBlockMeta = 0;

    private String wallFillingBlockId = Block.STONE;
    private short wallFillingBlockMeta = 0;

    private String wallPlotBlockId = Block.STONE_BLOCK_SLAB;
    private short wallPlotBlockMeta = 0;

    private String claimPlotBlockId = Block.STONE_BLOCK_SLAB;
    private short claimPlotBlockMeta = 1;

    private String roadBlockId = Block.OAK_PLANKS;
    private short roadBlockMeta = 0;

    private String roadFillingBlockId = Block.DIRT;
    private short roadFillingBlockMeta = 0;

    public BlockState getFirstLayerState() {
        return Registries.BLOCK.getBlockProperties(firstLayerBlockId).getBlockState(this.firstLayerBlockMeta);
    }

    public BlockState getMiddleLayerState() {
        return Registries.BLOCK.getBlockProperties(middleLayerBlockId).getBlockState(this.middleLayerBlockMeta);
    }

    public BlockState getLastLayerState() {
        return Registries.BLOCK.getBlockProperties(lastLayerBlockId).getBlockState(this.lastLayerBlockMeta);
    }

    public BlockState getWallFillingState() {
        return Registries.BLOCK.getBlockProperties(wallFillingBlockId).getBlockState(this.wallFillingBlockMeta);
    }

    public BlockState getWallPlotState() {
        return Registries.BLOCK.getBlockProperties(wallPlotBlockId).getBlockState(this.wallPlotBlockMeta);
    }

    public BlockState getClaimPlotState() {
        return Registries.BLOCK.getBlockProperties(claimPlotBlockId).getBlockState(this.claimPlotBlockMeta);
    }

    public BlockState getRoadState() {
        return Registries.BLOCK.getBlockProperties(roadBlockId).getBlockState(this.roadBlockMeta);
    }

    public BlockState getRoadFillingState() {
        return Registries.BLOCK.getBlockProperties(roadFillingBlockId).getBlockState(this.roadFillingBlockMeta);
    }

    public int getTotalSize() {
        return this.plotSize + this.roadSize;
    }

    public void fromMap(Map<String, Object> map) {
        for (Field field : this.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Class<?> type = field.getType();
                if (field.getType() == Integer.class || type == int.class) {
                    Number number = (Number) map.get(field.getName());
                    field.set(this, number.intValue());
                } else if (type == Short.class || type == short.class) {
                    Number number = (Number) map.get(field.getName());
                    field.set(this, number.shortValue());
                } else if (type == Byte.class || type == byte.class) {
                    Number number = (Number) map.get(field.getName());
                    field.set(this, number.byteValue());
                } else if (type == Double.class || type == double.class) {
                    Number number = (Number) map.get(field.getName());
                    field.set(this, number.doubleValue());
                } else if (type == Float.class || type == float.class) {
                    Number number = (Number) map.get(field.getName());
                    field.set(this, number.floatValue());
                } else if (type == Long.class || type == long.class) {
                    Number number = (Number) map.get(field.getName());
                    field.set(this, number.longValue());
                } else {
                    field.set(this, map.get(field.getName()));
                }
            } catch (IllegalAccessException ignored) {
            }
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        for (Field field : this.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                map.put(field.getName(), field.get(this));
            } catch (IllegalAccessException ignored) {
            }
        }
        return map;
    }
}
