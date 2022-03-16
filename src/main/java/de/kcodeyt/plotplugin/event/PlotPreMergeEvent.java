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

package de.kcodeyt.plotplugin.event;

import cn.nukkit.Player;
import cn.nukkit.event.Cancellable;
import cn.nukkit.event.HandlerList;
import de.kcodeyt.plotplugin.util.Plot;
import lombok.Getter;

@Getter
public class PlotPreMergeEvent extends PlotEvent implements Cancellable {

    @Getter
    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final int direction;

    public PlotPreMergeEvent(Player player, Plot plot, int direction) {
        super(plot);
        this.player = player;
        this.direction = direction;
    }

}