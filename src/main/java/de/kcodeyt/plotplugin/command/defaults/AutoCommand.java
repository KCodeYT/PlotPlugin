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
import de.kcodeyt.plotplugin.PlotPlugin;
import de.kcodeyt.plotplugin.command.SubCommand;
import de.kcodeyt.plotplugin.event.PlotClaimEvent;
import de.kcodeyt.plotplugin.event.PlotPreClaimEvent;
import de.kcodeyt.plotplugin.manager.PlotManager;
import de.kcodeyt.plotplugin.util.Plot;

public class AutoCommand extends SubCommand {

    public AutoCommand(PlotPlugin plugin) {
        super(plugin, "auto", "a");
    }

    @Override
    public boolean execute(Player player, String[] args) {
        PlotManager plotManager = this.plugin.getPlotManager(player.getLevel());
        if(plotManager == null && this.plugin.getDefaultPlotLevel() == null || plotManager == null && (plotManager = this.plugin.getPlotManager(this.plugin.getDefaultPlotLevel())) == null) {
            player.sendMessage(this.translate("no-plot-world"));
            return false;
        }

        final Plot plot = plotManager.getNextFreePlot();

        if(plot != null) {
            final PlotPreClaimEvent plotPreClaimEvent = new PlotPreClaimEvent(player, plot, true, true);
            this.plugin.getServer().getPluginManager().callEvent(plotPreClaimEvent);
            final PlotManager finalPlotManager = plotManager;
            plotPreClaimEvent.getWaiter().addTask(() -> {
                if(plotPreClaimEvent.isCancelled())
                    return;

                plot.setOwner(player.getUniqueId());
                if(plotPreClaimEvent.isBorderChanging())
                    finalPlotManager.changeBorder(plot, finalPlotManager.getLevelSettings().getClaimPlotState());
                finalPlotManager.savePlots();

                final PlotClaimEvent plotClaimEvent = new PlotClaimEvent(player, plot, true);
                this.plugin.getServer().getPluginManager().callEvent(plotClaimEvent);

                finalPlotManager.teleportPlayerToPlot(player, plot);
                player.sendMessage(this.translate("auto-success"));
            });
            return true;
        } else {
            player.sendMessage(this.translate("auto-failure"));
            return false;
        }
    }

}
