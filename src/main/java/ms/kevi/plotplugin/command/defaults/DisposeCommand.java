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
import ms.kevi.plotplugin.PlotPlugin;
import ms.kevi.plotplugin.command.PlotCommand;
import ms.kevi.plotplugin.command.SubCommand;
import ms.kevi.plotplugin.lang.TranslationKey;
import ms.kevi.plotplugin.manager.PlotManager;
import ms.kevi.plotplugin.util.Plot;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
public class DisposeCommand extends SubCommand {

    public DisposeCommand(PlotPlugin plugin, PlotCommand parent) {
        super(plugin, parent, "dispose", "reset");
    }

    @Override
    public boolean execute(Player player, String[] args) {
        final PlotManager plotManager = this.plugin.getPlotManager(player.getLevel());
        final Plot plot;
        if(plotManager == null || (plot = plotManager.getMergedPlot(player.getFloorX(), player.getFloorZ())) == null) {
            player.sendMessage(this.translate(player, TranslationKey.NO_PLOT));
            return false;
        }

        if(!plot.isOwner(player.getUniqueId()) && !player.hasPermission("plot.command.admin.dispose")) {
            player.sendMessage(this.translate(player, TranslationKey.DISPOSE_FAILURE));
            return false;
        }

        if(!plotManager.disposePlot(plot)) {
            player.sendMessage(this.translate(player, TranslationKey.DISPOSE_FAILURE_COULD_NOT_DISPOSE));
            return false;
        }

        plotManager.savePlots();
        player.sendMessage(this.translate(player, TranslationKey.DISPOSE_SUCCESS));
        return true;
    }

}
