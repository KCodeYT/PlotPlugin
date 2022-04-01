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
import ms.kevi.plotplugin.PlotPlugin;
import ms.kevi.plotplugin.command.PlotCommand;
import ms.kevi.plotplugin.command.SubCommand;
import ms.kevi.plotplugin.lang.TranslationKey;
import ms.kevi.plotplugin.util.Plot;
import ms.kevi.plotplugin.util.Utils;

import java.util.UUID;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
public class DenyCommand extends SubCommand {

    public DenyCommand(PlotPlugin plugin, PlotCommand parent) {
        super(plugin, parent, "deny");
        this.addParameter(CommandParameter.newType("player", CommandParamType.TARGET));
    }

    @Override
    public void execute(Player player, String[] args) {
        this.plugin.getPlotManager(player.getLevel()).whenCompleteAsync((plotManager, throwable) -> {
            final Plot plot;
            if(plotManager == null || (plot = plotManager.getMergedPlot(player.getFloorX(), player.getFloorZ()).join()) == null) {
                player.sendMessage(this.translate(player, TranslationKey.NO_PLOT));
                return;
            }

            final String targetName = (args.length > 0 ? args[0] : "").trim();
            final UUID targetId = this.plugin.getUniqueIdByName(targetName);
            final boolean isEveryone = targetId != null && targetId.equals(Utils.UUID_EVERYONE);
            final Player target = targetId != null ? player.getServer().getPlayer(targetId).orElse(null) : null;

            if(targetName.equalsIgnoreCase(player.getName()) && !player.hasPermission("plot.command.admin.deny")) {
                player.sendMessage(this.translate(player, TranslationKey.PLAYER_SELF));
                return;
            }

            if(targetName.trim().isEmpty() || targetId == null) {
                player.sendMessage(this.translate(player, TranslationKey.NO_PLAYER));
                return;
            }

            if(!player.hasPermission("plot.command.admin.deny") && !plot.isOwner(player.getUniqueId())) {
                player.sendMessage(this.translate(player, TranslationKey.NO_PLOT_OWNER));
                return;
            }

            if(plot.denyPlayer(targetId)) {
                plotManager.addPlot(plot).join();
                plotManager.savePlots();

                if(target != null || isEveryone) {
                    if(!isEveryone) {
                        final Plot plot1 = plotManager.getMergedPlot(target.getFloorX(), target.getFloorZ()).join();
                        if(plot1 != null && (plot.getId().equals(plot1.getId())) && !target.hasPermission("plot.admin.nodeny"))
                            plotManager.teleportPlayerToPlot(target, plot1);
                    } else {
                        for(Player onlinePlayer : this.plugin.getServer().getOnlinePlayers().values()) {
                            final Plot plot1 = plotManager.getMergedPlot(onlinePlayer.getFloorX(), onlinePlayer.getFloorZ()).join();
                            if(!plot.isOwner(onlinePlayer.getUniqueId()) && !plot.isHelper(onlinePlayer.getUniqueId()) && plot1 != null && (plot.getId().equals(plot1.getId())) && !onlinePlayer.hasPermission("plot.admin.nodeny"))
                                plotManager.teleportPlayerToPlot(onlinePlayer, plot1);
                        }
                    }
                }

                player.sendMessage(this.translate(player, TranslationKey.DENY_SUCCESS, this.plugin.getCorrectName(targetId)));
            } else {
                player.sendMessage(this.translate(player, TranslationKey.DENY_FAILURE, this.plugin.getCorrectName(targetId)));
            }
        });
    }

}
