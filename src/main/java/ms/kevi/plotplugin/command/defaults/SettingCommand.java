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
import cn.nukkit.command.data.CommandParameter;
import ms.kevi.plotplugin.PlotPlugin;
import ms.kevi.plotplugin.command.PlotCommand;
import ms.kevi.plotplugin.command.SubCommand;
import ms.kevi.plotplugin.lang.TranslationKey;
import ms.kevi.plotplugin.util.Plot;
import ms.kevi.plotplugin.util.PlotConfig;
import ms.kevi.plotplugin.util.Utils;

import java.util.Arrays;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
public class SettingCommand extends SubCommand {

    public SettingCommand(PlotPlugin plugin, PlotCommand parent) {
        super(plugin, parent, "setting", "config");
        this.addParameter(CommandParameter.newEnum("name", new String[]{"damage", "pve", "pvp"}));
    }

    @Override
    public void execute(Player player, String[] args0) {
        this.plugin.getPlotManager(player.getLevel()).whenCompleteAsync((plotManager, throwable) -> {
            final Plot plot;
            if(plotManager == null || (plot = plotManager.getMergedPlot(player.getFloorX(), player.getFloorZ()).join()) == null) {
                player.sendMessage(this.translate(player, TranslationKey.NO_PLOT));
                return;
            }

            String[] args = args0;
            final String configName = args.length > 0 ? args[0] : "";
            args = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];

            if(!player.hasPermission("plot.command.admin.config") && !plot.isOwner(player.getUniqueId())) {
                player.sendMessage(this.translate(player, TranslationKey.NO_PLOT_OWNER));
                return;
            }

            switch(configName.toLowerCase()) {
                case "damage" -> {
                    final boolean damageValue = args.length > 0 && Utils.parseBoolean(args[0]);
                    PlotConfig.ConfigEnum.DAMAGE.getConfig().set(plot, damageValue);
                    plotManager.addPlot(plot).join();
                    plotManager.savePlots();

                    player.sendMessage(this.translate(player, TranslationKey.CONFIG_DAMAGE, this.translate(player, damageValue ? TranslationKey.ACTIVATED : TranslationKey.DEACTIVATED)));
                }
                case "pve" -> {
                    final boolean pveValue = args.length > 0 && Utils.parseBoolean(args[0]);
                    PlotConfig.ConfigEnum.PVE.getConfig().set(plot, pveValue);
                    plotManager.addPlot(plot).join();
                    plotManager.savePlots();

                    player.sendMessage(this.translate(player, TranslationKey.CONFIG_PVE, this.translate(player, pveValue ? TranslationKey.ACTIVATED : TranslationKey.DEACTIVATED)));
                }
                case "pvp" -> {
                    final boolean pvpValue = args.length > 0 && Utils.parseBoolean(args[0]);
                    PlotConfig.ConfigEnum.PVP.getConfig().set(plot, pvpValue);
                    plotManager.addPlot(plot).join();
                    plotManager.savePlots();

                    player.sendMessage(this.translate(player, TranslationKey.CONFIG_PVP, this.translate(player, pvpValue ? TranslationKey.ACTIVATED : TranslationKey.DEACTIVATED)));
                }
                default -> {
                    player.sendMessage(this.translate(player, TranslationKey.CONFIG_HELP_TITLE));
                    player.sendMessage(this.translate(player, TranslationKey.CONFIG_HELP_DAMAGE));
                    player.sendMessage(this.translate(player, TranslationKey.CONFIG_HELP_PVE));
                    player.sendMessage(this.translate(player, TranslationKey.CONFIG_HELP_PVP));
                    player.sendMessage(this.translate(player, TranslationKey.CONFIG_HELP_END));
                }
            }
        });
    }

}
