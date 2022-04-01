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
import cn.nukkit.blockstate.BlockState;
import cn.nukkit.level.Level;
import cn.nukkit.level.biome.EnumBiome;
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

    private int plotBiome = EnumBiome.PLAINS.id;
    private int roadBiome = EnumBiome.PLAINS.id;

    private int groundHeight = 64;
    private int plotSize = 35;
    private int roadSize = 7;

    private int firstLayerBlockId = Block.BEDROCK;
    private int firstLayerBlockMeta = 0;

    private int middleLayerBlockId = Block.DIRT;
    private int middleLayerBlockMeta = 0;

    private int lastLayerBlockId = Block.GRASS;
    private int lastLayerBlockMeta = 0;

    private int wallFillingBlockId = Block.STONE;
    private int wallFillingBlockMeta = 0;

    private int wallPlotBlockId = Block.STONE_SLAB;
    private int wallPlotBlockMeta = 0;

    private int claimPlotBlockId = Block.STONE_SLAB;
    private int claimPlotBlockMeta = 1;

    private int roadBlockId = Block.PLANKS;
    private int roadBlockMeta = 0;

    private int roadFillingBlockId = Block.DIRT;
    private int roadFillingBlockMeta = 0;

    public BlockState getFirstLayerState() {
        return BlockState.of(this.firstLayerBlockId, this.firstLayerBlockMeta);
    }

    public BlockState getMiddleLayerState() {
        return BlockState.of(this.middleLayerBlockId, this.middleLayerBlockMeta);
    }

    public BlockState getLastLayerState() {
        return BlockState.of(this.lastLayerBlockId, this.lastLayerBlockMeta);
    }

    public BlockState getWallFillingState() {
        return BlockState.of(this.wallFillingBlockId, this.wallFillingBlockMeta);
    }

    public BlockState getWallPlotState() {
        return BlockState.of(this.wallPlotBlockId, this.wallPlotBlockMeta);
    }

    public BlockState getClaimPlotState() {
        return BlockState.of(this.claimPlotBlockId, this.claimPlotBlockMeta);
    }

    public BlockState getRoadState() {
        return BlockState.of(this.roadBlockId, this.roadBlockMeta);
    }

    public BlockState getRoadFillingState() {
        return BlockState.of(this.roadFillingBlockId, this.roadFillingBlockMeta);
    }

    public int getTotalSize() {
        return this.plotSize + this.roadSize;
    }

    public void fromMap(Map<String, Object> map) {
        for(Field field : this.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                field.set(this, map.get(field.getName()));
            } catch(IllegalAccessException ignored) {
            }
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        for(Field field : this.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                map.put(field.getName(), field.get(this));
            } catch(IllegalAccessException ignored) {
            }
        }
        return map;
    }

}
