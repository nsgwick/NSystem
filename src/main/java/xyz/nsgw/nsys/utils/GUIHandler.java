package xyz.nsgw.nsys.utils;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import dev.triumphteam.gui.guis.ScrollingGui;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import xyz.nsgw.nsys.NSys;
import xyz.nsgw.nsys.storage.objects.Profile;
import xyz.nsgw.nsys.storage.objects.locations.Warp;

import static xyz.nsgw.nsys.utils.DisplayUtils.txt;

public class GUIHandler {

    private PaginatedGui menu;

    public GUIHandler() {
    }

    public void profile(Profile profile, Player player, boolean self) {

        OfflinePlayer human = self ? player : Bukkit.getOfflinePlayer(profile.getKey());
        String name = human.getName();

        ScrollingGui gui = Gui.scrolling()
                .title(txt(ChatColor.RED+human.getName()+"'s profile"))
                .rows(6)
                .pageSize(45)
                .disableAllInteractions()
                .create();

        GuiItem skull = ItemBuilder.skull().owner(human)
                .name(txt(ChatColor.YELLOW+name))
                .lore(DisplayUtils.loreProfileMeta(profile, human))
                .asGuiItem();
        skull.setAction(e -> {
            gui.close(player);
            player.performCommand("nsys:profile");
        });

        gui.setItem(1,1,skull);

        gui.open(player);
    }

    public void homes(Profile profile, Player player) {
        PaginatedGui homes = Gui.paginated()
                .title(Component.text("Your Homes"))
                .rows(4)
                .create();
        Location home;
        for(String name : profile.getHomes().keySet()) {
            home = profile.getHome(name);
            assert home != null;
            final String asStr = LocationUtils.simplifyLocation(home);
            homes.addItem(new GuiItem(ItemBuilder.from(Material.ITEM_FRAME)
                    .name(Component.text(ChatColor.LIGHT_PURPLE+""+ChatColor.BOLD+ name))
                    .build(), event -> {
                Location loc = profile.getHome(name);
                if(loc==null) return;
                homes.close(player);
                player.teleport(loc);
            }));
        }
        homes.open(player);
    }

    public void warps(Player player) {
        PaginatedGui warps = Gui.paginated()
                .title(Component.text("Warps"))
                .rows(4)
                .create();
        Warp w;
        for(String name : NSys.sql().wrapList("warps").getList()) {
            w = NSys.sql().wrapWarp(name);
            warps.addItem(new GuiItem(ItemBuilder.from(Material.ENDER_PEARL)
                    .name(Component.text(ChatColor.LIGHT_PURPLE+""+ChatColor.BOLD+ name))
                    .lore(Component.text(ChatColor.WHITE+"Owned by "+Bukkit.getOfflinePlayer(w.getOwnerUuid()).getName()))
                    .build(), event -> {
                Warp loc = NSys.sql().wrapWarp(name);
                if(loc==null) return;
                warps.close(player);
                loc.teleport(player);
            }));
        }
        warps.open(player);
    }
}
