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

package ms.kevi.plotplugin.command.defaults;

import cn.nukkit.Player;
import cn.nukkit.command.data.CommandParamType;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.FullChunk;
import ms.kevi.plotplugin.PlotPlugin;
import ms.kevi.plotplugin.command.PlotCommand;
import ms.kevi.plotplugin.command.SubCommand;
import ms.kevi.plotplugin.generator.PlotGenerator;
import ms.kevi.plotplugin.lang.TranslationKey;
import ms.kevi.plotplugin.util.Utils;
import ms.kevi.plotplugin.util.async.TaskExecutor;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
public class RegenAllRoadsCommand extends SubCommand {

    public RegenAllRoadsCommand(PlotPlugin plugin, PlotCommand parent) {
        super(plugin, parent, "regenallroads");
        this.setPermission("plot.command.admin.regenallroads");
        this.addParameter(CommandParameter.newType("radius", CommandParamType.INT));
    }

    @Override
    public void execute(Player player, String[] args) {
        this.plugin.getPlotManager(player.getLevel()).whenCompleteAsync((plotManager, throwable) -> {
            if(plotManager == null) {
                player.sendMessage(this.translate(player, TranslationKey.NO_PLOT_WORLD));
                return;
            }

            final int chunkRadius = Utils.parseInteger(args.length > 0 ? args[0] : "32", 32);
            final Level level = player.getLevel();
            final PlotGenerator plotGenerator = (PlotGenerator) level.getGenerator();

            final int pChunkX = player.getChunkX();
            final int pChunkZ = player.getChunkZ();

            TaskExecutor.executeAsync(() -> {
                for(int chunkX = -chunkRadius; chunkX <= chunkRadius; chunkX++) {
                    for(int chunkZ = -chunkRadius; chunkZ <= chunkRadius; chunkZ++) {
                        final FullChunk fullChunk = level.getChunk(pChunkX + chunkX, pChunkZ + chunkZ, false);
                        if(fullChunk != null) plotGenerator.regenerateChunk(plotManager, fullChunk, true);
                    }
                }

                player.sendMessage(this.translate(player, TranslationKey.REGENALLROADS_FINISHED));
            });

            player.sendMessage(this.translate(player, TranslationKey.REGENALLROADS_START));
        });
    }

}
