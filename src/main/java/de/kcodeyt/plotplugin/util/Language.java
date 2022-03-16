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

import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;

import java.io.File;
import java.util.Objects;

public class Language {

    private final File file;
    private final Config config;

    public Language(File file) {
        this.file = file;
        this.config = new Config(this.file, Config.PROPERTIES);
        this.init(this.config.getKeys().toArray(new String[0]));
    }

    public void init(String[] messages) {
        for(String message : messages)
            this.translate(message);
    }

    public String translate(String messageId) {
        return this.translate(messageId, new Object[0]);
    }

    public String translate(String messageId, Object... params) {
        String message = this.config.get(messageId, "Not Found");
        if(message.equalsIgnoreCase("Not Found")) {
            this.config.set(messageId, messageId);
            this.config.save(this.file);
        }

        for(int i = 0; i < params.length; i++)
            message = message.replace("{" + i + "}", Objects.toString(params[0]));
        return message.replaceAll("&", "" + TextFormat.ESCAPE);
    }

    public void reload() {
        this.config.reload();
    }

}
