package xyz.nsgw.nsys.commands;

import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.PaperCommandManager;
import org.bukkit.Location;
import xyz.nsgw.nsys.NSys;
import xyz.nsgw.nsys.storage.Home;
import xyz.nsgw.nsys.storage.Profile;

public class CommandHandler {

    public CommandHandler(NSys pl, PaperCommandManager manager) {

        manager.getCommandContexts().registerContext(Home.class, c-> {
            Profile p = pl.sql().wrapProfile(c.getPlayer().getUniqueId());
            Location loc = p.getHome(c.popFirstArg());
            if(loc == null) {
                throw new InvalidCommandArgument("A home could not be found.");
            }
            return new Home(loc);
        });

        manager.getCommandCompletions().registerCompletion("homes",c-> pl.sql().wrapProfile(c.getPlayer().getUniqueId()).getHomes().keySet());
        manager.registerCommand(new HomeCmd());
        manager.registerCommand(new HomesCmd(pl));
        manager.registerCommand(new SetHomeCmd(pl));
        manager.registerCommand(new DelHomeCmd(pl));
    }
}