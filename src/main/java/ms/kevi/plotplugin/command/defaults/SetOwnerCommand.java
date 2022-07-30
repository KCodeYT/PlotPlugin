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
import ms.kevi.plotplugin.manager.PlotManager;
import ms.kevi.plotplugin.util.Plot;

import java.util.UUID;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
public class SetOwnerCommand extends SubCommand {

    public SetOwnerCommand(PlotPlugin plugin, PlotCommand parent) {
        super(plugin, parent, "setowner");
        this.setPermission("plot.command.setowner");
        this.addParameter(CommandParameter.newType("player", CommandParamType.TARGET));
    }

    @Override
    public boolean execute(Player player, String[] args) {
        final PlotManager plotManager = this.plugin.getPlotManager(player.getLevel());
        final Plot plot;
        if(plotManager == null || (plot = plotManager.getMergedPlot(player.getFloorX(), player.getFloorZ())) == null) {
            player.sendMessage(this.translate(player, TranslationKey.NO_PLOT));
            return false;
        }

        final String targetName = (args.length > 0 ? args[0] : "").trim();
        final UUID targetId = this.plugin.getUniqueIdByName(targetName, false);
        final Player target = targetId != null ? player.getServer().getPlayer(targetId).orElse(null) : null;

        if(!plot.isOwner(player.getUniqueId()) && !player.hasPermission("plot.command.admin.setowner")) {
            player.sendMessage(this.translate(player, TranslationKey.NO_PLOT_OWNER));
            return false;
        }

        if(targetName.trim().isEmpty() || targetId == null) {
            player.sendMessage(this.translate(player, TranslationKey.NO_PLAYER));
            return false;
        }

        if(target == null) {
            player.sendMessage(this.translate(player, TranslationKey.PLAYER_NOT_ONLINE));
            return false;
        }

        if(targetName.equalsIgnoreCase(player.getName()) && !player.hasPermission("plot.command.admin.setowner")) {
            player.sendMessage(this.translate(player, TranslationKey.PLAYER_SELF));
            return false;
        }

        final int ownedPlots = plotManager.getPlotsByOwner(targetId).size();
        if(!target.isOp() && !player.hasPermission("plot.command.admin.setowner")) {
            int maxLimit = -1;
            for(String permission : target.getEffectivePermissions().keySet()) {
                if(permission.startsWith("plot.limit.")) {
                    try {
                        final String limitStr = permission.substring("plot.limit.".length());
                        if(limitStr.isBlank()) continue;
                        final int limit = Integer.parseInt(limitStr);

                        if(limit > maxLimit) maxLimit = limit;
                    } catch(NumberFormatException ignored) {
                    }
                }
            }

            if(maxLimit > 0 && ownedPlots >= maxLimit) {
                player.sendMessage(this.translate(player, TranslationKey.SETOWNER_FAILURE_TOO_MANY));
                return false;
            }
        }

        plot.setOwner(targetId);
        plotManager.savePlots();

        target.sendMessage(this.translate(target, TranslationKey.SETOWNER_SUCCESS_TARGET, plot.getId()));
        player.sendMessage(this.translate(player, TranslationKey.SETOWNER_SUCCESS, this.plugin.getCorrectName(targetId)));
        return true;
    }

}
