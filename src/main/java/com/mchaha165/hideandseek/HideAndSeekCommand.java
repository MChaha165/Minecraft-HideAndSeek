/*
 * HideAndSeek - 躲猫猫插件
 * Copyright (c) 2026 MChaha165. All rights reserved.
 *
 * 本代码仅供个人学习、研究使用。
 * 未经作者书面许可，禁止任何形式的商业使用。
 * 项目地址: https://github.com/MChaha165/Minecraft-HideAndSeek/
 */

package com.mchaha165.hideandseek;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class HideAndSeekCommand implements CommandExecutor, TabCompleter {

    private final List<String> mainCommands = Arrays.asList(
            "give", "undisguise", "cleanup", "region", "testblock", "help",
            "join", "leave", "ready", "startgame", "setlobby", "addspawn");
    private final List<String> regionSubCommands = Arrays.asList("wand", "create", "list", "info", "scan", "setblocks", "remove");
    private final LanguageManager lang = HideAndSeek.getInstance().getLanguageManager();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            if (sender instanceof Player) {
                sender.sendMessage(lang.getMessage((Player) sender, "cmd.no_permission"));
            } else {
                sender.sendMessage("You need OP permission to use this command.");
            }
            return true;
        }

        if (args.length < 1 || args[0].equalsIgnoreCase("help")) {
            showHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give":
                handleGive(sender, args);
                break;
            case "undisguise":
                handleUndisguise(sender, args);
                break;
            case "cleanup":
                HideAndSeek.getInstance().cleanupAllDisguises();
                if (sender instanceof Player) {
                    sender.sendMessage(lang.getMessage((Player) sender, "cmd.cleanup.success"));
                } else {
                    sender.sendMessage("All disguise entities cleaned up.");
                }
                break;
            case "region":
                handleRegionCommand(sender, args);
                break;
            case "testblock":
                handleTestBlock(sender, args);
                break;
            case "join":
                handleJoin(sender, args);
                break;
            case "leave":
                handleLeave(sender);
                break;
            case "ready":
                handleReady(sender);
                break;
            case "startgame":
                handleStartGame(sender, args);
                break;
            case "setlobby":
                handleSetLobby(sender);
                break;
            case "addspawn":
                handleAddSpawn(sender, args);
                break;
            default:
                if (sender instanceof Player) {
                    sender.sendMessage(lang.getMessage((Player) sender, "cmd.unknown", args[0]));
                } else {
                    sender.sendMessage("Unknown command. Use /hs help to see available commands.");
                }
        }
        return true;
    }

    private void showHelp(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("========== HideAndSeek Help ==========");
            sender.sendMessage("/hs give <player> - Give disguise item to player");
            sender.sendMessage("/hs undisguise [player] - Remove disguise");
            sender.sendMessage("/hs cleanup - Cleanup all disguise entities");
            sender.sendMessage("/hs testblock <block> - Test disguise with specific block");
            sender.sendMessage("/hs region ... - Region management");
            sender.sendMessage("/hs join <region> - Join waiting queue");
            sender.sendMessage("/hs leave - Leave current game");
            sender.sendMessage("/hs ready - Mark yourself ready");
            sender.sendMessage("/hs startgame <region> - Force start game (OP)");
            sender.sendMessage("/hs setlobby - Set lobby location (OP)");
            sender.sendMessage("/hs addspawn <region> <hider|hunter> - Add spawn point (OP)");
            sender.sendMessage("=======================================");
            return;
        }
        Player player = (Player) sender;
        player.sendMessage(ChatColor.GOLD + lang.getMessage(player, "cmd.help.header"));
        player.sendMessage(ChatColor.YELLOW + "/hs give <player> " + ChatColor.WHITE + "- " + lang.getMessage(player, "cmd.help.give"));
        player.sendMessage(ChatColor.YELLOW + "/hs undisguise [player] " + ChatColor.WHITE + "- " + lang.getMessage(player, "cmd.help.undisguise"));
        player.sendMessage(ChatColor.YELLOW + "/hs cleanup " + ChatColor.WHITE + "- " + lang.getMessage(player, "cmd.help.cleanup"));
        player.sendMessage(ChatColor.YELLOW + "/hs testblock <block> " + ChatColor.WHITE + "- " + lang.getMessage(player, "cmd.help.testblock"));
        player.sendMessage(ChatColor.YELLOW + "/hs region ... " + ChatColor.WHITE + "- " + lang.getMessage(player, "cmd.help.region"));
        player.sendMessage(ChatColor.YELLOW + "/hs join <region> " + ChatColor.WHITE + "- " + lang.getMessage(player, "cmd.help.join"));
        player.sendMessage(ChatColor.YELLOW + "/hs leave " + ChatColor.WHITE + "- " + lang.getMessage(player, "cmd.help.leave"));
        player.sendMessage(ChatColor.YELLOW + "/hs ready " + ChatColor.WHITE + "- " + lang.getMessage(player, "cmd.help.ready"));
        player.sendMessage(ChatColor.YELLOW + "/hs startgame <region> " + ChatColor.WHITE + "- " + lang.getMessage(player, "cmd.help.startgame"));
        player.sendMessage(ChatColor.YELLOW + "/hs setlobby " + ChatColor.WHITE + "- " + lang.getMessage(player, "cmd.help.setlobby"));
        player.sendMessage(ChatColor.YELLOW + "/hs addspawn <region> <hider|hunter> " + ChatColor.WHITE + "- " + lang.getMessage(player, "cmd.help.addspawn"));
        player.sendMessage(ChatColor.GOLD + "=====================================");
        player.sendMessage(ChatColor.DARK_AQUA + "作者: MChaha165 | 版本: " +
                HideAndSeek.getInstance().getDescription().getVersion());
        player.sendMessage(ChatColor.DARK_AQUA + "项目: https://github.com/MChaha165/Minecraft-HideAndSeek/");
        player.sendMessage(ChatColor.DARK_AQUA + lang.getMessage(player, "cmd.help.footer"));
    }

    // ---------- 命令处理 ----------
    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 2) {
            if (sender instanceof Player) {
                sender.sendMessage(lang.getMessage((Player) sender, "cmd.give.specify_player"));
            } else {
                sender.sendMessage("Please specify a player.");
            }
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !target.isOnline()) {
            if (sender instanceof Player) {
                sender.sendMessage(lang.getMessage((Player) sender, "cmd.player_not_found"));
            } else {
                sender.sendMessage("Player not found or offline.");
            }
            return;
        }
        GameManager.giveItem(target);
        if (sender instanceof Player) {
            sender.sendMessage(lang.getMessage((Player) sender, "cmd.give.success", target.getName()));
        } else {
            sender.sendMessage("Gave disguise item to " + target.getName());
        }
    }

    private void handleUndisguise(CommandSender sender, String[] args) {
        Player toUndisguise;
        if (args.length >= 2) {
            toUndisguise = Bukkit.getPlayer(args[1]);
        } else if (sender instanceof Player) {
            toUndisguise = (Player) sender;
        } else {
            sender.sendMessage("Please specify a player.");
            return;
        }
        if (toUndisguise == null || !toUndisguise.isOnline()) {
            if (sender instanceof Player) {
                sender.sendMessage(lang.getMessage((Player) sender, "cmd.player_not_found"));
            } else {
                sender.sendMessage("Player not found or offline.");
            }
            return;
        }
        HideAndSeek.getInstance().undisguisePlayer(toUndisguise);
        if (sender instanceof Player) {
            sender.sendMessage(lang.getMessage((Player) sender, "cmd.undisguise.success", toUndisguise.getName()));
        } else {
            sender.sendMessage("Removed disguise of " + toUndisguise.getName());
        }
    }

    private void handleRegionCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return;
        }
        Player player = (Player) sender;
        RegionManager regionManager = HideAndSeek.getInstance().getRegionManager();

        if (args.length < 2) {
            player.sendMessage(ChatColor.GOLD + lang.getMessage(player, "cmd.region.header"));
            player.sendMessage(ChatColor.YELLOW + "/hs region wand " + ChatColor.WHITE + "- " + lang.getMessage(player, "cmd.region.wand"));
            player.sendMessage(ChatColor.YELLOW + "/hs region create <name> " + ChatColor.WHITE + "- " + lang.getMessage(player, "cmd.region.create"));
            player.sendMessage(ChatColor.YELLOW + "/hs region list " + ChatColor.WHITE + "- " + lang.getMessage(player, "cmd.region.list"));
            player.sendMessage(ChatColor.YELLOW + "/hs region info <name> " + ChatColor.WHITE + "- " + lang.getMessage(player, "cmd.region.info"));
            player.sendMessage(ChatColor.YELLOW + "/hs region scan <name> " + ChatColor.WHITE + "- " + lang.getMessage(player, "cmd.region.scan"));
            player.sendMessage(ChatColor.YELLOW + "/hs region setblocks <name> <blocks> " + ChatColor.WHITE + "- " + lang.getMessage(player, "cmd.region.setblocks"));
            player.sendMessage(ChatColor.YELLOW + "/hs region remove <name> " + ChatColor.WHITE + "- " + lang.getMessage(player, "cmd.region.remove"));
            return;
        }

        switch (args[1].toLowerCase()) {
            case "wand":
                ItemStack wand = new ItemStack(Material.GOLDEN_AXE);
                ItemMeta meta = wand.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(lang.getMessage(player, "wand.name"));
                    wand.setItemMeta(meta);
                }
                player.getInventory().addItem(wand);
                player.sendMessage(lang.getMessage(player, "cmd.region.wand_given"));
                break;

            case "create":
                if (args.length < 3) {
                    player.sendMessage(lang.getMessage(player, "cmd.region.specify_name"));
                    return;
                }
                String createName = args[2];
                if (!regionManager.hasPlayerSelection(player)) {
                    player.sendMessage(lang.getMessage(player, "cmd.region.no_selection"));
                    return;
                }
                Location[] sel = regionManager.getPlayerSelection(player);
                Location pos1 = sel[0];
                Location pos2 = sel[1];
                World world = pos1.getWorld();
                if (!world.equals(pos2.getWorld())) {
                    player.sendMessage(lang.getMessage(player, "cmd.region.different_worlds"));
                    return;
                }
                Region region = new Region(createName, world,
                        pos1.getBlockX(), pos1.getBlockY(), pos1.getBlockZ(),
                        pos2.getBlockX(), pos2.getBlockY(), pos2.getBlockZ());
                regionManager.addRegion(region);
                regionManager.saveRegions();
                regionManager.clearPlayerSelection(player);
                player.sendMessage(lang.getMessage(player, "cmd.region.created", createName));
                break;

            case "list":
                player.sendMessage(lang.getMessage(player, "cmd.region.list_header"));
                for (String name : regionManager.getRegions().keySet()) {
                    player.sendMessage("  - " + name);
                }
                break;

            case "info":
                if (args.length < 3) {
                    player.sendMessage(lang.getMessage(player, "cmd.region.specify_name"));
                    return;
                }
                String infoName = args[2];
                Region infoReg = regionManager.getRegion(infoName);
                if (infoReg == null) {
                    player.sendMessage(lang.getMessage(player, "cmd.region.not_found", infoName));
                    return;
                }
                player.sendMessage(lang.getMessage(player, "cmd.region.info_header", infoName));
                player.sendMessage("  " + lang.getMessage(player, "cmd.region.info_world") + " " + infoReg.getWorld().getName());
                player.sendMessage("  " + lang.getMessage(player, "cmd.region.info_range") + " X=" + infoReg.getMinX() + "~" + infoReg.getMaxX()
                        + ", Y=" + infoReg.getMinY() + "~" + infoReg.getMaxY()
                        + ", Z=" + infoReg.getMinZ() + "~" + infoReg.getMaxZ());
                player.sendMessage("  " + lang.getMessage(player, "cmd.region.info_blocks") + " " + infoReg.getBlocks().size());
                break;

            case "scan":
                if (args.length < 3) {
                    player.sendMessage(lang.getMessage(player, "cmd.region.specify_name"));
                    return;
                }
                String scanName = args[2];
                Region scanReg = regionManager.getRegion(scanName);
                if (scanReg == null) {
                    player.sendMessage(lang.getMessage(player, "cmd.region.not_found", scanName));
                    return;
                }
                player.sendMessage(lang.getMessage(player, "cmd.region.scan_start", scanName));
                Bukkit.getScheduler().runTaskAsynchronously(HideAndSeek.getInstance(), () -> {
                    scanReg.scanBlocks();
                    regionManager.saveRegions();
                    Bukkit.getScheduler().runTask(HideAndSeek.getInstance(), () -> {
                        player.sendMessage(lang.getMessage(player, "cmd.region.scan_complete", scanReg.getBlocks().size()));
                    });
                });
                break;

            case "setblocks":
                if (args.length < 4) {
                    player.sendMessage(lang.getMessage(player, "cmd.region.setblocks_usage"));
                    return;
                }
                String blockRegName = args[2];
                Region blockReg = regionManager.getRegion(blockRegName);
                if (blockReg == null) {
                    player.sendMessage(lang.getMessage(player, "cmd.region.not_found", blockRegName));
                    return;
                }
                String[] blockNames = args[3].split(",");
                List<Material> mats = new ArrayList<>();
                for (String name : blockNames) {
                    Material mat = Material.getMaterial(name.trim().toUpperCase());
                    if (mat != null && mat.isSolid()) {
                        mats.add(mat);
                    } else {
                        player.sendMessage(lang.getMessage(player, "cmd.region.invalid_block", name.trim()));
                    }
                }
                if (mats.isEmpty()) {
                    player.sendMessage(lang.getMessage(player, "cmd.region.no_valid_blocks"));
                    return;
                }
                blockReg.getBlocks().clear();
                blockReg.getBlocks().addAll(mats);
                regionManager.saveRegions();
                player.sendMessage(lang.getMessage(player, "cmd.region.setblocks_success", blockRegName, mats.size()));
                break;

            case "remove":
                if (args.length < 3) {
                    player.sendMessage(lang.getMessage(player, "cmd.region.specify_name"));
                    return;
                }
                String removeName = args[2];
                if (regionManager.getRegion(removeName) == null) {
                    player.sendMessage(lang.getMessage(player, "cmd.region.not_found", removeName));
                    return;
                }
                regionManager.removeRegion(removeName);
                regionManager.saveRegions();
                player.sendMessage(lang.getMessage(player, "cmd.region.removed", removeName));
                break;

            default:
                player.sendMessage(lang.getMessage(player, "cmd.region.unknown"));
        }
    }

    private void handleTestBlock(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return;
        }
        Player player = (Player) sender;
        if (args.length < 2) {
            player.sendMessage(lang.getMessage(player, "cmd.testblock.usage"));
            return;
        }
        String blockName = args[1].toUpperCase();
        Material material = Material.getMaterial(blockName);
        if (material == null || !material.isBlock()) {
            player.sendMessage(lang.getMessage(player, "cmd.testblock.invalid"));
            return;
        }
        CombinedDisguise disguise = HideAndSeek.getInstance().getDisguise();
        if (disguise == null) {
            player.sendMessage(lang.getMessage(player, "cmd.testblock.error"));
            return;
        }
        disguise.testDisguise(player, material);
    }

    private void handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return;
        }
        Player player = (Player) sender;
        if (args.length < 2) {
            player.sendMessage(lang.getMessage(player, "cmd.join.usage"));
            return;
        }
        String regionName = args[1];
        GameManager.getInstance().joinGame(player, regionName);
    }

    private void handleLeave(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return;
        }
        Player player = (Player) sender;
        GameManager.getInstance().leaveGame(player);
        player.sendMessage(lang.getMessage(player, "cmd.leave.success"));
    }

    private void handleReady(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return;
        }
        Player player = (Player) sender;
        GameInstance game = GameManager.getInstance().getGameByPlayer(player);
        if (game == null) {
            player.sendMessage(lang.getMessage(player, "cmd.ready.not_in_queue"));
            return;
        }
        if (game.getState() != GameInstance.GameState.WAITING) {
            player.sendMessage(lang.getMessage(player, "cmd.ready.not_waiting"));
            return;
        }
        game.readyPlayer(player);
    }

    private void handleStartGame(CommandSender sender, String[] args) {
        if (args.length < 2) {
            if (sender instanceof Player) {
                sender.sendMessage(lang.getMessage((Player) sender, "cmd.startgame.usage"));
            } else {
                sender.sendMessage("Usage: /hs startgame <region>");
            }
            return;
        }
        String regionName = args[1];
        Region region = HideAndSeek.getInstance().getRegionManager().getRegion(regionName);
        if (region == null) {
            if (sender instanceof Player) {
                sender.sendMessage(lang.getMessage((Player) sender, "cmd.region.not_found", regionName));
            } else {
                sender.sendMessage("Region " + regionName + " not found.");
            }
            return;
        }
        GameInstance game = GameManager.getInstance().getGameByRegion(regionName);
        if (game == null) {
            game = new GameInstance(region, GameManager.getInstance().getDefaultGameTime());
            GameManager.getInstance().startGame(regionName);
            game = GameManager.getInstance().getGameByRegion(regionName);
            if (sender instanceof Player) {
                sender.sendMessage(lang.getMessage((Player) sender, "cmd.startgame.created", regionName));
            } else {
                sender.sendMessage("Created new game for region " + regionName);
            }
        }
        game.startGameNow();
        if (sender instanceof Player) {
            sender.sendMessage(lang.getMessage((Player) sender, "cmd.startgame.success"));
        } else {
            sender.sendMessage("Game started.");
        }
    }

    private void handleSetLobby(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return;
        }
        Player player = (Player) sender;
        GameManager.getInstance().setLobbyLocation(player.getLocation());
        player.sendMessage(lang.getMessage(player, "cmd.setlobby.success"));
    }

    private void handleAddSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(lang.getMessage((Player) sender, "cmd.addspawn.usage"));
            return;
        }
        String regionName = args[1];
        String type = args[2].toLowerCase();
        if (!type.equals("hider") && !type.equals("hunter")) {
            sender.sendMessage(lang.getMessage((Player) sender, "cmd.addspawn.invalid_type"));
            return;
        }
        Player player = (Player) sender;
        if (type.equals("hider")) {
            GameManager.getInstance().addHiderSpawn(regionName, player.getLocation());
        } else {
            GameManager.getInstance().addHunterSpawn(regionName, player.getLocation());
        }
        player.sendMessage(lang.getMessage(player, "cmd.addspawn.success", type, regionName));
    }

    // ---------- Tab 补全 ----------
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.isOp()) return null;
        if (args.length == 1) {
            return mainCommands.stream()
                    .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("region")) {
                return regionSubCommands.stream()
                        .filter(cmd -> cmd.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("undisguise")) {
                String partial = args[1].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(partial))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("join") || args[0].equalsIgnoreCase("startgame") || args[0].equalsIgnoreCase("addspawn")) {
                RegionManager regionManager = HideAndSeek.getInstance().getRegionManager();
                return regionManager.getRegions().keySet().stream()
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("testblock")) {
                return new ArrayList<>();
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("region") &&
                (args[1].equalsIgnoreCase("info") || args[1].equalsIgnoreCase("scan") ||
                        args[1].equalsIgnoreCase("setblocks") || args[1].equalsIgnoreCase("remove"))) {
            RegionManager regionManager = HideAndSeek.getInstance().getRegionManager();
            return regionManager.getRegions().keySet().stream()
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}