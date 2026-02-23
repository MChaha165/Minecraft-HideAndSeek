/*
 * HideAndSeek - 躲猫猫插件
 * Copyright (c) 2026 MChaha165. All rights reserved.
 *
 * 本代码仅供个人学习、研究使用。
 * 未经作者书面许可，禁止任何形式的商业使用。
 * 项目地址: https://github.com/MChaha165/Minecraft-HideAndSeek/
 */

package com.mchaha165.hideandseek;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RegionManager {
    private final HideAndSeek plugin;
    private final Map<String, Region> regions = new HashMap<>();
    private final Map<UUID, Location[]> playerSelections = new HashMap<>(); // 每个玩家的临时选区

    public RegionManager(HideAndSeek plugin) {
        this.plugin = plugin;
        loadRegions();
    }

    public void loadRegions() {
        regions.clear();
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection regSection = config.getConfigurationSection("regions");
        if (regSection != null) {
            for (String key : regSection.getKeys(false)) {
                ConfigurationSection section = regSection.getConfigurationSection(key);
                if (section != null) {
                    Region region = new Region(section);
                    regions.put(key, region);
                }
            }
        }
        plugin.getLogger().info("已加载 " + regions.size() + " 个区域");
    }

    public void saveRegions() {
        FileConfiguration config = plugin.getConfig();
        config.set("regions", null);
        ConfigurationSection regSection = config.createSection("regions");
        for (Map.Entry<String, Region> entry : regions.entrySet()) {
            ConfigurationSection section = regSection.createSection(entry.getKey());
            entry.getValue().saveToConfig(section);
        }
        plugin.saveConfig();
    }

    public Region getRegion(String name) {
        return regions.get(name);
    }

    public Region getRegionAt(Location loc) {
        for (Region region : regions.values()) {
            if (region.contains(loc)) {
                return region;
            }
        }
        return null;
    }

    public void addRegion(Region region) {
        regions.put(region.getName(), region);
    }

    public void removeRegion(String name) {
        regions.remove(name);
    }

    public Map<String, Region> getRegions() {
        return regions;
    }

    // 玩家选区操作
    public void setPlayerPos1(Player player, Location loc) {
        UUID uuid = player.getUniqueId();
        Location[] sel = playerSelections.computeIfAbsent(uuid, k -> new Location[2]);
        sel[0] = loc;
        player.sendMessage("§a第一个点已设置: " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
    }

    public void setPlayerPos2(Player player, Location loc) {
        UUID uuid = player.getUniqueId();
        Location[] sel = playerSelections.computeIfAbsent(uuid, k -> new Location[2]);
        sel[1] = loc;
        player.sendMessage("§a第二个点已设置: " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
    }

    public Location[] getPlayerSelection(Player player) {
        return playerSelections.get(player.getUniqueId());
    }

    public boolean hasPlayerSelection(Player player) {
        Location[] sel = playerSelections.get(player.getUniqueId());
        return sel != null && sel[0] != null && sel[1] != null && sel[0].getWorld().equals(sel[1].getWorld());
    }

    public void clearPlayerSelection(Player player) {
        playerSelections.remove(player.getUniqueId());
    }
}