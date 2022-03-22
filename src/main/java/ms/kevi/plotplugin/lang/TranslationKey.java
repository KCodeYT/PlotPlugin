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

package ms.kevi.plotplugin.lang;

import lombok.Getter;

import java.util.Locale;

/**
 * @author Kevims KCodeYT
 */
public enum TranslationKey {

    ACTIVATED,
    ADDED_HELPER,
    ALREADY_HELPER,
    AUTO_FAILURE,
    AUTO_SUCCESS,
    CLAIM_FAILURE,
    CLAIM_SUCCESS,
    CLEAR_FAILURE,
    CLEAR_SUCCESS,
    CONFIG_DAMAGE,
    CONFIG_HELP_DAMAGE,
    CONFIG_HELP_END,
    CONFIG_HELP_PVE,
    CONFIG_HELP_PVP,
    CONFIG_HELP_TITLE,
    CONFIG_PVE,
    CONFIG_PVP,
    DEACTIVATED,
    DENY_FAILURE,
    DENY_SUCCESS,
    DISPOSE_FAILURE,
    DISPOSE_SUCCESS,
    GENERATE_DIMENSION,
    GENERATE_FAILURE,
    GENERATE_FIRST_LAYER,
    GENERATE_GROUND_HEIGHT,
    GENERATE_LAST_LAYER,
    GENERATE_MIDDLE_LAYER,
    GENERATE_PLOT_BIOME,
    GENERATE_PLOT_SIZE,
    GENERATE_ROAD,
    GENERATE_ROAD_BIOME,
    GENERATE_ROAD_FILLING,
    GENERATE_ROAD_SIZE,
    GENERATE_START,
    GENERATE_SUCCESS,
    GENERATE_SUCCESS_DEFAULT,
    GENERATE_WALL_CLAIMED,
    GENERATE_WALL_FILLING,
    GENERATE_WALL_UNOWNED,
    HELP_ADDHELPER,
    HELP_AUTO,
    HELP_CLAIM,
    HELP_CLEAR,
    HELP_DENY,
    HELP_DISPOSE,
    HELP_END,
    HELP_GENERATE,
    HELP_HOME,
    HELP_INFO,
    HELP_MERGE,
    HELP_REGENALLROADS,
    HELP_REGENROAD,
    HELP_RELOAD,
    HELP_REMOVEHELPER,
    HELP_SETOWNER,
    HELP_SETROADS,
    HELP_SETTING,
    HELP_TELEPORT,
    HELP_TITLE,
    HELP_UNDENY,
    HELP_UNLINK,
    HELP_WARP,
    HOME_FAILURE,
    HOME_FAILURE_DENIED,
    HOME_FAILURE_ID,
    HOME_FAILURE_OWN,
    HOME_FAILURE_OWN_ID,
    HOME_SUCCESS,
    HOME_SUCCESS_OWN,
    INFO_DAMAGE,
    INFO_DENIED,
    INFO_END,
    INFO_FAILURE,
    INFO_HELPERS,
    INFO_ID,
    INFO_OWNER,
    INFO_PVE,
    INFO_PVP,
    INFO_TITLE,
    MERGE_FAILURE_ALREADY_MERGED,
    MERGE_FAILURE_NO_PLOTS_FOUND,
    MERGE_FAILURE_OWNER,
    MERGE_SUCCESS,
    NO_HELPER,
    NO_PERMS,
    NO_PLAYER,
    NO_PLOT,
    NO_PLOT_ID,
    NO_PLOT_OWNER,
    NO_PLOT_WORLD,
    NO_WORLD,
    PLAYER_SELF,
    PLOT_POPUP_NO_OWNER,
    PLOT_POPUP_OWNER,
    REGENALLROADS_FINISHED,
    REGENALLROADS_START,
    REGENROAD_FINISHED,
    REGENROAD_START,
    RELOAD_FAILURE,
    RELOAD_SUCCESS,
    REMOVED_HELPER,
    SETOWNER_SUCCESS,
    SETOWNER_SUCCESS_TARGET,
    SETROADS_FINISHED,
    SETROADS_NO_ROAD_FOUND,
    SETROADS_ROAD_REMOVED,
    SETROADS_STARTING,
    TELEPORT_FORM_TITLE,
    TELEPORT_NO_PLOT_WORLD,
    TELEPORT_SUCCESS,
    UNDENY_FAILURE,
    UNDENY_SUCCESS,
    UNLINK_FAILURE,
    UNLINK_SUCCESS,
    WARP_FAILURE,
    WARP_FAILURE_FREE,
    WARP_SUCCESS;

    @Getter
    private final String key;

    TranslationKey() {
        this.key = this.name().toLowerCase(Locale.ROOT).replace("_", "-");
    }

}
