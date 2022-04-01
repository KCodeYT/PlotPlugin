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

import java.util.List;
import java.util.UUID;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
public class HomeCommand extends SubCommand {

    public HomeCommand(PlotPlugin plugin, PlotCommand parent) {
        super(plugin, parent, "home", "h", "visit", "v");
        this.addParameter(CommandParameter.newType("id", true, CommandParamType.INT));
    }

    @Override
    public void execute(Player player, String[] args) {
        this.plugin.getPlotManager(player.getLevel()).whenCompleteAsync((plotManager, throwable) -> {
            if(plotManager == null && this.plugin.getDefaultPlotLevel() == null || plotManager == null && (plotManager = this.plugin.getPlotManager(this.plugin.getDefaultPlotLevel()).join()) == null) {
                player.sendMessage(this.translate(player, TranslationKey.NO_PLOT_WORLD));
                return;
            }

            int plotId = Utils.parseInteger(args.length == 1 ? args[0] : args.length > 1 ? args[1] : "1") - 1;
            if(plotId < 0) plotId = 0;

            final String targetName = (args.length >= 2 ? this.plugin.findPlayerName(args[0]) : player.getName()).trim();
            final UUID targetId = this.plugin.getUniqueIdByName(targetName);

            if(targetName.isEmpty() || targetId == null) {
                player.sendMessage(this.translate(player, TranslationKey.NO_PLAYER));
                return;
            }

            final List<Plot> plots;
            if((plots = plotManager.getPlotsByOwner(targetId).join()).size() != 0) {
                if(plotId < plots.size()) {
                    final Plot plot = plots.get(plotId);
                    final boolean canPerform = (!plot.isDenied(player.getUniqueId()) && !plot.isDenied(Utils.UUID_EVERYONE)) || player.hasPermission("plot.admin.nodeny");
                    if(targetName.equalsIgnoreCase(player.getName())) {
                        player.sendMessage(this.translate(player, TranslationKey.HOME_SUCCESS_OWN));
                        plotManager.teleportPlayerToPlot(player, plots.get(plotId));
                    } else if(canPerform) {
                        player.sendMessage(this.translate(player, TranslationKey.HOME_SUCCESS, this.plugin.getCorrectName(targetId)));
                        plotManager.teleportPlayerToPlot(player, plots.get(plotId));
                    } else
                        player.sendMessage(this.translate(player, TranslationKey.HOME_FAILURE_DENIED));
                } else {
                    if(targetName.equalsIgnoreCase(player.getName()))
                        player.sendMessage(this.translate(player, TranslationKey.HOME_FAILURE_OWN_ID, plotId + 1));
                    else
                        player.sendMessage(this.translate(player, TranslationKey.HOME_FAILURE_ID, this.plugin.getCorrectName(targetId), plotId + 1));
                }
            } else {
                if(targetName.equalsIgnoreCase(player.getName()))
                    player.sendMessage(this.translate(player, TranslationKey.HOME_FAILURE_OWN));
                else
                    player.sendMessage(this.translate(player, TranslationKey.HOME_FAILURE, this.plugin.getCorrectName(targetId)));
            }
        });
    }

}
