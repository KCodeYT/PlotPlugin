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
import de.kcodeyt.plotplugin.util.PlotLevelRegistration;
import de.kcodeyt.plotplugin.util.Utils;

public class GenerateCommand extends SubCommand {

    public GenerateCommand(PlotPlugin plugin) {
        super(plugin, "generate");
        this.setPermission("plot.command.admin.generate");
        this.addParameter(CommandParameter.newType("level", CommandParamType.STRING));
        this.addParameter(CommandParameter.newEnum("default", new String[]{"true", "false"}));
    }

    @Override
    public boolean execute(Player player, String[] args) {
        final String levelName = args.length > 0 ? args[0] : "";
        final boolean defaultLevel = args.length > 1 && Utils.parseBoolean(args[1]);

        if(levelName.trim().isEmpty()) {
            player.sendMessage(this.translate("no-world"));
            return false;
        }

        if(!this.plugin.getServer().isLevelGenerated(levelName)) {
            final PlotLevelRegistration levelRegistration = new PlotLevelRegistration(levelName, defaultLevel);
            this.plugin.getLevelRegistrationMap().put(player, levelRegistration);
            player.sendMessage(this.translate("generate-start", levelName));
            player.sendMessage(this.translate("generate-dimension", levelRegistration.getLevelSettings().getDimension()));
            return true;
        } else {
            if(!this.plugin.getServer().isLevelLoaded(levelName))
                this.plugin.getServer().loadLevel(levelName);
            player.teleport(this.plugin.getServer().getLevelByName(levelName).getSpawnLocation());
            player.sendMessage(this.translate("generate-failure"));
            return false;
        }
    }

}
