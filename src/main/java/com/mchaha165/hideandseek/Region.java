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
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class Region {
    private String name;
    private World world;
    private int minX, maxX, minY, maxY, minZ, maxZ;
    private List<Material> blocks; // 伪装候选方块列表

    public Region(String name, World world, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.name = name;
        this.world = world;
        setBounds(x1, y1, z1, x2, y2, z2);
        this.blocks = new ArrayList<>();
    }

    // 从配置文件加载
    public Region(ConfigurationSection section) {
        this.name = section.getName();
        this.world = org.bukkit.Bukkit.getWorld(section.getString("world"));
        this.minX = section.getInt("minX");
        this.maxX = section.getInt("maxX");
        this.minY = section.getInt("minY");
        this.maxY = section.getInt("maxY");
        this.minZ = section.getInt("minZ");
        this.maxZ = section.getInt("maxZ");
        this.blocks = new ArrayList<>();
        List<String> blockNames = section.getStringList("blocks");
        for (String name : blockNames) {
            Material mat = Material.getMaterial(name);
            if (mat != null) blocks.add(mat);
        }
    }

    public void setBounds(int x1, int y1, int z1, int x2, int y2, int z2) {
        minX = Math.min(x1, x2);
        maxX = Math.max(x1, x2);
        minY = Math.min(y1, y2);
        maxY = Math.max(y1, y2);
        minZ = Math.min(z1, z2);
        maxZ = Math.max(z1, z2);
    }

    public boolean contains(Location loc) {
        if (!loc.getWorld().equals(world)) return false;
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public Material getRandomBlock(Random random) {
        if (blocks.isEmpty()) return Material.STONE; // 默认
        return blocks.get(random.nextInt(blocks.size()));
    }

    // 扫描区域内所有方块，收集非空气、适合伪装的方块
    public void scanBlocks() {
        Set<Material> blockSet = new HashSet<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    Material type = block.getType();
                    if (type != Material.AIR && type.isSolid() && !isExcluded(type)) {
                        blockSet.add(type);
                    }
                }
            }
        }
        blocks.clear();
        blocks.addAll(blockSet);
    }

    private boolean isExcluded(Material type) {
        // 排除液体、基岩、命令方块等基础排除项
        if (type == Material.WATER || type == Material.LAVA
                || type == Material.BEDROCK || type == Material.COMMAND_BLOCK
                || type == Material.STRUCTURE_BLOCK || type == Material.BARRIER
                || type == Material.STRUCTURE_VOID || type == Material.FIRE
                || type == Material.SOUL_FIRE || type == Material.CAMPFIRE
                || type == Material.SOUL_CAMPFIRE || type == Material.CANDLE
                || type == Material.CAKE || type == Material.POWDER_SNOW) {
            return true;
        }

        // 排除所有容器类方块（箱子、陷阱箱、末影箱、漏斗等）
        if (type == Material.CHEST || type == Material.TRAPPED_CHEST
                || type == Material.ENDER_CHEST || type == Material.BARREL
                || type == Material.HOPPER || type == Material.DISPENSER
                || type == Material.DROPPER || type == Material.FURNACE
                || type == Material.BLAST_FURNACE || type == Material.SMOKER
                || type == Material.BREWING_STAND || type == Material.SHULKER_BOX
                || type == Material.BLACK_SHULKER_BOX || type == Material.BLUE_SHULKER_BOX
                || type == Material.BROWN_SHULKER_BOX || type == Material.CYAN_SHULKER_BOX
                || type == Material.GRAY_SHULKER_BOX || type == Material.GREEN_SHULKER_BOX
                || type == Material.LIGHT_BLUE_SHULKER_BOX || type == Material.LIGHT_GRAY_SHULKER_BOX
                || type == Material.LIME_SHULKER_BOX || type == Material.MAGENTA_SHULKER_BOX
                || type == Material.ORANGE_SHULKER_BOX || type == Material.PINK_SHULKER_BOX
                || type == Material.PURPLE_SHULKER_BOX || type == Material.RED_SHULKER_BOX
                || type == Material.WHITE_SHULKER_BOX || type == Material.YELLOW_SHULKER_BOX) {
            return true;
        }

        // 排除其他常见实体方块（如活塞、末地传送门等）
        if (type == Material.PISTON || type == Material.STICKY_PISTON
                || type == Material.MOVING_PISTON || type == Material.END_PORTAL
                || type == Material.END_GATEWAY || type == Material.NETHER_PORTAL) {
            return true;
        }

        return false;
    }

    public void saveToConfig(ConfigurationSection section) {
        section.set("world", world.getName());
        section.set("minX", minX);
        section.set("maxX", maxX);
        section.set("minY", minY);
        section.set("maxY", maxY);
        section.set("minZ", minZ);
        section.set("maxZ", maxZ);
        List<String> blockNames = new ArrayList<>();
        for (Material mat : blocks) {
            blockNames.add(mat.name());
        }
        section.set("blocks", blockNames);
    }

    // Getter
    public String getName() { return name; }
    public World getWorld() { return world; }
    public int getMinX() { return minX; }
    public int getMaxX() { return maxX; }
    public int getMinY() { return minY; }
    public int getMaxY() { return maxY; }
    public int getMinZ() { return minZ; }
    public int getMaxZ() { return maxZ; }
    public List<Material> getBlocks() { return blocks; }
}