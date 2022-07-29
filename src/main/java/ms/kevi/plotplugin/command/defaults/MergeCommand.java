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
import cn.nukkit.math.NukkitMath;
import ms.kevi.plotplugin.PlotPlugin;
import ms.kevi.plotplugin.command.PlotCommand;
import ms.kevi.plotplugin.command.SubCommand;
import ms.kevi.plotplugin.event.PlotMergeEvent;
import ms.kevi.plotplugin.event.PlotPreMergeEvent;
import ms.kevi.plotplugin.lang.TranslationKey;
import ms.kevi.plotplugin.manager.PlotManager;
import ms.kevi.plotplugin.util.Plot;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
public class MergeCommand extends SubCommand {

    public MergeCommand(PlotPlugin plugin, PlotCommand parent) {
        super(plugin, parent, "merge");
        this.setPermission("plot.command.merge");
    }

    @Override
    public boolean execute(Player player, String[] args) {
        final PlotManager plotManager = this.plugin.getPlotManager(player.getLevel());
        final Plot plot;
        if(plotManager == null || (plot = plotManager.getMergedPlot(player.getFloorX(), player.getFloorZ())) == null) {
            player.sendMessage(this.translate(player, TranslationKey.NO_PLOT));
            return false;
        }

        final int dir = (NukkitMath.floorDouble((player.getYaw() * 4 / 360) + 0.5) - 2) & 3;
        final Set<Plot> plotsToMerge = plotManager.calculatePlotsToMerge(plot, dir);

        boolean isOwner = player.hasPermission("plot.command.admin.merge");
        if(!isOwner) {
            isOwner = true;
            for(Plot plotToMerge : plotsToMerge) {
                if(!plotToMerge.isOwner(player.getUniqueId())) {
                    isOwner = false;
                    break;
                }
            }
        }

        if(isOwner) {
            if(!player.hasPermission("plot.merge.unlimited") && !player.isOp()) {
                if(!player.isOp()) {
                    int maxLimit = -1;
                    for(String permission : player.getEffectivePermissions().keySet()) {
                        if(permission.startsWith("plot.merge.limit.")) {
                            try {
                                final String limitStr = permission.substring("plot.merge.limit.".length());
                                if(limitStr.isBlank()) continue;
                                final int limit = Integer.parseInt(limitStr);

                                if(limit > maxLimit) maxLimit = limit;
                            } catch(NumberFormatException ignored) {
                            }
                        }
                    }

                    if(maxLimit > 0 && plotsToMerge.size() > maxLimit) {
                        player.sendMessage(this.translate(player, TranslationKey.MERGE_FAILURE_TOO_MANY, plotsToMerge.size()));
                        return false;
                    }
                }
            }

            if(plot.isMerged(dir)) {
                player.sendMessage(this.translate(player, TranslationKey.MERGE_FAILURE_ALREADY_MERGED));
                return false;
            }

            final PlotPreMergeEvent plotPreMergeEvent = new PlotPreMergeEvent(player, plot, dir, plotsToMerge);
            this.plugin.getServer().getPluginManager().callEvent(plotPreMergeEvent);
            if(plotPreMergeEvent.isCancelled()) return false;

            if(!plotManager.startMerge(plot, plotsToMerge)) {
                player.sendMessage(this.translate(player, TranslationKey.MERGE_FAILURE_NO_PLOTS_FOUND));
                return false;
            }

            final PlotMergeEvent plotMergeEvent = new PlotMergeEvent(player, plot, plotsToMerge);
            this.plugin.getServer().getPluginManager().callEvent(plotMergeEvent);

            player.sendMessage(this.translate(player, TranslationKey.MERGE_SUCCESS));
            return true;
        } else {
            player.sendMessage(this.translate(player, TranslationKey.MERGE_FAILURE_OWNER));
            return false;
        }
    }

}
