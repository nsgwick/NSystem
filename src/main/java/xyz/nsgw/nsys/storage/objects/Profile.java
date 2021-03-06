/*
 * Copyright (c) Nicholas Williams 2021.
 */

package xyz.nsgw.nsys.storage.objects;

import dev.jorel.commandapi.CommandAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import xyz.nsgw.nsys.NSys;
import xyz.nsgw.nsys.config.settings.GeneralSettings;
import xyz.nsgw.nsys.storage.objects.locations.Home;
import xyz.nsgw.nsys.storage.sql.DbData;
import xyz.nsgw.nsys.storage.sql.SQLTable;
import xyz.nsgw.nsys.storage.sql.SQLUtils;
import xyz.nsgw.nsys.utils.ArithmeticUtils;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Profile {

    private final UUID key;
    private String discord;
    private boolean trackingTeleports;
    private int trackingTeleportsLevel;

    private List<String> privateNotes;

    private List<String> staffAlerts;//todo

    private HashMap<String, Home> homes;

    private List<String> ownedWarps;

    private final int maxHomes;

    private int maxLogins;

    private Date muteFrom;
    private int muteSeconds;
    private boolean shadowMute;

    private boolean afk;
    private Location afkLocation;

    private Date lastActive;

    private String lastName;

    public Profile(final UUID uuid) {
        key = uuid;
        discord = "";
        trackingTeleports = true;
        trackingTeleportsLevel = NSys.sh().gen().getProperty(GeneralSettings.TRACKTP_PLAYER_MODE);
        homes = new HashMap<>();
        privateNotes = new ArrayList<>();
        muteFrom = null;
        muteSeconds = -1;
        shadowMute = false;
        maxHomes = NSys.sh().gen().getProperty(GeneralSettings.HOMES_MAXIMUM_DEFAULT);
        maxLogins = 0;
        afk = false;
        afkLocation = Bukkit.getWorlds().get(0).getSpawnLocation();
        lastActive = new Date();
        lastName = "";
        ownedWarps = new ArrayList<>();
    }

    public Player getBase() {
        return Bukkit.getPlayer(this.key);
    }

    public UUID getKey() {
        return key;
    }
    public boolean isOnline() {
        return Bukkit.getPlayer(key)!=null;
    }
    public Player player() {
        return Bukkit.getPlayer(key);
    }
    public OfflinePlayer offlinePlayer() {
        return Bukkit.getOfflinePlayer(key);
    }
    public boolean isBanned() {
        return Bukkit.getOfflinePlayer(key).isBanned();
    }
    public void setDiscord(final String dId) {
        discord = dId;
    }
    public String getDiscord() {
        return discord;
    }

    public int getMaxHomes() {
        return maxHomes;
    }
    public void setHomes(String raw) {
        homes = new HashMap<>();
        if(raw.isEmpty()) return;
        String[] values;
        String name;
        // homename:worlduuid,x,y,z,yaw,pitch;
        for(String home : raw.split(";")) {
            values = home.split(":");
            name = values[0].split(",")[0];
            homes.put(name,new Home(SQLUtils.stringToLocation(values[1]), name));
        }
    }
    public void setHomesMap(HashMap<String,Home> hs) {
        this.homes = hs;
    }

    public String getHomesString() {
        if(homes.isEmpty()) return "";
        StringBuilder raw = new StringBuilder();
        String data;
        for(String homeName : homes.keySet()) {
            data = homeName + ":" + SQLUtils.locationToString(homes.get(homeName)) +";";
            raw.append(data);
        }
        return raw.toString();
    }

    public boolean setHome(final String homeName, final Location location) {
        if(homes.size() < maxHomes || (homes.size() == maxHomes && homes.containsKey(homeName))) {
            homes.put(homeName, new Home(location, homeName));
            return true;
        }
        return false;
    }
    public void setHomeHere(final String homeName) {
        Player p = this.getBase();
        if(homeName.contains(":")) {
            p.sendMessage(ChatColor.RED + "Home names cannot contain ':'.");
        } else if(this.setHome(homeName, p.getLocation())) {
            CommandAPI.updateRequirements(p);
            p.sendMessage(ChatColor.GREEN + "Home '" + homeName + "' set.");
        } else {
            p.sendMessage(ChatColor.RED + "You have reached your maximum amount of homes.");
        }
    }
    public Home delHome(final String homeName) {
        return homes.remove(homeName);
    }

    @Nullable
    public Home getHome(final String homeName) {
        if(!homes.containsKey(homeName)) return null;
        return homes.get(homeName);
    }
    public Home getHomeAtIndex(final int index) {
        if(homes.size() < index) return null;
        return homes.get(homes.keySet().toArray(String[]::new)[index]);
    }
    public boolean hasHome(String name) {
        return homes.containsKey(name);
    }
    public HashMap<String,Home> getHomes() {
        return homes;
    }
    public Set<String> getHomeNames() {return homes.keySet();}

    // Private notes
    public List<String> getPrivateNotes() {
        return privateNotes;
    }
    public String getPrivateNotesString() {
        return String.join(DbData.NOTES_SEP,privateNotes);
    }
    public void setPrivateNotes(String notes) {
        setPrivateNotes(Arrays.asList(notes.split(DbData.NOTES_SEP)));
    }
    public void setPrivateNotes(List<String> privateNotes) {
        this.privateNotes = privateNotes;
    }

    // Tracking teleports
    public boolean isTrackingTeleports() {return trackingTeleports;}
    public void setTrackingTeleports(boolean track) {trackingTeleports = track;}

    // Muting
    public String getMute() {
        return (muteFrom==null?"none":muteFrom.getTime())+"/"+muteSeconds;
    }
    public void setMute(String m) {
        String mFrom = m.split("/")[0];
        muteFrom = Objects.equals(mFrom, "none") ? null : Date.from(Instant.ofEpochMilli(Long.parseLong(mFrom)));
        muteSeconds = Integer.parseInt(m.split("/")[1]);
    }
    public boolean isMuted() {return muteFrom!=null && muteSeconds>-1;}
    public boolean isShadowMute() {return shadowMute;}
    public void setShadowMute(boolean sm) {shadowMute=sm;}
    public boolean checkMute() {
        if(muteSeconds == -1 || getMuteSecsPassed()>muteSeconds || muteFrom==null) {
            setMuted(false);
            return false;
        }return true;
    }
    public double getMuteSecsPassed() {
        if(muteFrom==null) return 0;
        return TimeUnit.SECONDS.convert(Math.abs((new Date()).getTime()-muteFrom.getTime()),TimeUnit.MILLISECONDS);
    }
    public Date getMuteFrom() {return muteFrom;}
    public void setMuteSeconds(int s){muteSeconds=s;}
    public void setMuted(boolean m) {muteFrom=(m?new Date():null);}

    public void login(String ign) {
        maxLogins++;
        setLastName(ign);
        updateActivity();
    }
    public void setMaxLogins(int mx) {
        maxLogins = mx;
    }
    public int getMaxLogins() {
        return maxLogins;
    }

    public void setAfk(boolean a, boolean silent) {
        if(!silent) Bukkit.broadcast(Component.text(ChatColor.GRAY+lastName+" is "+(a?"now":"no longer")+" AFK."));
        afk = a;
    }

    public boolean isAfk() {
        return afk;
    }

    public Location getAfkLocation() {
        return afkLocation;
    }

    public void setAfkLocation(Location afkLocation) {
        this.afkLocation = afkLocation;
    }

    public Date getLastActive() {
        return lastActive;
    }

    public void setLastActive(Date lA) {
        lastActive = lA;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String ign) {
        lastName = ign;
    }

    public void updateActivity() {
        lastActive = new Date();
        if(afk) setAfk(false,false);
    }
    public void updateActivitySilently() {
        lastActive = new Date();
    }

    public void setTrackingTeleportsLevel(int l) {
        trackingTeleportsLevel = l;
    }
    public int getTrackingTeleportsLevel() {
        return trackingTeleportsLevel;
    }

    public List<String> getOwnedWarps() {
        return ownedWarps;
    }
    public String getOwnedWarpsString() {
        return this.ownedWarps.isEmpty() ? "" : String.join(";",this.ownedWarps);
    }

    public void setOwnedWarps(String raw) {
        this.ownedWarps = new ArrayList<>();
        if(raw.isEmpty()) return;
        this.ownedWarps.addAll(Arrays.stream(raw.split(";")).toList());
    }

    public Profile loadAttributes(final SQLTable table) {
        List<Object> row = SQLUtils.getRow(table, "\""+key.toString()+"\"",Arrays.stream(DbData.PROFILE_COLUMNS).map(c->c[0]).collect(Collectors.toList()).toArray(String[]::new));
        // DISCORD , TRACK TP , HOMES , NOTES , MUTE, LOGINS, AFK LOC
        setDiscord((String) row.get(0));
        setTrackingTeleports((Boolean) row.get(1));
        setHomes((String) row.get(2));
        setPrivateNotes((String) row.get(3));
        setMute((String) row.get(4));
        setMaxLogins((Integer) row.get(5));
        setAfk((Boolean) row.get(6), true);
        setAfkLocation(SQLUtils.stringToLocation((String) row.get(7)));
        setLastActive(ArithmeticUtils.dateFromStr((String) row.get(8)));
        setLastName((String) row.get(9));
        setTrackingTeleportsLevel((Integer) row.get(10));
        setOwnedWarps(row.get(11) == null ? "" : (String) row.get(11));
        return this;
    }

    public Object[] getDbValues() {
        return new Object[] {
                // DISCORD , TRACK TP , HOMES , NOTES , MUTE
                /*DISCORD*/getDiscord(),
                /*TRACK TP*/SQLUtils.getBoolInSQL(isTrackingTeleports()),
                /*HOMES*/getHomesString(),
                /*NOTES*/getPrivateNotesString(),
                /*MUTE*/getMute(),
                /*MAXLOGINS*/getMaxLogins(),
                /*IS AFK*/SQLUtils.getBoolInSQL(isAfk()),
                /*LAST LOCATION*/SQLUtils.locationToString(afkLocation),
                /*LAST ACTIVE*/Long.toString(getLastActive().getTime()),
                /*LAST NAME*/getLastName(),
                /*TRACK TP LVL*/getTrackingTeleportsLevel(),
                /*OWNED WARPS*/getOwnedWarpsString()
        };
    }
}
