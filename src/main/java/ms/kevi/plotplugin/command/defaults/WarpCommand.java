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

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
public class WarpCommand extends SubCommand {

    public WarpCommand(PlotPlugin plugin, PlotCommand parent) {
        super(plugin, parent, "warp", "w");
        this.addParameter(CommandParameter.newType("id", CommandParamType.STRING));
    }

    @Override
    public void execute(Player player, String[] args) {
        this.plugin.getPlotManager(player.getLevel()).whenCompleteAsync((plotManager, throwable) -> {
            if(plotManager == null && this.plugin.getDefaultPlotLevel() == null || plotManager == null && (plotManager = this.plugin.getPlotManager(this.plugin.getDefaultPlotLevel()).join()) == null) {
                player.sendMessage(this.translate(player, TranslationKey.NO_PLOT_WORLD));
                return;
            }

            final String[] plotIds = args.length > 0 ?
                    args[0].split(";").length > 1 ? args[0].split(";") :
                            args[0].split(":").length > 1 ? args[0].split(":") :
                                    args[0].split(",").length > 1 ? args[0].split(",") :
                                            new String[0] : new String[0];

            final Integer plotX = plotIds.length != 0 ? Utils.parseIntegerWithNull(plotIds[0]) : null;
            final Integer plotZ = plotIds.length != 0 ? Utils.parseIntegerWithNull(plotIds[1]) : null;

            if(plotX == null || plotZ == null) {
                player.sendMessage(this.translate(player, TranslationKey.NO_PLOT_ID));
                return;
            }

            final Plot plot = plotManager.getPlotById(plotX, plotZ).join();

            if(!plot.hasOwner() && !player.hasPermission("plot.command.warp.free")) {
                player.sendMessage(this.translate(player, TranslationKey.WARP_FAILURE_FREE));
                return;
            }

            if((!plot.isDenied(player.getUniqueId()) && !plot.isDenied(Utils.UUID_EVERYONE)) || player.hasPermission("plot.admin.nodeny")) {
                plotManager.teleportPlayerToPlot(player, plot);
                player.sendMessage(this.translate(player, TranslationKey.WARP_SUCCESS, (plotX + ";" + plotZ)));
                return;
            }

            player.sendMessage(this.translate(player, TranslationKey.WARP_FAILURE));
        });
    }

}
