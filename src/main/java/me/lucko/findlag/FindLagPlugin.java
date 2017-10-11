/*
 * This file is part of FindLag, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.findlag;

import me.lucko.findlag.report.BukkitReportListener;
import me.lucko.findlag.report.ReportListener;
import me.lucko.findlag.report.SpigotReportListener;
import me.lucko.findlag.utils.TickCounter;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FindLagPlugin extends JavaPlugin implements Listener {

    private TickCounter tickCounter;
    private final Map<UUID, ReportListener> listeners = new HashMap<>();
    private boolean spigot = false;

    @Override
    public void onEnable() {

        // register global listeners
        getServer().getPluginManager().registerEvents(this, this);

        // setup tick counter
        tickCounter = new TickCounter();
        getServer().getScheduler().runTaskTimer(this, tickCounter, 1L, 1L);

        // register command
        getCommand("findlag").setExecutor(new LagCommand(this));

        // try to detect spigot
        spigot = classExists("org.bukkit.event.entity.SpawnerSpawnEvent");
        if (spigot) {
            getLogger().info("Spigot detected! Enabled tracking for mob spawner spawn events.");
        }
    }

    @Override
    public void onDisable() {
        // terminate any active listeners.
        listeners.keySet().forEach(this::stopListening);
        listeners.clear();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        stopListening(e.getPlayer().getUniqueId());
    }

    public void doAsync(Runnable runnable) {
        getServer().getScheduler().runTaskAsynchronously(this, runnable);
    }

    public ReportListener startListening(UUID uuid) {
        stopListening(uuid);

        ReportListener listener = spigot ? new SpigotReportListener(this) : new BukkitReportListener(this);
        listeners.put(uuid, listener);
        listener.start();

        return listener;
    }

    public ReportListener stopListening(UUID uuid) {
        ReportListener listener = listeners.remove(uuid);
        if (listener != null) {
            listener.stop();
        }
        return listener;
    }

    private static boolean classExists(String clazz) {
        try {
            Class.forName(clazz);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public TickCounter getTickCounter() {
        return tickCounter;
    }
}
