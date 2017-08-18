package me.lucko.lagfind;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import me.lucko.helper.Scheduler;
import me.lucko.helper.serialize.BlockPosition;
import me.lucko.helper.serialize.ChunkPosition;
import me.lucko.helper.utils.Players;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LagListener implements Listener {
    private static final Function<ChunkPosition, AtomicInteger> NEW_COUNTER = o -> new AtomicInteger(0);

    private final long startSeconds = System.currentTimeMillis() / 1000L;

    private final Map<ChunkPosition, AtomicInteger> redstone = new HashMap<>();
    private final Map<ChunkPosition, AtomicInteger> hoppers = new HashMap<>();
    private final Map<ChunkPosition, AtomicInteger> pistons = new HashMap<>();
    private final Map<ChunkPosition, AtomicInteger> physics = new HashMap<>();

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

    public void sendReport(CommandSender sender) {
        Scheduler.runAsync(() -> {
            Players.msg(sender, "&aBuilding report. Please wait...");

            StringBuilder report = new StringBuilder();
            long time = (System.currentTimeMillis() / 1000L) - startSeconds;
            report.append("Time: ").append(time).append(" seconds");
            report.append("\n");

            appendReport(report, redstone, "Redstone");
            appendReport(report, hoppers, "Hoppers");
            appendReport(report, pistons, "Pistons");
            appendReport(report, physics, "Physics");

            String url = PasteUtils.paste("LagFind results", ImmutableList.of(Maps.immutableEntry("data.txt", report.toString())));
            Players.msg(sender, "&aLagFind results url: " + url);

            redstone.clear();
            hoppers.clear();
            pistons.clear();
            physics.clear();
        });
    }

    private static void appendReport(StringBuilder report, Map<ChunkPosition, AtomicInteger> data, String name) {
        report.append("================================================\n");
        report.append("==> ").append(name).append("\nTop Chunks:\n\n");

        List<Map.Entry<ChunkPosition, Integer>> sorted = data.entrySet().stream()
                .filter(e -> e.getValue().get() > 0)
                .sorted((o1, o2) -> {
                    int i = Integer.compare(o2.getValue().get(), o1.getValue().get());
                    return i == 0 ? 1 : i;
                })
                .map(e -> Maps.immutableEntry(e.getKey(), e.getValue().get()))
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
    }

}
