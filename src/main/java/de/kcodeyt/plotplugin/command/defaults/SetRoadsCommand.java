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

package de.kcodeyt.plotplugin.command.defaults;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.blockstate.BlockState;
import cn.nukkit.level.Level;
import cn.nukkit.math.Vector3;
import de.kcodeyt.plotplugin.PlotPlugin;
import de.kcodeyt.plotplugin.command.SubCommand;
import de.kcodeyt.plotplugin.generator.PlotGenerator;
import de.kcodeyt.plotplugin.manager.PlotManager;
import de.kcodeyt.plotplugin.schematic.Schematic;
import de.kcodeyt.plotplugin.schematic.SchematicBlock;
import de.kcodeyt.plotplugin.util.ChunkVector;
import de.kcodeyt.plotplugin.util.ShapeType;
import de.kcodeyt.plotplugin.util.async.TaskExecutor;

import java.util.LinkedHashMap;
import java.util.Map;

public class SetRoadsCommand extends SubCommand {

    public SetRoadsCommand(PlotPlugin plugin) {
        super(plugin, "setroads");
        this.setPermission("plot.command.admin.setroads");
    }

    @Override
    public boolean execute(Player player, String[] args) {
        final PlotManager plotManager = this.plugin.getPlotManager(player.getLevel());
        if(plotManager == null) {
            player.sendMessage(this.translate("no-plot-world"));
            return false;
        }

        TaskExecutor.executeAsync(() -> {
            final Level level = player.getLevel();
            final PlotGenerator plotGenerator = (PlotGenerator) level.getGenerator();

            final int playerX = player.getFloorX();
            final int playerZ = player.getFloorZ();

            final Schematic schematic = new Schematic();
            final Vector3[] plotArea = plotGenerator.getPlotArea(plotManager, playerX, playerZ);
            final Vector3 startPos = plotArea[0];
            final Vector3 endPos = plotArea[1];
            final Map<ChunkVector, ShapeType[]> chunkShapes = new LinkedHashMap<>();

            for(int x = startPos.getFloorX(); x <= endPos.getFloorX(); x++) {
                for(int z = startPos.getFloorZ(); z <= endPos.getFloorZ(); z++) {
                    final ChunkVector chunkVector = new ChunkVector(x >> 4, z >> 4);

                    for(int y = startPos.getFloorY(); y <= endPos.getFloorY(); y++) {
                        final Vector3 defaultBlockVector = new Vector3(x, y, z);
                        final Vector3 blockVector = new Vector3(x - startPos.getFloorX(), y, z - startPos.getFloorZ());

                        final BlockState blockState0 = level.getBlockStateAt(x, y, z, 0);
                        final BlockState blockState1 = level.getBlockStateAt(x, y, z, 1);

                        if(blockState1.getBlockId() == Block.AIR) {
                            final ShapeType[] shapes;
                            if(!chunkShapes.containsKey(chunkVector))
                                chunkShapes.put(chunkVector, shapes = plotManager.getShapes(chunkVector.getX() << 4, chunkVector.getZ() << 4));
                            else shapes = chunkShapes.get(chunkVector);

                            if(plotGenerator.getDefaultBlockStateAt(plotManager, shapes, defaultBlockVector).equals(blockState0))
                                continue;
                        }

                        schematic.addBlock(blockVector, new SchematicBlock(
                                blockState0,
                                blockState1
                        ));

                        final BlockEntity blockEntity = level.getBlockEntity(new Vector3(x, y, z));
                        if(blockEntity != null)
                            schematic.addBlockEntity(blockVector.asBlockVector3(), blockEntity.getSaveId(), blockEntity.namedTag.copy().remove("x").remove("y").remove("z"));
                    }
                }
            }

            if(schematic.isEmpty()) {
                if(plotManager.getPlotSchematic().getSchematic() != null) {
                    plotManager.getPlotSchematic().remove(plotManager.getPlotSchematicFile());
                    player.sendMessage(this.translate("setroads-road-removed"));
                    return;
                }

                player.sendMessage(this.translate("setroads-no-road-found"));
                return;
            }

            plotManager.getPlotSchematic().init(schematic);
            plotManager.getPlotSchematic().save(plotManager.getPlotSchematicFile());
            player.sendMessage(this.translate("setroads-finished"));
        });

        player.sendMessage(this.translate("setroads-starting"));
        return true;
    }

}
