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

package ms.kevi.plotplugin.event;

import cn.nukkit.Player;
import cn.nukkit.event.HandlerList;
import ms.kevi.plotplugin.util.Plot;
import lombok.Getter;

/**
 * @author Kevims KCodeYT
 */
@Getter
public class PlotMergeEvent extends PlotEvent {

    @Getter
    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final int direction;

    public PlotMergeEvent(Player player, Plot plot, int direction) {
        super(plot);
        this.player = player;
        this.direction = direction;
    }

}
