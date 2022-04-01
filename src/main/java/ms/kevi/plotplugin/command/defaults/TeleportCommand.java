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
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.response.FormResponse;
import cn.nukkit.form.response.FormResponseSimple;
import cn.nukkit.form.window.FormWindowSimple;
import ms.kevi.plotplugin.PlotPlugin;
import ms.kevi.plotplugin.command.PlotCommand;
import ms.kevi.plotplugin.command.SubCommand;
import ms.kevi.plotplugin.lang.TranslationKey;
import ms.kevi.plotplugin.manager.PlotManager;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
public class TeleportCommand extends SubCommand {

    private final Map<Player, Map<Integer, Consumer<FormResponse>>> map;

    public TeleportCommand(PlotPlugin plugin, PlotCommand parent) {
        super(plugin, parent, "teleport", "tp");
        this.setPermission("plot.command.admin.teleport");

        this.map = new HashMap<>();

        this.plugin.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onResponded(PlayerFormRespondedEvent event) {
                final Map<Integer, Consumer<FormResponse>> map;
                if((map = TeleportCommand.this.map.get(event.getPlayer())) != null) {
                    final Consumer<FormResponse> consumer = map.get(event.getFormID());
                    if(consumer != null) consumer.accept(event.getResponse());
                }
            }
        }, this.plugin);
    }

    @Override
    public boolean execute(Player player, String[] args) {
        Map<Integer, Consumer<FormResponse>> map;
        if((map = this.map.get(player)) == null)
            this.map.put(player, map = new HashMap<>());

        final Map<String, PlotManager> plotManagers = this.plugin.getPlotManagerMap();
        final FormWindowSimple window = new FormWindowSimple(this.translate(player, TranslationKey.TELEPORT_FORM_TITLE), "");

        for(String levelName : plotManagers.keySet())
            window.addButton(new ElementButton(levelName));

        map.put(player.showFormWindow(window), response -> {
            if(response instanceof FormResponseSimple) {
                final PlotManager plotManager = plotManagers.get(((FormResponseSimple) response).getClickedButton().getText());
                if(plotManager == null) return;

                player.teleport(plotManager.getLevel().getSpawnLocation());
                player.sendMessage(this.translate(player, TranslationKey.TELEPORT_SUCCESS, plotManager.getLevel().getFolderName()));
            }
        });
        return false;
    }

}
