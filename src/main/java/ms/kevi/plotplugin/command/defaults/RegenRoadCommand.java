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
import cn.nukkit.level.Level;
import cn.nukkit.level.format.FullChunk;
import ms.kevi.plotplugin.PlotPlugin;
import ms.kevi.plotplugin.command.PlotCommand;
import ms.kevi.plotplugin.command.SubCommand;
import ms.kevi.plotplugin.generator.PlotGenerator;
import ms.kevi.plotplugin.lang.TranslationKey;
import ms.kevi.plotplugin.manager.PlotManager;
import ms.kevi.plotplugin.util.async.TaskExecutor;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
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
            player.sendMessage(this.translate(player, TranslationKey.REGENROAD_FINISHED));
        });

        player.sendMessage(this.translate(player, TranslationKey.REGENROAD_START));
        return true;
    }

}
