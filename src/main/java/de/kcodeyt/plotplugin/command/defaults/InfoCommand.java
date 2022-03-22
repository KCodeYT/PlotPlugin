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
import cn.nukkit.utils.TextFormat;
import de.kcodeyt.plotplugin.PlotPlugin;
import de.kcodeyt.plotplugin.command.PlotCommand;
import de.kcodeyt.plotplugin.command.SubCommand;
import de.kcodeyt.plotplugin.lang.TranslationKey;
import de.kcodeyt.plotplugin.manager.PlotManager;
import de.kcodeyt.plotplugin.util.Plot;
import de.kcodeyt.plotplugin.util.PlotConfig;

public class InfoCommand extends SubCommand {

    public InfoCommand(PlotPlugin plugin, PlotCommand parent) {
        super(plugin, parent, "info", "i");
    }

    @Override
    public boolean execute(Player player, String[] args) {
        final PlotManager plotManager = this.plugin.getPlotManager(player.getLevel());
        final Plot plot;
        if(plotManager == null || (plot = plotManager.getMergedPlot(player.getFloorX(), player.getFloorZ())) == null) {
            player.sendMessage(this.translate(player, TranslationKey.NO_PLOT));
            return false;
        }

        if(plot.hasOwner() || player.hasPermission("plot.command.admin.info")) {
            final String plotOwner = this.plugin.getCorrectName(plot.getOwner());
            final StringBuilder helpers = new StringBuilder();
            final String helpersLastColors = TextFormat.getLastColors(this.translate(player, TranslationKey.INFO_HELPERS, ""));
            plot.getHelpers().forEach(helper -> helpers.append(this.plugin.getCorrectName(helper)).append("§r§7, ").append(helpersLastColors));

            final StringBuilder denied = new StringBuilder();
            final String deniedLastColors = TextFormat.getLastColors(this.translate(player, TranslationKey.INFO_DENIED, ""));
            plot.getDeniedPlayers().forEach(deniedPlayer -> denied.append(this.plugin.getCorrectName(deniedPlayer)).append("§r§7, ").append(deniedLastColors));

            player.sendMessage(this.translate(player, TranslationKey.INFO_TITLE));
            player.sendMessage(this.translate(player, TranslationKey.INFO_ID, plot.getId()));
            player.sendMessage(this.translate(player, TranslationKey.INFO_OWNER, plotOwner));
            player.sendMessage(this.translate(player, TranslationKey.INFO_HELPERS, (helpers.length() > 0 ? helpers.substring(0, helpers.length() - 2 - helpersLastColors.length()) : "§c-----")));
            player.sendMessage(this.translate(player, TranslationKey.INFO_DENIED, (denied.length() > 0 ? denied.substring(0, denied.length() - 2 - deniedLastColors.length()) : "§c-----")));

            for(PlotConfig.ConfigEnum configEnum : PlotConfig.ConfigEnum.values())
                player.sendMessage(this.translate(player, configEnum.getConfig().getInfoTranslationKey(),
                        this.translate(player, ((boolean) configEnum.getConfig().get(plot)) ? TranslationKey.ACTIVATED : TranslationKey.DEACTIVATED)
                ));

            player.sendMessage(this.translate(player, TranslationKey.INFO_END));
            return true;
        }

        player.sendMessage(this.translate(player, TranslationKey.INFO_FAILURE));
        return false;
    }

}
