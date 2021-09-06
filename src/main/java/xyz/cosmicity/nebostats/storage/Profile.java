/*
© Copyright Nick Williams 2021.
Credit should be given to the original author where this code is used.
 */

package xyz.cosmicity.nebostats.storage;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Profile {

    private final UUID key;
    private String discord;
    private boolean trackingTeleports;

    private List<String> homesRaw;

    private List<String> privateNotes;

    private HashMap<String, Location> homes;

    private Date muteFrom;
    private int muteSeconds;
    private boolean shadowMute;

    private Date lastChat;


    public Profile(final UUID uuid) {
        key = uuid;
        discord = "";
        trackingTeleports = false;
        homesRaw = new ArrayList<>();
        homes = new HashMap<>();
        privateNotes = new ArrayList<>();
        muteFrom = null;
        muteSeconds = -1;
        lastChat = null;
        shadowMute = false;
    }

    public UUID getUuid() {
        return key;
    }
    public void setDiscord(final String dId) {
        discord = dId;
    }
    public String getDiscord() {
        return discord;
    }

    public void setHomes(String raw) {
        homes = new HashMap<>();
        homesRaw = Arrays.asList(raw.split(";"));
        String[] vals1;
        String[][] vals2;
        // homename:worlduuid,x,y,z,yaw,pitch;
        for(String home : homesRaw) {
            vals1 = home.split(":");
            vals2 = Arrays.stream(vals1).map(s->s.split(",")).collect(Collectors.toList()).toArray(String[][]::new);
            homes.put(vals2[0][0],new Location(Bukkit.getWorld(UUID.fromString(vals2[1][0])),
                    Double.parseDouble(vals2[1][1]),
                    Double.parseDouble(vals2[1][2]),
                    Double.parseDouble(vals2[1][3]),
                    Float.parseFloat(vals2[1][4]),
                    Float.parseFloat(vals2[1][5])));
        }
    }

    public String getHomesString() {
        StringBuilder raw = new StringBuilder();
        Location location;
        String data;
        for(String homeName : homes.keySet()) {
            location = homes.get(homeName);
            data = homeName + ":" + location.getX() +","+ location.getY() +","+ location.getZ() +"," + location.getYaw() +","+ location.getPitch() +";";
            raw.append(data);
        }
        return raw.toString();
    }

    public void setHome(final String homeName, final Location location) {
        homes.put(homeName, location);
    }
    public void delHome(final String homeName) {
        homes.remove(homeName);
    }

    public Location getHome(final String homeName) {
        if(!homes.containsKey(homeName)) return null;
        return homes.get(homeName);
    }
    public boolean hasHome(String name) {
        return homes.containsKey(name);
    }
    public HashMap<String,Location> getHomes() {
        return homes;
    }

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
        return Long.toString(muteFrom.getTime())+"/"+muteSeconds;
    }
    public void setMute(String m) {
        muteFrom = Date.from(Instant.ofEpochMilli(Long.parseLong(m.split("/")[0])));
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
    public long getSecsSinceChat(){return TimeUnit.SECONDS.convert(Math.abs((new Date()).getTime()- lastChat.getTime()),TimeUnit.MILLISECONDS);}
    public void recordChatAttempt(){lastChat=new Date();}

    public Profile loadAttributes(final SQLTable table) {
        List<Object> row = SQLUtils.getRow(table, "\""+key.toString()+"\"",Arrays.stream(DbData.PROFILE_COLUMNS).map(c->c[0]).collect(Collectors.toList()).toArray(String[]::new));
        // DISCORD , TRACK TP , HOMES , NOTES , MUTE
        setDiscord((String) row.get(0));
        setTrackingTeleports((Boolean) row.get(1));
        setHomes((String) row.get(2));
        setPrivateNotes((String) row.get(3));
        setMute((String) row.get(4));
        return this;
    }
}
