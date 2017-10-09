package me.lucko.lagfind;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import me.lucko.helper.Scheduler;
import me.lucko.helper.serialize.BlockPosition;
import me.lucko.helper.serialize.ChunkPosition;
import me.lucko.helper.utils.Players;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.vehicle.VehicleMoveEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LagListener implements Listener {
    private static final Function<ChunkPosition, AtomicInteger> NEW_COUNTER = o -> new AtomicInteger(0);
    private static final Function<EntityType, AtomicInteger> NEW_ENTITY_COUNTER = o -> new AtomicInteger(0);

    private final long startSeconds = System.currentTimeMillis() / 1000L;

    private final Map<ChunkPosition, AtomicInteger> redstone = new HashMap<>();
    private final Map<ChunkPosition, AtomicInteger> hoppers = new HashMap<>();
    private final Map<ChunkPosition, AtomicInteger> pistons = new HashMap<>();
    private final Map<ChunkPosition, AtomicInteger> physics = new HashMap<>();
    private final Map<ChunkPosition, AtomicInteger> mobSpawners = new HashMap<>();
    private final Map<ChunkPosition, AtomicInteger> spawns = new HashMap<>();
    private final Map<ChunkPosition, AtomicInteger> vehicles = new HashMap<>();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRedstone(BlockRedstoneEvent e) {
        redstone.computeIfAbsent(BlockPosition.of(e.getBlock()).toChunk(), NEW_COUNTER).incrementAndGet();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHopper(InventoryMoveItemEvent e) {
        if (e.getInitiator().getType() == InventoryType.HOPPER) {
            Location location = e.getInitiator().getLocation();
            if (location != null) {
                hoppers.computeIfAbsent(BlockPosition.of(location).toChunk(), NEW_COUNTER).incrementAndGet();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        pistons.computeIfAbsent(BlockPosition.of(e.getBlock()).toChunk(), NEW_COUNTER).incrementAndGet();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        pistons.computeIfAbsent(BlockPosition.of(e.getBlock()).toChunk(), NEW_COUNTER).incrementAndGet();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPhysics(BlockPhysicsEvent e) {
        physics.computeIfAbsent(BlockPosition.of(e.getBlock()).toChunk(), NEW_COUNTER).incrementAndGet();
    }

    @EventHandler(priority = EventPriority.LOWEST) // log attempted spawns (accounts for stacking)
    public void onMobSpawner(SpawnerSpawnEvent e) {
        mobSpawners.computeIfAbsent(BlockPosition.of(e.getLocation()).toChunk(), NEW_COUNTER).incrementAndGet();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawn(EntitySpawnEvent e) {
        spawns.computeIfAbsent(BlockPosition.of(e.getLocation()).toChunk(), NEW_COUNTER).incrementAndGet();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicle(VehicleMoveEvent e) {
        // ignore if the vehicle hasn't moved over a block boundary
        if (e.getFrom().getBlockX() == e.getTo().getBlockX() && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) {
            return;
        }

        vehicles.computeIfAbsent(BlockPosition.of(e.getTo()).toChunk(), NEW_COUNTER).incrementAndGet();
    }

    private List<Map<ChunkPosition, AtomicInteger>> gatherChunkEntityStats() {
        Map<ChunkPosition, AtomicInteger> entities = new HashMap<>();
        Map<ChunkPosition, AtomicInteger> items = new HashMap<>();
        Map<ChunkPosition, AtomicInteger> tiles = new HashMap<>();

        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                ChunkPosition chunkPosition = ChunkPosition.of(chunk);

                Entity[] ents = chunk.getEntities();
                entities.computeIfAbsent(chunkPosition, NEW_COUNTER).addAndGet(ents.length);

                for (Entity entity : ents) {
                    if (entity.getType() == EntityType.DROPPED_ITEM) {
                        items.computeIfAbsent(chunkPosition, NEW_COUNTER).incrementAndGet();
                    }
                }

                tiles.computeIfAbsent(chunkPosition, NEW_COUNTER).addAndGet(chunk.getTileEntities().length);
            }
        }
        
        return ImmutableList.of(entities, items, tiles);
    }

    private Map<EntityType, AtomicInteger> gatherEntityStats() {
        Map<EntityType, AtomicInteger> entities = new HashMap<>();

        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                entities.computeIfAbsent(entity.getType(), NEW_ENTITY_COUNTER).incrementAndGet();
            }
        }

        return entities;
    }

    public void sendReport(CommandSender sender) {

        Map<EntityType, AtomicInteger> entityStats = gatherEntityStats();
        List<Map<ChunkPosition, AtomicInteger>> chunkEntityStats = gatherChunkEntityStats();

        long time = (System.currentTimeMillis() / 1000L) - startSeconds;

        Scheduler.runAsync(() -> {
            Players.msg(sender, "&aBuilding report. Please wait...");

            StringBuilder report = new StringBuilder();
            report.append("Time: ").append(time).append(" seconds");
            report.append("\n");

            appendReport(report, redstone, "Redstone");
            appendReport(report, hoppers, "Hoppers");
            appendReport(report, pistons, "Pistons");
            appendReport(report, physics, "Physics");
            appendReport(report, mobSpawners, "Mob Spawners");
            appendReport(report, spawns, "Entity Spawns");

            appendReport(report, chunkEntityStats.get(0), "Total Entities");
            appendReport(report, chunkEntityStats.get(1), "Total Items");
            appendReport(report, chunkEntityStats.get(2), "Total Tile Entities");

            appendEnumReport(report, entityStats, "Entity Distribution");

            String url = PasteUtils.paste("LagFind results", ImmutableList.of(Maps.immutableEntry("data.txt", report.toString())));
            Players.msg(sender, "&aLagFind results url: " + url);
        });
    }

    private static void appendReport(StringBuilder report, Map<ChunkPosition, AtomicInteger> data, String name) {
        report.append("================================================\n");
        report.append("==> ").append(name).append("\nTop Chunks:\n\n");

        List<Map.Entry<ChunkPosition, Integer>> sorted = data.entrySet().stream()
                .filter(e -> e.getValue().get() > 0)
                .map(e -> Maps.immutableEntry(e.getKey(), e.getValue().get()))
                .sorted(Collections.reverseOrder((o1, o2) -> {
                    if (o1.equals(o2)) {
                        return 0;
                    }

                    int i = Integer.compare(o1.getValue(), o2.getValue());
                    if (i != 0) {
                        return i;
                    }

                    ChunkPosition chunk1 = o1.getKey();
                    ChunkPosition chunk2 = o2.getKey();

                    i = Integer.compare(chunk1.getX(), chunk2.getX());
                    if (i != 0) {
                        return i;
                    }

                    i = Integer.compare(chunk1.getZ(), chunk2.getZ());
                    if (i != 0) {
                        return i;
                    }

                    return chunk1.getWorld().compareTo(chunk2.getWorld());
                }))
                .limit(25)
                .collect(Collectors.toList());

        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<ChunkPosition, Integer> e = sorted.get(i);
            report.append("#").append(i + 1).append(" ")
                    .append(e.getKey().getX()).append(" ")
                    .append(e.getKey().getZ()).append(" ")
                    .append(e.getKey().getWorld()).append(" - ")
                    .append(e.getValue())
                    .append("\n");
        }

        report.append("\n\n");
        data.clear();
    }

    private static <T extends Enum<T>> void appendEnumReport(StringBuilder report, Map<T, AtomicInteger> data, String name) {
        report.append("================================================\n");
        report.append("==> ").append(name).append("\nTop Types:\n\n");

        List<Map.Entry<T, Integer>> sorted = data.entrySet().stream()
                .filter(e -> e.getValue().get() > 0)
                .map(e -> Maps.immutableEntry(e.getKey(), e.getValue().get()))
                .sorted(Collections.reverseOrder((o1, o2) -> {
                    if (o1.equals(o2)) {
                        return 0;
                    }

                    int i = Integer.compare(o1.getValue(), o2.getValue());
                    if (i != 0) {
                        return i;
                    }

                    T et1 = o1.getKey();
                    T et2 = o2.getKey();

                    return et1.compareTo(et2);
                }))
                .limit(25)
                .collect(Collectors.toList());

        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<T, Integer> e = sorted.get(i);
            report.append("#").append(i + 1).append(" ")
                    .append(e.getKey().name()).append(" - ")
                    .append(e.getValue())
                    .append("\n");
        }

        report.append("\n\n");
        data.clear();
    }

}
