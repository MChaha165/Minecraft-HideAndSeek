/*
 * HideAndSeek - 躲猫猫插件
 * Copyright (c) 2026 MChaha165. All rights reserved.
 *
 * 本代码仅供个人学习、研究使用。
 * 未经作者书面许可，禁止任何形式的商业使用。
 * 项目地址: https://github.com/MChaha165/Minecraft-HideAndSeek/
 */

package com.mchaha165.hideandseek;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

public class HideAndSeek extends JavaPlugin {

    private static HideAndSeek instance;
    private CombinedDisguise disguise;
    private RegionManager regionManager;
    private GameManager gameManager;
    private LanguageManager languageManager;
    private BukkitAudiences adventure;

    @Override
    public void onEnable() {
        try {
            instance = this;

            // 先保存默认配置（如果不存在）
            saveDefaultConfig();
            // 尝试重新加载配置，如果失败则忽略，后续使用默认值
            try {
                reloadConfig();
            } catch (Exception e) {
                getLogger().warning("加载配置文件时出现错误，部分配置可能无效，请检查 config.yml 中的世界名是否正确。");
                e.printStackTrace();
            }

            getLogger().info("开始初始化语言管理器...");
            languageManager = new LanguageManager(this);

            getLogger().info("开始初始化区域管理器...");
            regionManager = new RegionManager(this);

            getLogger().info("开始初始化游戏管理器...");
            gameManager = new GameManager(this);

            getLogger().info("开始初始化伪装系统...");
            disguise = new CombinedDisguise();

            // 初始化 Adventure
            this.adventure = BukkitAudiences.create(this);

            getServer().getPluginManager().registerEvents(disguise, this);
            getServer().getPluginManager().registerEvents(new SelectionListener(), this);
            getServer().getPluginManager().registerEvents(new GameListener(), this);

            this.getCommand("hs").setExecutor(new HideAndSeekCommand());
            this.getCommand("hs").setTabCompleter(new HideAndSeekCommand());

            // 输出作者信息
            getLogger().info("=========================================");
            getLogger().info("  躲猫猫插件已启动！");
            getLogger().info("  作者: MChaha165");
            getLogger().info("  版本: " + getDescription().getVersion());
            getLogger().info("  项目: https://github.com/MChaha165/Minecraft-HideAndSeek/");
            getLogger().info("  个人服务器免费使用，商业用途需授权");
            getLogger().info("=========================================");

        } catch (Throwable t) {
            t.printStackTrace();
            getLogger().severe("插件启动失败: " + t.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("躲猫猫插件已关闭，正在清理所有游戏和伪装...");
        if (gameManager != null) {
            gameManager.disableAllGames();
        }
        if (disguise != null) {
            disguise.cleanupDisguises();
        }
        if (adventure != null) {
            adventure.close();
        }
    }

    public static HideAndSeek getInstance() {
        return instance;
    }

    public RegionManager getRegionManager() {
        return regionManager;
    }

    public CombinedDisguise getDisguise() {
        return disguise;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public BukkitAudiences adventure() {
        return adventure;
    }

    public void undisguisePlayer(Player player) {
        if (disguise != null) {
            disguise.removeDisguise(player);
        }
    }

    public void cleanupAllDisguises() {
        if (disguise != null) {
            disguise.cleanupDisguises();
        }
    }

    // 内部监听器，处理金斧选区
    private class SelectionListener implements Listener {
        @EventHandler
        public void onPlayerInteract(PlayerInteractEvent event) {
            Player player = event.getPlayer();
            ItemStack item = event.getItem();
            if (item == null || item.getType() != Material.GOLDEN_AXE) return;
            if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName() ||
                    !item.getItemMeta().getDisplayName().contains("躲猫猫选区工具")) return;

            if (event.getClickedBlock() == null) return;

            Action action = event.getAction();
            Location loc = event.getClickedBlock().getLocation();
            RegionManager regionManager = HideAndSeek.getInstance().getRegionManager();

            if (action == Action.LEFT_CLICK_BLOCK) {
                regionManager.setPlayerPos1(player, loc);
                event.setCancelled(true);
            } else if (action == Action.RIGHT_CLICK_BLOCK) {
                regionManager.setPlayerPos2(player, loc);
                event.setCancelled(true);
            }
        }
    }

    // 游戏事件监听器
    private class GameListener implements Listener {
        private final LanguageManager lang = HideAndSeek.getInstance().getLanguageManager();

        @EventHandler
        public void onPlayerDeath(PlayerDeathEvent event) {
            Player player = event.getEntity();
            gameManager.onPlayerDeath(player);
        }

        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent event) {
            Player player = event.getPlayer();
            gameManager.onPlayerQuit(player);
        }

        @EventHandler
        public void onPlayerMove(PlayerMoveEvent event) {
            Player player = event.getPlayer();
            GameInstance game = GameManager.getInstance().getGameByPlayer(player);
            if (game == null) return;
            if (game.getState() == GameInstance.GameState.ACTIVE &&
                    !game.isHunterReleased(player) &&
                    event.getTo() != null && (event.getTo().getX() != event.getFrom().getX() ||
                    event.getTo().getY() != event.getFrom().getY() ||
                    event.getTo().getZ() != event.getFrom().getZ())) {
                event.setCancelled(true);
                player.sendMessage(lang.getMessage(player, "game.active.hunter_cannot_move"));
            }
        }

        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            Player player = event.getPlayer();
            gameManager.reconnectPlayer(player);
        }

        @EventHandler
        public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
            Player player = event.getPlayer();
            GameInstance game = GameManager.getInstance().getGameByPlayer(player);
            if (game == null) return;
            if (game.getState() != GameInstance.GameState.ACTIVE) return;

            String command = event.getMessage().toLowerCase();
            List<String> blockedCommands = Arrays.asList(
                    "/home", "/back", "/tpa", "/tpahere", "/warp", "/spawn",
                    "/sethome", "/delhome", "/tpa", "/tpaccept", "/tpdeny"
            );
            for (String cmd : blockedCommands) {
                if (command.startsWith(cmd)) {
                    event.setCancelled(true);
                    player.sendMessage(lang.getMessage(player, "game.active.cannot_tp"));
                    return;
                }
            }
        }
    }
}