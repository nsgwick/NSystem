/*
 * Copyright (c) Nicholas Williams 2021.
 */

package xyz.nsgw.nsys.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import xyz.nsgw.nsys.NSys;

@CommandAlias("nsys")
@CommandPermission("nsys.cmd.admin")
public class MainCmd extends BaseCommand {

    @Default
    public void onVer(CommandSender s) {
        s.sendMessage("NSys installation: " + NSys.version());
    }

    @Subcommand("reload|rel|re|r")
    public void onReload(CommandSender s) {
        NSys.reload();
        s.sendMessage(ChatColor.BLUE + "The general_config for NSys has been reloaded.");
    }

    @HelpCommand
    public void onHelp(CommandSender s, CommandHelp help) {
        help.showHelp();
    }

}
