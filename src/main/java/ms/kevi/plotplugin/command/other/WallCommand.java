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

package ms.kevi.plotplugin.command.other;

import cn.nukkit.Player;
import cn.nukkit.blockstate.BlockState;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.element.ElementButtonImageData;
import cn.nukkit.form.handler.FormResponseHandler;
import cn.nukkit.form.window.FormWindowSimple;
import ms.kevi.plotplugin.PlotPlugin;
import ms.kevi.plotplugin.command.PlotCommand;
import ms.kevi.plotplugin.command.SubCommand;
import ms.kevi.plotplugin.lang.TranslationKey;
import ms.kevi.plotplugin.manager.PlotManager;
import ms.kevi.plotplugin.util.Plot;
import ms.kevi.plotplugin.util.WallEntry;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
public class WallCommand extends SubCommand {

    public WallCommand(PlotPlugin plugin, PlotCommand parent) {
        super(plugin, parent, "wall", "border");
    }

    @Override
    public boolean execute(Player player, String[] args) {
        final PlotManager plotManager = this.plugin.getPlotManager(player.getLevel());
        final Plot plot;
        if(plotManager == null || (plot = plotManager.getMergedPlot(player.getFloorX(), player.getFloorZ())) == null) {
            player.sendMessage(this.translate(player, TranslationKey.NO_PLOT));
            return false;
        }

        if(!player.hasPermission("plot.command.admin.wall") && !plot.isOwner(player.getUniqueId())) {
            player.sendMessage(this.translate(player, TranslationKey.NO_PLOT_OWNER));
            return false;
        }

        final FormWindowSimple window = new FormWindowSimple(this.translate(player, TranslationKey.WALL_FORM_TITLE), "");

        final Map<ElementButton, WallEntry> buttons = new HashMap<>();
        for(WallEntry wallEntry : this.plugin.getWallEntries()) {
            final String imageType = wallEntry.getImageType();
            final ElementButtonImageData imageData;
            if(imageType != null) {
                switch(imageType.toLowerCase(Locale.ROOT)) {
                    case "url" ->
                            imageData = new ElementButtonImageData(ElementButtonImageData.IMAGE_DATA_TYPE_URL, wallEntry.getImageData());
                    case "path" ->
                            imageData = new ElementButtonImageData(ElementButtonImageData.IMAGE_DATA_TYPE_PATH, wallEntry.getImageData());
                    default -> imageData = new ElementButtonImageData(ElementButtonImageData.IMAGE_DATA_TYPE_URL, "");
                }
            } else imageData = new ElementButtonImageData(ElementButtonImageData.IMAGE_DATA_TYPE_URL, "");

            final ElementButton button;
            if(wallEntry.isDefault()) {
                window.addButton(button = new ElementButton(this.translate(player, TranslationKey.WALL_RESET_TO_DEFAULT_BUTTON).replace("\\n", "\n"), imageData));
            } else {
                final boolean hasPerm = wallEntry.getPermission() == null || player.hasPermission(wallEntry.getPermission());
                window.addButton(button = new ElementButton(this.translate(player, TranslationKey.WALL_FORM_BUTTON, wallEntry.getName(), this.translate(player, hasPerm ? TranslationKey.WALL_BUTTON_HAS_PERM : TranslationKey.WALL_BUTTON_NO_PERM)).replace("\\n", "\n"), imageData));
            }

            buttons.put(button, wallEntry);
        }

        window.addHandler(FormResponseHandler.withoutPlayer(ignored -> {
            if(!window.wasClosed()) {
                final ElementButton button = window.getResponse().getClickedButton();
                final WallEntry wallEntry;
                if(button == null || (wallEntry = buttons.get(button)) == null) return;

                if(wallEntry.getPermission() != null && !player.hasPermission(wallEntry.getPermission())) {
                    player.sendMessage(this.translate(player, TranslationKey.WALL_NO_PERMS, wallEntry.getName()));
                    return;
                }

                if(wallEntry.isDefault()) {
                    for(Plot mergedPlot : plotManager.getConnectedPlots(plot))
                        plotManager.changeBorder(mergedPlot, plotManager.getLevelSettings().getClaimPlotState());
                    player.sendMessage(this.translate(player, TranslationKey.WALL_RESET_TO_DEFAULT_SUCCESS));
                } else {
                    for(Plot mergedPlot : plotManager.getConnectedPlots(plot))
                        plotManager.changeBorder(mergedPlot, wallEntry.getBlockState());
                    player.sendMessage(this.translate(player, TranslationKey.WALL_SUCCESS, wallEntry.getName()));
                }
            }
        }));

        player.showFormWindow(window);
        return true;
    }

}
