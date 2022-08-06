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
import cn.nukkit.command.data.CommandEnum;
import cn.nukkit.command.data.CommandParameter;
import ms.kevi.plotplugin.PlotPlugin;
import ms.kevi.plotplugin.command.PlotCommand;
import ms.kevi.plotplugin.command.SubCommand;
import ms.kevi.plotplugin.lang.TranslationKey;
import ms.kevi.plotplugin.manager.PlotManager;
import ms.kevi.plotplugin.util.Plot;

import java.util.Locale;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
public class UnlinkCommand extends SubCommand {

    public UnlinkCommand(PlotPlugin plugin, PlotCommand parent) {
        super(plugin, parent, "unlink");
        this.setPermission("plot.command.unlink");
        this.addParameter(CommandParameter.newEnum("type", new CommandEnum("plot unlink type", "all", "neighbors")));
    }

    @Override
    public boolean execute(Player player, String[] args) {
        final String type = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "neighbors";

        final PlotManager plotManager = this.plugin.getPlotManager(player.getLevel());
        final Plot plot;
        if(plotManager == null || (plot = plotManager.getMergedPlot(player.getFloorX(), player.getFloorZ())) == null) {
            player.sendMessage(this.translate(player, TranslationKey.NO_PLOT));
            return false;
        }

        if(!player.hasPermission("plot.command.admin.unlink") && !plot.isOwner(player.getUniqueId())) {
            player.sendMessage(this.translate(player, TranslationKey.NO_PLOT_OWNER));
            return false;
        }

        if(plot.hasNoMerges()) {
            player.sendMessage(this.translate(player, TranslationKey.UNLINK_FAILURE));
            return false;
        }

        switch(type) {
            case "all" -> plotManager.unlinkPlotFromAll(plot);
            case "neighbors" -> plotManager.unlinkPlotFromNeighbors(plot);
            default -> {
                player.sendMessage(this.translate(player, TranslationKey.UNLINK_FAILURE_UNKNOWN_TYPE));
                return false;
            }
        }

        plotManager.savePlots();
        player.sendMessage(this.translate(player, TranslationKey.UNLINK_SUCCESS));
        return true;
    }

}
