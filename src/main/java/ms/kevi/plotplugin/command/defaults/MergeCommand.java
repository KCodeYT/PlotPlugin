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
import cn.nukkit.math.BlockFace;
import ms.kevi.plotplugin.PlotPlugin;
import ms.kevi.plotplugin.command.PlotCommand;
import ms.kevi.plotplugin.command.SubCommand;
import ms.kevi.plotplugin.event.PlotMergeEvent;
import ms.kevi.plotplugin.event.PlotPreMergeEvent;
import ms.kevi.plotplugin.lang.TranslationKey;
import ms.kevi.plotplugin.util.Plot;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
public class MergeCommand extends SubCommand {

    public MergeCommand(PlotPlugin plugin, PlotCommand parent) {
        super(plugin, parent, "merge");
        this.setPermission("plot.command.admin.merge");
    }

    @Override
    public void execute(Player player, String[] args) {
        this.plugin.getPlotManager(player.getLevel()).whenCompleteAsync((plotManager, throwable) -> {
            final Plot plot;
            if(plotManager == null || (plot = plotManager.getMergedPlot(player.getFloorX(), player.getFloorZ()).join()) == null) {
                player.sendMessage(this.translate(player, TranslationKey.NO_PLOT));
                return;
            }

            final BlockFace blockFace = player.getDirection();
            final int dir = blockFace == BlockFace.NORTH ? 0 : blockFace == BlockFace.EAST ? 1 : blockFace == BlockFace.SOUTH ? 2 : blockFace == BlockFace.WEST ? 3 : -1;
            final Plot rPlot = plotManager.getPlotById(plot.getRelative(dir)).join();

            if(player.hasPermission("plot.command.admin.merge") || (plot.isOwner(player.getUniqueId()) && rPlot.isOwner(player.getUniqueId()))) {
                if(plot.isMerged(dir)) {
                    player.sendMessage(this.translate(player, TranslationKey.MERGE_FAILURE_ALREADY_MERGED));
                    return;
                }

                final PlotPreMergeEvent plotPreMergeEvent = new PlotPreMergeEvent(player, plot, dir);
                this.plugin.getServer().getPluginManager().callEvent(plotPreMergeEvent);
                if(plotPreMergeEvent.isCancelled()) return;

                if(!plotManager.startMerge(plot, dir).join()) {
                    player.sendMessage(this.translate(player, TranslationKey.MERGE_FAILURE_NO_PLOTS_FOUND));
                    return;
                }

                final PlotMergeEvent plotMergeEvent = new PlotMergeEvent(player, plot, dir);
                this.plugin.getServer().getPluginManager().callEvent(plotMergeEvent);

                player.sendMessage(this.translate(player, TranslationKey.MERGE_SUCCESS));
            } else {
                player.sendMessage(this.translate(player, TranslationKey.MERGE_FAILURE_OWNER));
            }
        });
    }

}
