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
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import de.kcodeyt.plotplugin.PlotPlugin;
import de.kcodeyt.plotplugin.command.defaults.*;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class PlotCommand extends Command {

    private final PlotPlugin plugin;
    private final Set<SubCommand> subCommands;

    public PlotCommand(PlotPlugin plugin) {
        super("plot", "The Plot Command", "", new String[]{"p"});
        this.plugin = plugin;
        this.subCommands = new LinkedHashSet<>();

        this.subCommands.add(new AddHelperCommand(this.plugin));
        this.subCommands.add(new AutoCommand(this.plugin));
        this.subCommands.add(new ClaimCommand(this.plugin));
        this.subCommands.add(new ClearCommand(this.plugin));
        this.subCommands.add(new DenyCommand(this.plugin));
        this.subCommands.add(new DisposeCommand(this.plugin));
        this.subCommands.add(new GenerateCommand(this.plugin));
        this.subCommands.add(new HomeCommand(this.plugin));
        this.subCommands.add(new InfoCommand(this.plugin));
        this.subCommands.add(new MergeCommand(this.plugin));
        this.subCommands.add(new RegenAllRoadsCommand(this.plugin));
        this.subCommands.add(new RegenRoadCommand(this.plugin));
        this.subCommands.add(new ReloadCommand(this.plugin));
        this.subCommands.add(new RemoveHelperCommand(this.plugin));
        this.subCommands.add(new SetOwnerCommand(this.plugin));
        this.subCommands.add(new SetRoadsCommand(this.plugin));
        this.subCommands.add(new SettingCommand(this.plugin));
        this.subCommands.add(new TeleportCommand(this.plugin));
        this.subCommands.add(new UndenyCommand(this.plugin));
        this.subCommands.add(new UnlinkCommand(this.plugin));
        this.subCommands.add(new WarpCommand(this.plugin));
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        final String subName = args.length > 0 ? args[0] : "";
        args = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];

        if(!(sender instanceof Player)) {
            sender.sendMessage("Sorry, this command is only available for players!");
            return false;
        }

        final Player player = (Player) sender;

        for(SubCommand subCommand : this.subCommands) {
            if(!subCommand.hasPermission(player)) continue;
            if(subCommand.getAliases().contains(subName))
                return subCommand.execute(player, args);
        }

        player.sendMessage(this.translate("help-title"));
        for(SubCommand subCommand : this.subCommands)
            if(subCommand.hasPermission(player)) player.sendMessage(this.translate("help-" + subCommand.getName()));
        player.sendMessage(this.translate("help-end"));
        return true;
    }

    private String translate(String message) {
        return this.plugin.getLanguage().translate(message);
    }

}
