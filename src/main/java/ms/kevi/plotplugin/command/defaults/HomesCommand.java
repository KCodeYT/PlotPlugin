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
import cn.nukkit.command.data.CommandParamType;
import cn.nukkit.command.data.CommandParameter;
import ms.kevi.plotplugin.PlotPlugin;
import ms.kevi.plotplugin.command.PlotCommand;
import ms.kevi.plotplugin.command.SubCommand;
import ms.kevi.plotplugin.lang.TranslationKey;
import ms.kevi.plotplugin.manager.PlotManager;
import ms.kevi.plotplugin.util.PaginationList;
import ms.kevi.plotplugin.util.Plot;
import ms.kevi.plotplugin.util.Utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
public class HomesCommand extends SubCommand {

    public HomesCommand(PlotPlugin plugin, PlotCommand parent) {
        super(plugin, parent, "homes");
        this.addParameter(CommandParameter.newType("page", true, CommandParamType.INT));
    }

    @Override
    public boolean execute(Player player, String[] args) {
        final int page = Utils.parseInteger(args.length == 1 ? args[0] : args.length > 1 ? args[1] : "1") - 1;

        final Map<PlotManager, List<Plot>> plots = new LinkedHashMap<>();
        for(PlotManager plotManager : this.plugin.getPlotManagerMap().values())
            plots.put(plotManager, plotManager.getPlotsByOwner(player.getUniqueId()));

        if(plots.isEmpty()) {
            player.sendMessage(this.translate(player, TranslationKey.HOMES_FAILURE));
            return false;
        }

        final List<Plot> allPlots = new ArrayList<>();
        for(List<Plot> list : plots.values()) allPlots.addAll(list);

        final PaginationList<Plot> pages = new PaginationList<>(allPlots, this.plugin.getPlotsPerPage());

        if(page < 0 || page >= pages.size()) {
            player.sendMessage(this.translate(player, TranslationKey.HOMES_FAILURE_PAGE_DID_NOT_EXIST, page + 1));
            return false;
        }

        player.sendMessage(this.translate(player, TranslationKey.HOMES_TITLE, page + 1, pages.size()));

        for(Plot plot : pages.get(page))
            player.sendMessage(this.translate(player, TranslationKey.HOMES_ENTRY,
                    plots.get(plot.getManager()).indexOf(plot) + 1,
                    plot.getId(),
                    plot.getManager().getLevel().getFolderName()
            ));

        player.sendMessage(this.translate(player, TranslationKey.HOMES_END));
        return true;
    }

}
