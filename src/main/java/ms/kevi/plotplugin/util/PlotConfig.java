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

import ms.kevi.plotplugin.lang.TranslationKey;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Kevims KCodeYT
 */
@Getter
@AllArgsConstructor
public class PlotConfig {

    private final Object defaultValue;
    private final String saveName;
    private final TranslationKey infoTranslationKey;
    private final TranslationKey defaultTranslationKey;

    public Object get(Plot plot) {
        Object o = plot.getConfigValue(this.saveName);
        if(o == null) {
            plot.setConfigValue(this.saveName, this.defaultValue);
            o = this.defaultValue;
        }
        return o;
    }

    public String getAsString(Plot plot) {
        Object o = plot.getConfigValue(this.saveName);
        if(o == null) {
            plot.setConfigValue(this.saveName, this.defaultValue);
            o = this.defaultValue;
        }
        return o.toString();
    }

    public void set(Plot plot, Object value) {
        plot.setConfigValue(this.saveName, value == null ? this.defaultValue : value);
    }

    @Getter
    @AllArgsConstructor
    public enum ConfigEnum {

        DAMAGE(new PlotConfig(false, "damage", TranslationKey.INFO_DAMAGE, TranslationKey.CONFIG_DAMAGE)),
        PVE(new PlotConfig(false, "pve", TranslationKey.INFO_PVE, TranslationKey.CONFIG_PVE)),
        PVP(new PlotConfig(false, "pvp", TranslationKey.INFO_PVP, TranslationKey.CONFIG_PVP));

        private final PlotConfig config;

    }

}
