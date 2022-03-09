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

import java.util.UUID;

public class RemoveHelperCommand extends SubCommand {

    public RemoveHelperCommand(PlotPlugin plugin) {
        super(plugin, "removehelper", "remove", "untrust");
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

        if(targetName.equalsIgnoreCase(player.getName()) && !player.hasPermission("plot.command.admin.removehelper")) {
            player.sendMessage(this.translate("player-self"));
            return false;
        }

        if(targetName.isEmpty() || targetId == null) {
            player.sendMessage(this.translate("no-player"));
            return false;
        }

        if(!player.hasPermission("plot.command.admin.removehelper") && !plot.isOwner(player.getUniqueId())) {
            player.sendMessage(this.translate("no-plot-owner"));
            return false;
        }

        if(plot.removeHelper(targetId)) {
            plotManager.savePlots();
            player.sendMessage(this.translate("removed-helper", this.plugin.getCorrectName(targetId)));
            return true;
        } else {
            player.sendMessage(this.translate("no-helper", this.plugin.getCorrectName(targetId)));
            return false;
        }
    }

}
