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
import cn.nukkit.command.data.CommandParamType;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.FullChunk;
import de.kcodeyt.plotplugin.PlotPlugin;
import de.kcodeyt.plotplugin.command.SubCommand;
import de.kcodeyt.plotplugin.generator.PlotGenerator;
import de.kcodeyt.plotplugin.manager.PlotManager;
import de.kcodeyt.plotplugin.util.Utils;
import de.kcodeyt.plotplugin.util.async.TaskExecutor;

public class RegenAllRoadsCommand extends SubCommand {

    public RegenAllRoadsCommand(PlotPlugin plugin) {
        super(plugin, "regenallroads");
        this.setPermission("plot.command.admin.regenallroads");
        this.addParameter(CommandParameter.newType("radius", CommandParamType.INT));
    }

    @Override
    public boolean execute(Player player, String[] args) {
        final PlotManager plotManager = this.plugin.getPlotManager(player.getLevel());
        if(plotManager == null) {
            player.sendMessage(this.translate("no-plot-world"));
            return false;
        }

        final int chunkRadius = Utils.parseInteger(args.length > 0 ? args[0] : "32", 32);
        final int fullSize = (chunkRadius * 2 * chunkRadius * 2) + 4;
        final Level level = player.getLevel();
        final PlotGenerator plotGenerator = (PlotGenerator) level.getGenerator();

        final int pChunkX = player.getChunkX();
        final int pChunkZ = player.getChunkZ();

        TaskExecutor.executeAsync(() -> {
            final long start = System.currentTimeMillis();
            int index = 0;
            for(int chunkX = -chunkRadius; chunkX <= chunkRadius; chunkX++) {
                for(int chunkZ = -chunkRadius; chunkZ <= chunkRadius; chunkZ++) {
                    index++;
                    final FullChunk fullChunk = level.getChunk(pChunkX + chunkX, pChunkZ + chunkZ, false);
                    if(fullChunk != null) plotGenerator.regenerateChunk(plotManager, fullChunk, true);
                }
                if(index % 5 == 0)
                    System.out.println("REGENALLROADS STATUS: [" + ((int) (Math.abs((float) index / fullSize) * 100)) + "/100 %] (" + (System.currentTimeMillis() - start) + "ms)");
            }

            System.out.println("REGENALLROADS FINISHED Took " + (System.currentTimeMillis() - start) + "ms!");
            player.sendMessage("Finished Regenallroads Task!");
        });

        player.sendMessage("Starting Regenallroads Task!");
        return true;
    }

}
