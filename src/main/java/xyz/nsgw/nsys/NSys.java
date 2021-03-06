/*
 * Copyright (c) Nicholas Williams 2021.
 */

package xyz.nsgw.nsys;

import ch.jalu.configme.SettingsManager;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIConfig;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.nsgw.nsys.commands.CommandHandler;
import xyz.nsgw.nsys.config.SettingsHandler;
import xyz.nsgw.nsys.config.settings.GeneralSettings;
import xyz.nsgw.nsys.config.settings.StartupSettings;
import xyz.nsgw.nsys.listeners.LoadingListener;
import xyz.nsgw.nsys.listeners.PlayerListener;
import xyz.nsgw.nsys.listeners.TpListener;
import xyz.nsgw.nsys.storage.JsonMigration;
import xyz.nsgw.nsys.storage.objects.Profile;
import xyz.nsgw.nsys.storage.objects.SettingsList;
import xyz.nsgw.nsys.storage.objects.SettingsMap;
import xyz.nsgw.nsys.storage.objects.locations.Warp;
import xyz.nsgw.nsys.storage.sql.SQLService;
import xyz.nsgw.nsys.storage.sql.SQLUtils;
import xyz.nsgw.nsys.utils.alerts.StaffAlertHandler;
import xyz.nsgw.nsys.utils.gui.GUIHandler;

import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

public final class NSys extends JavaPlugin {
    private static SQLService sql;
    private static SettingsHandler settingsHandler;
    private static Logger logger;
    private CommandHandler commandHandler;
    private GUIHandler guiHandler;
    private static StaffAlertHandler staffAlertHandler;

    private static Economy econ = null;

    public static String version() {
        return "v1.0.0";
    }

    @Override
    public void onLoad() {
        CommandAPI.onLoad(new CommandAPIConfig().silentLogs(false));
    }

    @Override
    public void onEnable() {

        getLogger().info("Enabling NSys...");

        CommandAPI.onEnable(this);

        if (!setupEconomy() ) {
            getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        setSettingsHandler(new SettingsHandler(this.getDataFolder()));

        SettingsManager startup = settingsHandler.startup();

        if(Objects.equals(startup.getProperty(StartupSettings.MYSQL_HOST), "not set")) {
            getLogger().warning("Please change the database options in general_config.yml accordingly.");
            getLogger().info("Disabling NSys due to missing settings.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        setSql(new SQLService(
                startup.getProperty(StartupSettings.MYSQL_HOST),
                startup.getProperty(StartupSettings.MYSQL_DB),
                startup.getProperty(StartupSettings.MYSQL_USER),
                startup.getProperty(StartupSettings.MYSQL_PASS)));

        setLogger(this.getServer().getLogger());

        Bukkit.getPluginManager().registerEvents(new LoadingListener(this), this);
        Bukkit.getPluginManager().registerEvents(new TpListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(), this);

        JsonMigration migration;
        switch(settingsHandler.startup().getProperty(StartupSettings.MIGRATION_MODE).toLowerCase()) {
            case "load":
                logger.info("Loading data from json...");
                migration = new JsonMigration(this.getDataFolder());
                loadFromJson(migration);
                logger.info("Loaded data from json.");
                logger.info(sql.wrapList("warps").getList().size()+" warps and "
                        + sql.wrapMap("players").getMap().size() + " players.");
            case "save":
                logger.info("Saving data to json...");
                migration = new JsonMigration(this.getDataFolder());
                saveToJson(migration);
                logger.info("Saved data to json.");
                logger.info(sql.wrapList("warps").getList().size()+" warps and "
                        + sql.wrapMap("players").getMap().size() + " players.");
            default:
                logger.info("Skipped DB migration with Json because migration is disabled.");
        }

        guiHandler = new GUIHandler();

        staffAlertHandler = new StaffAlertHandler(this);

        commandHandler = new CommandHandler(this);

        //scheduleRepeatingTask(new Timer(), 10L, 50L);

        log().info("NSys Enabled!");

    }

    private void loadFromJson(JsonMigration migration) {
        SettingsList newWarps = migration.getList("warps");
        sql.validateList(newWarps);
        Warp w;
        for(String name : newWarps.getList()) {
            w = migration.getWarp(name);
            sql.validateWarp(w);
        }
        SettingsMap newPlayers = migration.getMap("players");
        sql.validateMap(newPlayers);
        Profile p;
        for(String uuid : newPlayers.getMap().keySet()) {
            p = migration.getProfile(uuid);
            sql.validateProfile(p);
        }
    }

    private void saveToJson(JsonMigration migration) {
        SettingsList warps = sql.wrapList("warps");
        migration.storeList(warps);
        for(String name : warps.getList()) {
            migration.storeWarp(sql.wrapWarp(name));
        }
        SettingsMap players = sql.wrapMap("players");
        migration.storeMap(players);
        for(String uuid : players.getMap().keySet()) {
            migration.storeProfile(sql.wrapProfile(UUID.fromString(uuid)));
        }
    }

    public SettingsManager getGenSettings() {
        return settingsHandler.gen();
    }

    public GUIHandler guiHandler() {return guiHandler;}

    @Override
    public void onDisable() {
        getLogger().info("Disabling NSys...");
        if(sql != null) {
            sql.invalidateList(sql.wrapList("warps"));
            sql.invalidateMap(sql.wrapMap("players"));
            sql.onDisable();
            SQLUtils.close();
        }
    }

    public static SQLService sql() {
        return sql;
    }

    private static void setSql(SQLService s) {
        sql = s;
    }

    public static SettingsHandler sh(){
        return settingsHandler;
    }

    private void setSettingsHandler(SettingsHandler handler) {
        settingsHandler = handler;
    }

    public static StaffAlertHandler alerts() {return staffAlertHandler;}

    public static Logger log() {
        return logger;
    }
    private void setLogger(final Logger lgr) {
        logger = lgr;
    }

    public static void reload() {
        settingsHandler.gen().reload();
    }

    private void scheduleRepeatingTask(Runnable runnable, final long del, final long per) {
        this.getServer().getScheduler().scheduleSyncRepeatingTask(this, runnable, del, per);
    }

    public static Economy econ() {
        return econ;
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return true;
    }

    public static Warp newWarp(final String name, final Player player, double price) {
        if(price == -1) {
            price = settingsHandler.gen().getProperty(GeneralSettings.PRICE_WARPS);
        } else if (price < 0) {
            logger.severe("Price cannot be less than 0 in NSys/general_settings.yml! Warp not created!");
            return null;
        }
        Warp warp = NSys.sql().wrapWarp(name);
        warp.setOwnerUuid(player.getUniqueId());
        warp.setLocation(player.getLocation());
        warp.setPrice(price);
        NSys.sql().validateWarp(warp);
        NSys.sql().wrapList("warps").add(name);
        return sql.wrapWarp(name);
    }
}
