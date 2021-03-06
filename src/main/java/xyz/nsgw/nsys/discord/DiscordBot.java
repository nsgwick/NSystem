/*
 * Copyright (c) Nicholas Williams 2021.
 */

package xyz.nsgw.nsys.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.configuration.ConfigurationSection;
import xyz.nsgw.nsys.NSys;

import javax.security.auth.login.LoginException;

public class DiscordBot {

    private JDA jda;

    public DiscordBot(NSys pl) {

        try {
            ConfigurationSection config = pl.getConfig().getConfigurationSection("discord");
            assert config != null;

            DiscordUtils.setJda(

                    JDABuilder.create(config.getString("token"), GatewayIntent.GUILD_MEMBERS)
                    .setActivity(Activity.playing("on Cosmicity."))
                    .addEventListeners(new DiscordListeners(pl))
                    .build()

            );

            DiscordUtils.setMod_role(config.getString("mod-role-id"));

            DiscordUtils.setDm_overflow_channel(config.getString("dm-overflow-channel-id"));

        }

        catch (LoginException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
