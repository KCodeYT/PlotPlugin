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
import de.kcodeyt.plotplugin.PlotPlugin;
import de.kcodeyt.plotplugin.command.SubCommand;
import de.kcodeyt.plotplugin.manager.PlotManager;
import de.kcodeyt.plotplugin.util.Plot;
import de.kcodeyt.plotplugin.util.Utils;

import java.util.UUID;

public class DenyCommand extends SubCommand {

    public DenyCommand(PlotPlugin plugin) {
        super(plugin, "deny");
        this.addParameter(CommandParameter.newType("player", CommandParamType.TARGET));
    }

    @Override
    public boolean execute(Player player, String[] args) {
        final PlotManager plotManager = this.plugin.getPlotManager(player.getLevel());
        final Plot plot;
        if(plotManager == null || (plot = plotManager.getMergedPlot(player.getFloorX(), player.getFloorZ())) == null) {
            player.sendMessage(this.translate("no-plot"));
            return false;
        }

        final String targetName = (args.length > 0 ? args[0] : "").trim();
        final UUID targetId = this.plugin.getUniqueIdByName(targetName);
        final boolean isEveryone = targetId != null && targetId.equals(Utils.UUID_EVERYONE);
        final Player target = targetId != null ? player.getServer().getPlayer(targetId).orElse(null) : null;

        if(targetName.equalsIgnoreCase(player.getName()) && !player.hasPermission("plot.command.admin.deny")) {
            player.sendMessage(this.translate("player-self"));
            return false;
        }

        if(targetName.trim().isEmpty() || targetId == null) {
            player.sendMessage(this.translate("no-player"));
            return false;
        }

        if(!player.hasPermission("plot.command.admin.deny") && !plot.isOwner(player.getUniqueId())) {
            player.sendMessage(this.translate("no-plot-owner"));
            return false;
        }

        if(plot.denyPlayer(targetId)) {
            plotManager.savePlots();

            if(target != null || isEveryone) {
                if(!isEveryone) {
                    final Plot plot1 = plotManager.getMergedPlot(target.getFloorX(), target.getFloorZ());
                    if(plot1 != null && (plot.getPlotVector().equals(plot1.getPlotVector())) && !target.hasPermission("plot.admin.nodeny"))
                        plotManager.teleportPlayerToPlot(target, plot1);
                } else {
                    for(Player onlinePlayer : this.plugin.getServer().getOnlinePlayers().values()) {
                        final Plot plot1 = plotManager.getMergedPlot(onlinePlayer.getFloorX(), onlinePlayer.getFloorZ());
                        if(!plot.isOwner(onlinePlayer.getUniqueId()) && !plot.isHelper(onlinePlayer.getUniqueId()) && plot1 != null && (plot.getPlotVector().equals(plot1.getPlotVector())) && !onlinePlayer.hasPermission("plot.admin.nodeny"))
                            plotManager.teleportPlayerToPlot(onlinePlayer, plot1);
                    }
                }
            }

            player.sendMessage(this.translate("deny-success", this.plugin.getCorrectName(targetId)));
            return true;
        } else {
            player.sendMessage(this.translate("deny-failure", this.plugin.getCorrectName(targetId)));
            return false;
        }
    }

}
