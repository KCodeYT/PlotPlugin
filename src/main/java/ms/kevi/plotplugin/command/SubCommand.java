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

package ms.kevi.plotplugin.command;

import cn.nukkit.Player;
import cn.nukkit.command.data.CommandParameter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import ms.kevi.plotplugin.PlotPlugin;
import ms.kevi.plotplugin.lang.TranslationKey;

import java.util.*;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
@Getter
public abstract class SubCommand {

    protected final PlotPlugin plugin;
    protected final PlotCommand parent;

    private final String name;
    private final TranslationKey helpTranslationKey;
    private final Set<String> aliases;

    private final Set<CommandParameter> parameters;

    @Setter(AccessLevel.PROTECTED)
    private String permission;

    protected SubCommand(PlotPlugin plugin, PlotCommand parent, String name, String alias) {
        this(plugin, parent, name, new String[]{alias});
    }

    protected SubCommand(PlotPlugin plugin, PlotCommand parent, String name, String... aliases) {
        this.plugin = plugin;
        this.parent = parent;
        this.name = name;
        this.helpTranslationKey = TranslationKey.valueOf("HELP_" + this.name.toUpperCase(Locale.ROOT));
        this.aliases = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        this.aliases.add(name);
        this.aliases.addAll(Arrays.asList(aliases));
        this.parameters = new LinkedHashSet<>();
    }

    public boolean hasPermission(Player player) {
        return this.permission == null || player.hasPermission(this.permission);
    }

    public abstract boolean execute(Player player, String[] args);

    protected void addParameter(CommandParameter parameter) {
        this.parameters.add(parameter);
    }

    protected String translate(Player player, TranslationKey key) {
        return this.parent.translate(player, key);
    }

    protected String translate(Player player, TranslationKey key, Object... params) {
        return this.parent.translate(player, key, params);
    }

}
