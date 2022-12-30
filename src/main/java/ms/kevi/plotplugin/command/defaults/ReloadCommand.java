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
import ms.kevi.plotplugin.lang.TranslationKey;

import java.io.IOException;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
public class ReloadCommand extends SubCommand {

    public ReloadCommand(PlotPlugin plugin, PlotCommand parent) {
        super(plugin, parent, "reload");
        this.setPermission("plot.command.admin.reload");
    }

    @Override
    public boolean execute(Player player, String[] args) {
        try {
            this.plugin.getLanguage().reload();
        } catch(IOException e) {
            this.plugin.getLogger().warning("Could not reload configurations!", e);
            player.sendMessage(this.translate(player, TranslationKey.RELOAD_FAILURE));
            return false;
        }

        player.sendMessage(this.translate(player, TranslationKey.RELOAD_SUCCESS));
        return true;
    }

}
