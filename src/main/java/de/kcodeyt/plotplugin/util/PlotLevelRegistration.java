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

package de.kcodeyt.plotplugin.util;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PlotLevelRegistration {

    private String levelName;
    private boolean defaultLevel;
    private PlotLevelSettings levelSettings;
    private RegistrationState state;

    public PlotLevelRegistration(String levelName, boolean defaultLevel) {
        this.levelName = levelName;
        this.defaultLevel = defaultLevel;
        this.levelSettings = new PlotLevelSettings();
        this.state = RegistrationState.DIMENSION;
    }

    public enum RegistrationState {

        DIMENSION,

        PLOT_BIOME,
        ROAD_BIOME,

        FIRST_LAYER,
        MIDDLE_LAYER,
        LAST_LAYER,

        ROAD,
        ROAD_FILLING,

        WALL_UNOWNED,
        WALL_CLAIMED,
        WALL_FILLING,

        PLOT_SIZE,
        ROAD_SIZE,
        GROUND_HEIGHT

    }

}
