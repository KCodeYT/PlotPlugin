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
import ms.kevi.plotplugin.event.PlotClaimEvent;
import ms.kevi.plotplugin.event.PlotPreClaimEvent;
import ms.kevi.plotplugin.lang.TranslationKey;
import ms.kevi.plotplugin.util.Plot;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
public class ClaimCommand extends SubCommand {

    public ClaimCommand(PlotPlugin plugin, PlotCommand parent) {
        super(plugin, parent, "claim", "c");
    }

    @Override
    public void execute(Player player, String[] args) {
        this.plugin.getPlotManager(player.getLevel()).whenCompleteAsync((plotManager, throwable) -> {
            final Plot plot;
            if(plotManager == null || (plot = plotManager.getMergedPlot(player.getFloorX(), player.getFloorZ()).join()) == null) {
                player.sendMessage(this.translate(player, TranslationKey.NO_PLOT));
                return;
            }

            if(!plot.hasOwner()) {
                final PlotPreClaimEvent plotPreClaimEvent = new PlotPreClaimEvent(player, plot, false, true);
                this.plugin.getServer().getPluginManager().callEvent(plotPreClaimEvent);
                plotPreClaimEvent.getWaiter().addTask(() -> {
                    if(plotPreClaimEvent.isCancelled()) return;

                    plot.setOwner(player.getUniqueId());
                    if(plotPreClaimEvent.isBorderChanging())
                        plotManager.changeBorder(plot, plotManager.getLevelSettings().getClaimPlotState());
                    plotManager.addPlot(plot).join();
                    plotManager.savePlots();

                    final PlotClaimEvent plotClaimEvent = new PlotClaimEvent(player, plot, false);
                    this.plugin.getServer().getPluginManager().callEvent(plotClaimEvent);

                    player.sendMessage(this.translate(player, TranslationKey.CLAIM_SUCCESS));
                });
            } else {
                player.sendMessage(this.translate(player, TranslationKey.CLAIM_FAILURE));
            }
        });
    }

}
