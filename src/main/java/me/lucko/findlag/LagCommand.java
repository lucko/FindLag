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

import me.lucko.findlag.report.ReportListener;
import me.lucko.findlag.utils.ServerUtils;
import me.lucko.findlag.utils.TextUtils;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class LagCommand implements CommandExecutor {

    private static final String PREFIX = TextUtils.color("&8&l[&eFindLag&8&l] &7");
    private static final UUID CONSOLE_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final FindLagPlugin plugin;

    public LagCommand(FindLagPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command c, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start":
            case "record":
                handleStart(sender, label);
                break;
            case "cancel":
                handleCancel(sender, label);
                break;
            case "report":
            case "paste":
                handleReport(sender, label);
                break;
            case "tpchunk":
            case "tp":
                handleTp(sender, label, args);
                break;
            case "setmaxplayers":
                handleMaxPlayers(sender, label, args);
                break;
            default:
                sendUsage(sender, label);
                break;
        }
        return true;
    }

    private void handleStart(CommandSender sender, String label) {
        UUID uuid = sender instanceof Player ? ((Player) sender).getUniqueId() : CONSOLE_UUID;

        plugin.startListening(uuid);
        msgPrefix(sender, "&eStarted gathering data. Run '/" + label + " report' to stop recording and view the results.");
    }

    private void handleCancel(CommandSender sender, String label) {
        UUID uuid = sender instanceof Player ? ((Player) sender).getUniqueId() : CONSOLE_UUID;

        ReportListener listener = plugin.stopListening(uuid);
        if (listener != null) {
            msgPrefix(sender, "&eYour existing report has been cancelled.");
        } else {
            msgPrefix(sender, "&eUnable to find any active reports for your user. Run '/" + label + " start' to start gathering data.");
        }
    }

    private void handleReport(CommandSender sender, String label) {
        UUID uuid = sender instanceof Player ? ((Player) sender).getUniqueId() : CONSOLE_UUID;

        ReportListener listener = plugin.stopListening(uuid);
        if (listener != null) {
            listener.sendReport(sender);
        } else {
            msgPrefix(sender, "&eUnable to find any active reports for your user. Run '/" + label + " start' to start gathering data.");
        }
    }

    private void handleTp(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            msgPrefix(sender, "&eYou must be a player to use this command!");
            return;
        }

        // tp x z world

        if (args.length < 4) {
            sendUsage(sender, label);
            return;
        }

        int x, z;
        World world;

        try {
            x = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            msgPrefix(sender, "&eExpected an integer for <x>, but got '" + args[1] + "' instead.");
            return;
        }

        try {
            z = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            msgPrefix(sender, "&eExpected an integer for <z>, but got '" + args[1] + "' instead.");
            return;
        }

        world = plugin.getServer().getWorld(args[3]);
        if (world == null) {
            msgPrefix(sender, "&eWorld '" + args[3] + "' does not exist.");
            return;
        }

        ((Player) sender).teleport(world.getHighestBlockAt(world.getChunkAt(x, z).getBlock(8, 0, 8).getLocation()).getLocation());
        msgPrefix(sender, "&eTeleported you to chunk x=" + x + ", z=" + z + ", world=" + world.getName());
    }

    private void handleMaxPlayers(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sendUsage(sender, label);
            return;
        }

        int amount;

        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            msgPrefix(sender, "&eExpected an integer for <amount>, but got '" + args[1] + "' instead.");
            return;
        }

        try {
            ServerUtils.setMaxPlayers(amount);
            msgPrefix(sender, "&eSet max players to " + amount);
        } catch (Exception e) {
            msgPrefix(sender, "&eUnable to set max players - " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    private void sendUsage(CommandSender sender, String label) {
        msgPrefix(sender, "Running &eFindLag v" + plugin.getDescription().getVersion() + "&7.");
        if (sender.hasPermission("findlag.use")) {
            msg(sender, "&8> &7/" + label + " start");
            msg(sender, "&8> &7/" + label + " cancel");
            msg(sender, "&8> &7/" + label + " report");
        }
        if (sender.hasPermission("findlag.tpchunk")) {
            msg(sender, "&8> &7/" + label + " tpchunk <x> <z> <world>");
        }
        if (sender.hasPermission("findlag.setmaxplayers")) {
            msg(sender, "&8> &7/" + label + " setmaxplayers <amount>");
        }
    }

    public static void msg(CommandSender sender, String msg) {
        sender.sendMessage(TextUtils.color(msg));
    }

    public static void msgPrefix(CommandSender sender, String msg) {
        sender.sendMessage(PREFIX  + TextUtils.color(msg));
    }
}
