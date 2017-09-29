package me.lucko.lagfind;

import me.lucko.helper.Commands;
import me.lucko.helper.Events;
import me.lucko.helper.plugin.ExtendedJavaPlugin;
import me.lucko.helper.plugin.ap.Plugin;
import me.lucko.helper.plugin.ap.PluginDependency;
import me.lucko.helper.serialize.ChunkPosition;
import me.lucko.helper.utils.Players;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Plugin(name = "LagFind", description = "Find sources of lag", authors = "Luck", depends = @PluginDependency("helper"))
public class LagFindPlugin extends ExtendedJavaPlugin {
    private static final UUID CONSOLE_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final Map<UUID, LagListener> listeners = new HashMap<>();

    @Override
    public void enable() {
        bindRunnable(() -> {
            listeners.keySet().forEach(this::stopListening);
            listeners.clear();
        });

        Events.subscribe(PlayerQuitEvent.class)
                .handler(e -> stopListening(e.getPlayer().getUniqueId()))
                .bindWith(this);

        Commands.create()
                .assertPermission("lagfind.use")
                .handler(c -> {
                    UUID uuid = c.sender() instanceof Player ? ((Player) c.sender()).getUniqueId() : CONSOLE_UUID;

                    LagListener listener = stopListening(uuid);
                    if (listener != null) {
                        listener.sendReport(c.sender());
                        return;
                    }

                    startListening(uuid);
                    Players.msg(c.sender(), "&aStarted listening. Run /lagfind again to stop.");
                })
                .register(this, "lagfind");

        Commands.create()
                .assertPermission("lagfind.tp")
                .assertPlayer()
                .handler(c -> {
                    int x = c.arg(0).parseOrFail(Integer.class);
                    int z = c.arg(1).parseOrFail(Integer.class);
                    String world = c.arg(2).parseOrFail(String.class);

                    ChunkPosition pos = ChunkPosition.of(x, z, world);
                    c.sender().teleport(pos.getBlock(0, 100, 0).toLocation());
                    c.sender().sendMessage("You were teleported to " + pos.toString());
                })
                .register(this, "lagtp");
    }

    public LagListener startListening(UUID uuid) {
        stopListening(uuid);
        LagListener listener = new LagListener();
        listeners.put(uuid, listener);
        getServer().getPluginManager().registerEvents(listener, this);
        return listener;
    }

    public LagListener stopListening(UUID uuid) {
        LagListener listener = listeners.remove(uuid);
        if (listener != null) {
            HandlerList.unregisterAll(listener);
        }
        return listener;
    }

}
