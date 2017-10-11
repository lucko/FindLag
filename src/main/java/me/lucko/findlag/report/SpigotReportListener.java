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

package me.lucko.findlag.report;

import me.lucko.findlag.FindLagPlugin;
import me.lucko.findlag.utils.ChunkPosition;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.SpawnerSpawnEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SpigotReportListener extends BukkitReportListener {

    private final Map<ChunkPosition, AtomicInteger> mobSpawners = new HashMap<>();

    public SpigotReportListener(FindLagPlugin plugin) {
        super(plugin);
    }

    @Override
    public void appendData(StringBuilder report) {
        super.appendData(report);
        appendReport(report, mobSpawners, "Mob Spawners", "This section shows the total number of mob spawner spawns per chunk.", true);
    }

    @EventHandler(priority = EventPriority.LOWEST) // log all attempted spawns, even if they were cancelled.
    public void onMobSpawnerSpawn(SpawnerSpawnEvent e) {
        mobSpawners.computeIfAbsent(ChunkPosition.of(e.getLocation()), ZERO_COUNTER).incrementAndGet();
    }

}
