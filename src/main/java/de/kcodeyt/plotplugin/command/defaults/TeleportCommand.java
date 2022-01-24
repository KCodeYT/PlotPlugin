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

public class TeleportCommand extends SubCommand {

    public TeleportCommand(PlotPlugin plugin) {
        super(plugin, "teleport", "tp");
        this.setPermission("plot.command.admin.teleport");
        this.addParameter(CommandParameter.newType("level", CommandParamType.STRING));
    }

    @Override
    public boolean execute(Player player, String[] args) {
        final String levelName = args.length > 0 ? args[0] : "";

        if(levelName.trim().isEmpty()) {
            player.sendMessage(this.translate("no-world"));
            return false;
        }

        final PlotManager plotManager = this.plugin.getPlotManager(levelName);
        if(plotManager == null) {
            player.sendMessage(this.translate("teleport-no-plot-world"));
            return false;
        }

        player.teleport(plotManager.getLevel().getSpawnLocation());
        player.sendMessage(this.translate("teleport-success", plotManager.getLevel().getFolderName()));
        return false;
    }

}
