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

public class ClaimCommand extends SubCommand {

    public ClaimCommand(PlotPlugin plugin) {
        super(plugin, "claim", "c");
    }

    @Override
    public boolean execute(Player player, String[] args) {
        final PlotManager plotManager = this.plugin.getPlotManager(player.getLevel());
        final Plot plot;
        if(plotManager == null || (plot = plotManager.getMergedPlot(player.getFloorX(), player.getFloorZ())) == null) {
            player.sendMessage(this.translate("no-plot"));
            return false;
        }

        if(!plot.hasOwner()) {
            final PlotPreClaimEvent plotPreClaimEvent = new PlotPreClaimEvent(player, plot, false, true);
            this.plugin.getServer().getPluginManager().callEvent(plotPreClaimEvent);
            plotPreClaimEvent.getWaiter().addTask(() -> {
                if(plotPreClaimEvent.isCancelled()) return;

                plot.setOwner(player.getUniqueId());
                if(plotPreClaimEvent.isBorderChanging())
                    plotManager.changeBorder(plot, plotManager.getLevelSettings().getClaimPlotState());
                plotManager.savePlots();

                final PlotClaimEvent plotClaimEvent = new PlotClaimEvent(player, plot, false);
                this.plugin.getServer().getPluginManager().callEvent(plotClaimEvent);

                player.sendMessage(this.translate("claim-success"));
            });
            return true;
        } else {
            player.sendMessage(this.translate("claim-failure"));
            return false;
        }
    }

}
