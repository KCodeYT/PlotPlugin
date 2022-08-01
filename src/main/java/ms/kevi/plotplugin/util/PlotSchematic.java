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
import ms.kevi.plotplugin.manager.PlotManager;
import ms.kevi.plotplugin.schematic.Schematic;

import java.io.File;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
@Getter
public class PlotSchematic {

    private final PlotManager plotManager;
    private Schematic schematic;

    public PlotSchematic(PlotManager plotManager) {
        this.plotManager = plotManager;
    }

    public void init(Schematic schematic) {
        this.schematic = schematic;
    }

    public void init(File file) {
        try {
            if(!file.exists()) return;

            final Schematic schematic = new Schematic();
            schematic.init(file);
            this.init(schematic);
        } catch(Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public void save(File file) {
        try {
            if(this.schematic == null) return;

            if(!file.getParentFile().exists())
                file.getParentFile().mkdirs();
            this.schematic.save(file);
        } catch(Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public void remove(File file) {
        if(this.schematic == null) return;
        this.schematic = null;

        if(!file.exists()) return;
        file.delete();
    }

}
