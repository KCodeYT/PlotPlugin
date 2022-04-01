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

package ms.kevi.plotplugin.util;

import lombok.Getter;
import ms.kevi.plotplugin.PlotPlugin;
import ms.kevi.plotplugin.manager.PlotManager;
import ms.kevi.plotplugin.schematic.Schematic;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
@Getter
public class PlotSchematic {

    private final PlotPlugin plugin;
    private final PlotManager manager;
    private Schematic schematic;

    public PlotSchematic(PlotPlugin plugin, PlotManager manager) {
        this.plugin = plugin;
        this.manager = manager;

        this.init();
    }

    public void init(Schematic schematic) {
        this.schematic = schematic;
    }

    public void init() {
        final Schematic schematic = this.plugin.getProvider().loadSchematic(this.manager.getLevelName()).join();
        if(schematic == null) return;

        this.init(schematic);
    }

    public void save() {
        if(this.schematic == null) return;

        this.plugin.getProvider().saveSchematic(this.manager.getLevelName(), this.schematic).join();
    }

    public void remove() {
        if(this.schematic == null) return;
        this.schematic = null;

        this.plugin.getProvider().deleteSchematic(this.manager.getLevelName()).join();
    }

}
