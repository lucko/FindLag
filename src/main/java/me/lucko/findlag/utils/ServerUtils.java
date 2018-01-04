package me.lucko.findlag.utils;

import org.bukkit.Bukkit;
import org.bukkit.Server;

import java.lang.reflect.Field;

public class ServerUtils {

    public static void setMaxPlayers(int maxPlayers) throws Exception {
        Server server = Bukkit.getServer();

        Class<? extends Server> serverClass = server.getClass();

        Field playerListField = serverClass.getDeclaredField("playerList");
        playerListField.setAccessible(true);

        Object playerList = playerListField.get(server);

        Field maxPlayersField = playerList.getClass().getSuperclass().getDeclaredField("maxPlayers");
        maxPlayersField.setAccessible(true);

        maxPlayersField.setInt(playerList, maxPlayers);
    }

}
