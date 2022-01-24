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

package de.kcodeyt.plotplugin.command;

import cn.nukkit.Player;
import cn.nukkit.command.data.CommandParameter;
import de.kcodeyt.plotplugin.PlotPlugin;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

@Getter
public abstract class SubCommand {

    protected final PlotPlugin plugin;

    private final String name;
    private final Set<String> aliases;

    private final Set<CommandParameter> parameters;

    @Setter(AccessLevel.PROTECTED)
    private String permission;

    protected SubCommand(PlotPlugin plugin, String name, String alias) {
        this(plugin, name, new String[]{alias});
    }

    protected SubCommand(PlotPlugin plugin, String name, String... aliases) {
        this.plugin = plugin;
        this.name = name;
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

    protected String translate(String message) {
        return this.plugin.getLanguage().translate(message);
    }

    protected String translate(String message, Object... params) {
        return this.plugin.getLanguage().translate(message, params);
    }

}
