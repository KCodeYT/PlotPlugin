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

import cn.nukkit.Player;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;
import lombok.Getter;
import lombok.Value;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
@Getter
public class Language {

    private final File directory;

    private final Locale defaultLang;
    private final Map<Locale, LangEntry> languages;

    public Language(File directory, String defaultLang) throws IOException {
        this.directory = directory;
        this.defaultLang = Locale.forLanguageTag(defaultLang.replace('_', '-'));
        this.languages = new LinkedHashMap<>();

        final File[] files = directory.listFiles();
        if(files == null || files.length == 0) throw new IOException("Could not find any language file!");

        for(File file : files) {
            final String name = file.getName();
            final int splitIndex = name.lastIndexOf('.');
            if(splitIndex == -1)
                throw new IOException("Could not find ending of file: '" + name + "'!");

            final String language = name.substring(0, splitIndex);
            final Locale locale = Locale.forLanguageTag(language.replace('_', '-'));

            final LangEntry entry = new LangEntry(locale, new Config(file, Config.PROPERTIES));
            entry.reload();
            this.languages.put(locale, entry);
        }

        if(!this.languages.containsKey(this.defaultLang))
            throw new IOException("Could not find default language!");
    }

    public Locale getLocaleByPlayer(Player player) {
        return Locale.forLanguageTag(player.getLoginChainData().getLanguageCode().replace('_', '-'));
    }

    public String translate(Player player, TranslationKey key) {
        return this.translate(player, key, new Object[0]);
    }

    public String translate(Player player, TranslationKey key, Object... params) {
        final LangEntry langEntry = this.languages.getOrDefault(this.getLocaleByPlayer(player), this.languages.get(this.defaultLang));

        String message = langEntry.getTranslations().get(key).
                replace("&", "" + TextFormat.ESCAPE).
                replace("\\n", "\n");
        for(int i = 0; i < params.length; i++)
            message = message.replace("{" + i + "}", Objects.toString(params[i]));

        return message;
    }

    public void reload() throws IOException {
        for(LangEntry langEntry : this.languages.values())
            langEntry.reload();
    }

    @Value
    private static class LangEntry {
        Locale locale;
        Config config;
        Map<TranslationKey, String> translations = new EnumMap<>(TranslationKey.class);

        protected void reload() throws IOException {
            if(!this.translations.isEmpty()) this.config.reload();

            for(TranslationKey key : TranslationKey.values()) {
                final String translation = this.config.getString(key.getKey());
                if(translation == null)
                    throw new IOException("Could not find translation for '" + key + "' in language '" + this.locale + "'!");

                this.translations.put(key, translation);
            }
        }
    }

}
