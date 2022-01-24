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
import cn.nukkit.blockstate.BlockState;
import cn.nukkit.command.data.CommandParamType;
import cn.nukkit.command.data.CommandParameter;
import de.kcodeyt.plotplugin.PlotPlugin;
import de.kcodeyt.plotplugin.command.SubCommand;
import de.kcodeyt.plotplugin.manager.PlotManager;
import de.kcodeyt.plotplugin.util.Plot;
import de.kcodeyt.plotplugin.util.PlotConfig;
import de.kcodeyt.plotplugin.util.Utils;

import java.util.Arrays;

public class SettingCommand extends SubCommand {

    public SettingCommand(PlotPlugin plugin) {
        super(plugin, "setting", "config");
        this.addParameter(CommandParameter.newEnum("name", new String[]{"damage", "pve", "pvp"}));
    }

    @Override
    public boolean execute(Player player, String[] args) {
        final PlotManager plotManager = this.plugin.getPlotManager(player.getLevel());
        final Plot plot;
        if(plotManager == null || (plot = plotManager.getMergedPlot(player.getFloorX(), player.getFloorZ())) == null) {
            player.sendMessage(this.translate("no-plot"));
            return false;
        }

        final String configName = args.length > 0 ? args[0] : "";
        args = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];

        if(!player.hasPermission("plot.command.admin.config") && !plot.isOwner(player.getUniqueId())) {
            player.sendMessage(this.translate("no-plot-owner"));
            return false;
        }

        switch(configName.toLowerCase()) {
            case "damage":
            {
                final boolean damageValue = args.length > 0 && Utils.parseBoolean(args[0]);
                PlotConfig.ConfigEnum.DAMAGE.getConfig().set(plot, damageValue);
                plotManager.savePlots();
                player.sendMessage(this.translate("config-damage", this.translate(damageValue ? "activated" : "deactivated")));
                return true;
            }
            case "pve":
            {
                final boolean pveValue = args.length > 0 && Utils.parseBoolean(args[0]);
                PlotConfig.ConfigEnum.PVE.getConfig().set(plot, pveValue);
                plotManager.savePlots();
                player.sendMessage(this.translate("config-pve", this.translate(pveValue ? "activated" : "deactivated")));
                return false;
            }
            case "pvp":
            {
                final boolean pvpValue = args.length > 0 && Utils.parseBoolean(args[0]);
                PlotConfig.ConfigEnum.PVP.getConfig().set(plot, pvpValue);
                plotManager.savePlots();
                player.sendMessage(this.translate("config-pvp", this.translate(pvpValue ? "activated" : "deactivated")));
                return false;
            }
            default:
            {
                player.sendMessage(this.translate("config-help-title"));
                player.sendMessage(this.translate("config-help-damage"));
                player.sendMessage(this.translate("config-help-pve"));
                player.sendMessage(this.translate("config-help-pvp"));
                player.sendMessage(this.translate("config-help-end"));
                return false;
            }
        }
    }

}
