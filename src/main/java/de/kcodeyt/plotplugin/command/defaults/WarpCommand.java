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

public class WarpCommand extends SubCommand {

    public WarpCommand(PlotPlugin plugin) {
        super(plugin, "warp", "w");
        this.addParameter(CommandParameter.newType("id", CommandParamType.STRING));
    }

    @Override
    public boolean execute(Player player, String[] args) {
        PlotManager plotManager = this.plugin.getPlotManager(player.getLevel());
        if(plotManager == null && this.plugin.getPlotLevel() == null || plotManager == null && (plotManager = this.plugin.getPlotManager(this.plugin.getPlotLevel())) == null) {
            player.sendMessage(this.translate("no-plot-world"));
            return false;
        }

        final String[] plotIds = args.length > 0 ?
                args[0].split(";").length > 1 ? args[0].split(";") :
                        args[0].split(":").length > 1 ? args[0].split(":") :
                                args[0].split(",").length > 1 ? args[0].split(",") :
                                        new String[0] : new String[0];

        final Integer plotX = plotIds.length != 0 ? Utils.parseIntegerWithNull(plotIds[0]) : null;
        final Integer plotZ = plotIds.length != 0 ? Utils.parseIntegerWithNull(plotIds[1]) : null;

        if(plotX == null || plotZ == null) {
            player.sendMessage(this.translate("no-plot-id"));
            return false;
        }

        final Plot plot = plotManager.getPlotById(plotX, plotZ);

        if(!plot.hasOwner() && !player.hasPermission("plot.command.warp.free")) {
            player.sendMessage(this.translate("warp-failure-free"));
            return false;
        }

        if((!plot.isDenied(player.getUniqueId()) && !plot.isDenied(Utils.UUID_EVERYONE)) || player.hasPermission("plot.admin.nodeny")) {
            plotManager.teleportPlayerToPlot(player, plot);
            player.sendMessage(this.translate("warp-success", (plotX + ";" + plotZ)));
            return true;
        }

        player.sendMessage(this.translate("warp-failure"));
        return false;
    }

}
