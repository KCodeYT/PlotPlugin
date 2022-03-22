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
import cn.nukkit.math.BlockFace;
import de.kcodeyt.plotplugin.PlotPlugin;
import de.kcodeyt.plotplugin.command.PlotCommand;
import de.kcodeyt.plotplugin.command.SubCommand;
import de.kcodeyt.plotplugin.event.PlotMergeEvent;
import de.kcodeyt.plotplugin.event.PlotPreMergeEvent;
import de.kcodeyt.plotplugin.lang.TranslationKey;
import de.kcodeyt.plotplugin.manager.PlotManager;
import de.kcodeyt.plotplugin.util.Plot;

/**
 * @author Kevims KCodeYT
 */
public class MergeCommand extends SubCommand {

    public MergeCommand(PlotPlugin plugin, PlotCommand parent) {
        super(plugin, parent, "merge");
        this.setPermission("plot.command.admin.merge");
    }

    @Override
    public boolean execute(Player player, String[] args) {
        final PlotManager plotManager = this.plugin.getPlotManager(player.getLevel());
        final Plot plot;
        if(plotManager == null || (plot = plotManager.getMergedPlot(player.getFloorX(), player.getFloorZ())) == null) {
            player.sendMessage(this.translate(player, TranslationKey.NO_PLOT));
            return false;
        }

        final BlockFace blockFace = player.getDirection();
        final int dir = blockFace == BlockFace.NORTH ? 0 : blockFace == BlockFace.EAST ? 1 : blockFace == BlockFace.SOUTH ? 2 : blockFace == BlockFace.WEST ? 3 : -1;
        final Plot rPlot = plotManager.getPlotById(plot.getRelative(dir));

        if(player.hasPermission("plot.command.admin.merge") || (plot.isOwner(player.getUniqueId()) && rPlot.isOwner(player.getUniqueId()))) {
            if(plot.isMerged(dir)) {
                player.sendMessage(this.translate(player, TranslationKey.MERGE_FAILURE_ALREADY_MERGED));
                return false;
            }

            final PlotPreMergeEvent plotPreMergeEvent = new PlotPreMergeEvent(player, plot, dir);
            this.plugin.getServer().getPluginManager().callEvent(plotPreMergeEvent);
            if(plotPreMergeEvent.isCancelled()) return false;

            if(!plotManager.startMerge(plot, dir)) {
                player.sendMessage(this.translate(player, TranslationKey.MERGE_FAILURE_NO_PLOTS_FOUND));
                return false;
            }

            final PlotMergeEvent plotMergeEvent = new PlotMergeEvent(player, plot, dir);
            this.plugin.getServer().getPluginManager().callEvent(plotMergeEvent);

            player.sendMessage(this.translate(player, TranslationKey.MERGE_SUCCESS));
            return true;
        } else {
            player.sendMessage(this.translate(player, TranslationKey.MERGE_FAILURE_OWNER));
            return false;
        }
    }

}
