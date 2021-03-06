/*
 * Copyright (c) Nicholas Williams 2021.
 */

package xyz.nsgw.nsys.storage.sql;

import co.aikar.idb.DB;
import co.aikar.idb.Database;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import xyz.nsgw.nsys.NSys;
import xyz.nsgw.nsys.storage.objects.Icon;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SQLUtils {

    private static Database db;

    public static void setDb(Database dbase) {
        db = dbase;
        DB.setGlobalDatabase(db);
    }
    public static Database getDb() {
        return db;
    }
    public static void close() {
        db.close();
    }

    public static List<Object> getRow(@NotNull final SQLTable table, @NotNull final String key, @NotNull final String... columns) {

        List<Object> objects = new ArrayList<>();

        try (Connection con = db.getConnection();
             PreparedStatement pst = con.prepareStatement("SELECT * FROM " + table.getName() + " WHERE "+table.getPkLabel()+"="+key+";");
             ResultSet rs = pst.executeQuery();){

            if(columns.length == 0) {
                int i=1;
                while (rs.next()) {
                    objects.add(rs.getObject(i));
                    i++;
                }
            }
            else {
                while(rs.next()) {
                    for(String s : columns) {
                        objects.add(rs.getObject(s));
                    }
                }
            }

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return objects;
    }

    /**
     * @param values - the values only. (just values not colLabel=value etc)
     */
    public static void setRow(final SQLTable table, final String key, final Object... values) {
        List<String> columnLabels = table.getColLabels(),
        equivalents = new ArrayList<>();
        for(String lbl : columnLabels) {
            equivalents.add(lbl + " = ?");
        }
        List<Object> objs = new ArrayList<>();
        objs.add(key);
        objs.add(values);
        objs.add(values);
        Bukkit.getServer().getLogger().info("INSERT INTO "+table.getName()+" ("+table.getPkLabel()+","+String.join(",",columnLabels) + ") VALUES (?" + ",?".repeat(columnLabels.size())+")" +
                " ON DUPLICATE KEY UPDATE " + String.join(", ",equivalents));
        db.createTransaction(stm -> {
            stm.executeUpdateQuery("INSERT INTO "+table.getName()+" ("+table.getPkLabel()+","+String.join(",",columnLabels) + ") VALUES (?" + ",?".repeat(columnLabels.size())+")" +
                    " ON DUPLICATE KEY UPDATE " + String.join(", ",equivalents) + ";", objs.toArray(Object[]::new));
            return true;
        });
    }

    public static void delRow(final SQLTable table, final String key) {
        db.createTransaction(stm -> {
            stm.executeUpdateQuery("DELETE FROM "+table.getName()+" WHERE "+table.getPkLabel()+"="+key+";");
            return true;
        });
    }

    public static void update(@Language("SQL") final String query) {
        try {
            db.executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public static void updateAndIgnore(@Language("SQL") final String query) {
        try {
            Bukkit.getLogger().info(query);
            db.executeUpdate(query);
        } catch (SQLException e) {e.printStackTrace();}
    }

    public static void update(@NotNull final String SQL_QUERY, Object... objects) {
        try (Connection con = db.getConnection();
             PreparedStatement pst = con.prepareStatement(SQL_QUERY);) {

            for(int i=1;i < objects.length;i++) {
                pst.setObject(i,objects[i]);
            }
            pst.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean holdsKey(final SQLTable table, final String key) {
        int size = 0;

        try(
                Connection con = db.getConnection();
                PreparedStatement pst = con.prepareStatement("SELECT * FROM "+ table.getName() +" WHERE "+ table.getPkLabel()+"="+key+";");
                ResultSet rs = pst.executeQuery()
        ) {
            if(rs != null) {
                while(rs.next()) {
                    size = rs.getRow();
                }
            }

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return size>0;
    }

    public static String getBoolInSQL(boolean b) {
        return b?"1":"0";
    }

    public static Location stringToLocation(String raw) {
        String[] values = raw.split(",");
        //worlduuid,x,y,z,yaw,pitch
        return new Location(Bukkit.getWorld(UUID.fromString(values[0])),
                Double.parseDouble(values[1]),
                Double.parseDouble(values[2]),
                Double.parseDouble(values[3]),
                Float.parseFloat(values[4]),
                Float.parseFloat(values[5]));
    }
    public static String locationToString(Location loc) {
        return loc.getWorld().getUID().toString()+','+loc.getX()+','+loc.getY()+','+loc.getZ()+','+loc.getYaw()+','+loc.getPitch();
    }
    public static List<Location> stringToLocations(String raw) {
        List<Location> locations = new ArrayList<>();
        for(String location : raw.split(";")) {
            locations.add(stringToLocation(location));
        }
        return locations;
    }
    public static String locationsToString(Location[] locations) {
        StringBuilder raw = new StringBuilder();
        for(Location loc : locations) {
            raw.append(locationToString(loc)).append(';');
        }
        raw.deleteCharAt(raw.length()-1);
        return raw.toString();
    }

    public static Icon stringToIcon(final String raw, OfflinePlayer owner) {
        String[] split = raw.split(",");
        Material material = Material.getMaterial(split[0]);
        return new Icon(material, Integer.parseInt(split[1]), owner, split[2]);
    }
    public static String iconToString(final Icon icon) {
        return icon.getStack().getType() + "," + icon.getStack().getAmount() + "," + icon.getAdditions();
    }
}
