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

import lombok.experimental.UtilityClass;
import ms.kevi.plotplugin.PlotPlugin;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * @author Kevims KCodeYT
 */
@UtilityClass
public class RewriteUtil {

    public void rewriteIfOld(PlotPlugin plugin, File configFile, String levelName) {
        final LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setMaxAliasesForCollections(Integer.MAX_VALUE);
        final Yaml yaml = new Yaml(loaderOptions);

        Map<String, Object> map;
        try(final FileInputStream inputStream = new FileInputStream(configFile)) {
            //noinspection unchecked
            map = yaml.loadAs(inputStream, Map.class);
        } catch(IOException e) {
            plugin.getLogger().error("Could not read config file for level " + levelName + "!", e);
            map = Collections.emptyMap();
        }

        //TODO: Load "plots" with fromConfig and insert them into database.
        //TODO: Load "Settings" and insert them into config directly.
    }

}
