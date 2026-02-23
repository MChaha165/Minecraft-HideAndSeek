/*
 * HideAndSeek - 躲猫猫插件
 * Copyright (c) 2026 MChaha165. All rights reserved.
 *
 * 本代码仅供个人学习、研究使用。
 * 未经作者书面许可，禁止任何形式的商业使用。
 * 项目地址: https://github.com/MChaha165/Minecraft-HideAndSeek/
 */

package com.mchaha165.hideandseek;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class PlayerData {
    private final ItemStack[] inventoryContents;
    private final ItemStack[] armorContents;
    private final ItemStack[] extraContents;
    private final ItemStack[] enderChestContents;
    private final long savedTime;

    public PlayerData(Player player) {
        PlayerInventory inv = player.getInventory();
        this.inventoryContents = inv.getContents();
        this.armorContents = inv.getArmorContents();
        this.extraContents = inv.getExtraContents();
        this.enderChestContents = player.getEnderChest().getContents();
        this.savedTime = System.currentTimeMillis();
    }

    public void restore(Player player) {
        player.getInventory().setContents(inventoryContents);
        player.getInventory().setArmorContents(armorContents);
        player.getInventory().setExtraContents(extraContents);
        player.getEnderChest().setContents(enderChestContents);
    }

    public void clear(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setExtraContents(new ItemStack[player.getInventory().getExtraContents().length]);
        player.getEnderChest().clear();
    }

    public boolean isExpired(long now, long maxAgeMillis) {
        return now - savedTime > maxAgeMillis;
    }
}