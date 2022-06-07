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
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import ms.kevi.plotplugin.PlotPlugin;
import ms.kevi.plotplugin.command.defaults.*;
import ms.kevi.plotplugin.lang.TranslationKey;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Kevims KCodeYT
 * @version 1.0
 */
public class PlotCommand extends Command {

    private final PlotPlugin plugin;
    private final Set<SubCommand> subCommands;

    public PlotCommand(PlotPlugin plugin) {
        super("plot", "The Plot Command", "", new String[]{"p"});
        this.plugin = plugin;
        this.subCommands = new LinkedHashSet<>();

        this.subCommands.add(new AddHelperCommand(this.plugin, this));
        this.subCommands.add(new AutoCommand(this.plugin, this));
        this.subCommands.add(new ClaimCommand(this.plugin, this));
        this.subCommands.add(new ClearCommand(this.plugin, this));
        this.subCommands.add(new DenyCommand(this.plugin, this));
        this.subCommands.add(new DisposeCommand(this.plugin, this));
        this.subCommands.add(new GenerateCommand(this.plugin, this));
        this.subCommands.add(new HomeCommand(this.plugin, this));
        this.subCommands.add(new HomesCommand(this.plugin, this));
        this.subCommands.add(new InfoCommand(this.plugin, this));
        this.subCommands.add(new KickCommand(this.plugin, this));
        this.subCommands.add(new MergeCommand(this.plugin, this));
        this.subCommands.add(new RegenAllRoadsCommand(this.plugin, this));
        this.subCommands.add(new RegenRoadCommand(this.plugin, this));
        this.subCommands.add(new ReloadCommand(this.plugin, this));
        this.subCommands.add(new RemoveHelperCommand(this.plugin, this));
        this.subCommands.add(new SetOwnerCommand(this.plugin, this));
        this.subCommands.add(new SetRoadsCommand(this.plugin, this));
        this.subCommands.add(new SettingCommand(this.plugin, this));
        this.subCommands.add(new TeleportCommand(this.plugin, this));
        this.subCommands.add(new UndenyCommand(this.plugin, this));
        this.subCommands.add(new UnlinkCommand(this.plugin, this));
        this.subCommands.add(new WarpCommand(this.plugin, this));
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        final String subName = args.length > 0 ? args[0] : "";
        args = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];

        if(!(sender instanceof final Player player)) {
            sender.sendMessage("Sorry, this command is only available for players!");
            return false;
        }

        for(SubCommand subCommand : this.subCommands) {
            if(!subCommand.hasPermission(player)) continue;
            if(subCommand.getAliases().contains(subName))
                return subCommand.execute(player, args);
        }

        player.sendMessage(this.translate(player, TranslationKey.HELP_TITLE));
        for(SubCommand subCommand : this.subCommands)
            if(subCommand.hasPermission(player))
                player.sendMessage(this.translate(player, subCommand.getHelpTranslationKey()));
        player.sendMessage(this.translate(player, TranslationKey.HELP_END));
        return true;
    }

    protected String translate(Player player, TranslationKey key) {
        return this.plugin.getLanguage().translate(player, key);
    }

    protected String translate(Player player, TranslationKey key, Object... params) {
        return this.plugin.getLanguage().translate(player, key, params);
    }

}
