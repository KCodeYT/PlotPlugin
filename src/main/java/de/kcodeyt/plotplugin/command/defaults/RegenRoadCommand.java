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
import cn.nukkit.level.Level;
import cn.nukkit.level.format.FullChunk;
import de.kcodeyt.plotplugin.PlotPlugin;
import de.kcodeyt.plotplugin.command.PlotCommand;
import de.kcodeyt.plotplugin.command.SubCommand;
import de.kcodeyt.plotplugin.generator.PlotGenerator;
import de.kcodeyt.plotplugin.lang.TranslationKey;
import de.kcodeyt.plotplugin.manager.PlotManager;
import de.kcodeyt.plotplugin.util.async.TaskExecutor;

public class RegenRoadCommand extends SubCommand {

    public RegenRoadCommand(PlotPlugin plugin, PlotCommand parent) {
        super(plugin, parent, "regenroad");
        this.setPermission("plot.command.admin.regenroad");
    }

    @Override
    public boolean execute(Player player, String[] args) {
        final PlotManager plotManager = this.plugin.getPlotManager(player.getLevel());
        if(plotManager == null) {
            player.sendMessage(this.translate(player, TranslationKey.NO_PLOT_WORLD));
            return false;
        }

        final Level level = player.getLevel();
        final PlotGenerator plotGenerator = (PlotGenerator) level.getGenerator();

        final int pChunkX = player.getChunkX();
        final int pChunkZ = player.getChunkZ();

        TaskExecutor.executeAsync(() -> {
            final FullChunk fullChunk = level.getChunk(pChunkX, pChunkZ, false);
            if(fullChunk != null) plotGenerator.regenerateChunk(plotManager, fullChunk, true);
            player.sendMessage("Finished Regenroad Task!");
        });

        player.sendMessage("Starting Regenroad Task!");
        return true;
    }

}
