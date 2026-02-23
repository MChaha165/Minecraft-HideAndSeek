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
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class CombinedDisguise implements Listener {

    private enum Mode { ARMOR_STAND, FALLING_BLOCK }

    private final Map<UUID, ArmorStand> armorStandMap = new HashMap<>();
    private final Map<UUID, FallingBlock> fallingBlockMap = new HashMap<>();
    private final Map<UUID, Mode> playerMode = new HashMap<>();
    private final Map<UUID, Material> blockTypeMap = new HashMap<>();
    private final Map<UUID, BukkitRunnable> lockTasks = new HashMap<>();
    private final LanguageManager lang = HideAndSeek.getInstance().getLanguageManager(); // 语言管理器

    // 盔甲架相对于玩家脚的Y偏移
    private final double ARMOR_STAND_Y_OFFSET = -1.2;
    // 下落方块生成时的Y偏移（使方块中心对齐地面）
    private final double FALLING_BLOCK_Y_OFFSET = 0.5;

    private final NamespacedKey disguiseKey = new NamespacedKey(HideAndSeek.getInstance(), "disguise");

    // ========== 全局默认随机方块列表（完整保留） ==========
    private static final Material[] RANDOM_BLOCKS = {
            // === 石头类 ===
            Material.STONE, Material.GRANITE, Material.POLISHED_GRANITE,
            Material.DIORITE, Material.POLISHED_DIORITE,
            Material.ANDESITE, Material.POLISHED_ANDESITE,
            Material.DEEPSLATE, Material.COBBLED_DEEPSLATE,
            Material.POLISHED_DEEPSLATE, Material.DEEPSLATE_BRICKS,
            Material.DEEPSLATE_TILES, Material.CRACKED_DEEPSLATE_BRICKS,
            Material.CRACKED_DEEPSLATE_TILES,
            Material.CALCITE, Material.TUFF, Material.DRIPSTONE_BLOCK,
            Material.SMOOTH_BASALT,
            // === 土/沙类 ===
            Material.DIRT, Material.COARSE_DIRT, Material.PODZOL,
            Material.ROOTED_DIRT, Material.MUD, Material.PACKED_MUD,
            Material.MUD_BRICKS,
            Material.SAND, Material.RED_SAND,
            Material.GRAVEL, Material.CLAY,
            // === 石头砖/建筑 ===
            Material.COBBLESTONE, Material.MOSSY_COBBLESTONE,
            Material.STONE_BRICKS, Material.MOSSY_STONE_BRICKS,
            Material.CRACKED_STONE_BRICKS, Material.CHISELED_STONE_BRICKS,
            Material.BRICKS, Material.PRISMARINE, Material.PRISMARINE_BRICKS,
            Material.DARK_PRISMARINE, Material.SEA_LANTERN,
            Material.NETHER_BRICKS, Material.CRACKED_NETHER_BRICKS,
            Material.CHISELED_NETHER_BRICKS, Material.RED_NETHER_BRICKS,
            Material.END_STONE, Material.END_STONE_BRICKS,
            Material.PURPUR_BLOCK, Material.PURPUR_PILLAR,
            Material.QUARTZ_BLOCK, Material.QUARTZ_BRICKS, Material.QUARTZ_PILLAR,
            Material.CHISELED_QUARTZ_BLOCK, Material.SMOOTH_QUARTZ,
            Material.SANDSTONE, Material.CHISELED_SANDSTONE,
            Material.CUT_SANDSTONE, Material.SMOOTH_SANDSTONE,
            Material.RED_SANDSTONE, Material.CHISELED_RED_SANDSTONE,
            Material.CUT_RED_SANDSTONE, Material.SMOOTH_RED_SANDSTONE,
            // === 木材类（完整方块）===
            Material.OAK_PLANKS, Material.SPRUCE_PLANKS,
            Material.BIRCH_PLANKS, Material.JUNGLE_PLANKS,
            Material.ACACIA_PLANKS, Material.DARK_OAK_PLANKS,
            Material.MANGROVE_PLANKS, Material.CHERRY_PLANKS,
            Material.BAMBOO_PLANKS, Material.BAMBOO_MOSAIC,
            Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG,
            Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG,
            Material.MANGROVE_LOG, Material.CHERRY_LOG,
            Material.STRIPPED_OAK_LOG, Material.STRIPPED_SPRUCE_LOG,
            Material.STRIPPED_BIRCH_LOG, Material.STRIPPED_JUNGLE_LOG,
            Material.STRIPPED_ACACIA_LOG, Material.STRIPPED_DARK_OAK_LOG,
            Material.STRIPPED_MANGROVE_LOG, Material.STRIPPED_CHERRY_LOG,
            Material.BAMBOO_BLOCK, Material.STRIPPED_BAMBOO_BLOCK,
            // === 下界/末地 ===
            Material.NETHERRACK, Material.CRIMSON_NYLIUM, Material.WARPED_NYLIUM,
            Material.SOUL_SAND, Material.SOUL_SOIL, Material.BASALT,
            Material.POLISHED_BASALT, Material.BLACKSTONE,
            Material.POLISHED_BLACKSTONE, Material.POLISHED_BLACKSTONE_BRICKS,
            Material.CRACKED_POLISHED_BLACKSTONE_BRICKS,
            Material.CHISELED_POLISHED_BLACKSTONE,
            Material.GILDED_BLACKSTONE,
            Material.MAGMA_BLOCK, Material.SHROOMLIGHT,
            // === 矿物块 ===
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.NETHER_QUARTZ_ORE, Material.NETHER_GOLD_ORE,
            Material.ANCIENT_DEBRIS,
            // === 纯矿物块 ===
            Material.COAL_BLOCK, Material.RAW_IRON_BLOCK, Material.RAW_COPPER_BLOCK,
            Material.RAW_GOLD_BLOCK, Material.IRON_BLOCK, Material.COPPER_BLOCK,
            Material.GOLD_BLOCK, Material.LAPIS_BLOCK, Material.REDSTONE_BLOCK,
            Material.DIAMOND_BLOCK, Material.EMERALD_BLOCK,
            Material.NETHERITE_BLOCK, Material.QUARTZ_BLOCK,
            // === 混凝土/陶瓦 ===
            Material.WHITE_CONCRETE, Material.ORANGE_CONCRETE,
            Material.MAGENTA_CONCRETE, Material.LIGHT_BLUE_CONCRETE,
            Material.YELLOW_CONCRETE, Material.LIME_CONCRETE,
            Material.PINK_CONCRETE, Material.GRAY_CONCRETE,
            Material.LIGHT_GRAY_CONCRETE, Material.CYAN_CONCRETE,
            Material.PURPLE_CONCRETE, Material.BLUE_CONCRETE,
            Material.BROWN_CONCRETE, Material.GREEN_CONCRETE,
            Material.RED_CONCRETE, Material.BLACK_CONCRETE,
            Material.WHITE_TERRACOTTA, Material.ORANGE_TERRACOTTA,
            Material.MAGENTA_TERRACOTTA, Material.LIGHT_BLUE_TERRACOTTA,
            Material.YELLOW_TERRACOTTA, Material.LIME_TERRACOTTA,
            Material.PINK_TERRACOTTA, Material.GRAY_TERRACOTTA,
            Material.LIGHT_GRAY_TERRACOTTA, Material.CYAN_TERRACOTTA,
            Material.PURPLE_TERRACOTTA, Material.BLUE_TERRACOTTA,
            Material.BROWN_TERRACOTTA, Material.GREEN_TERRACOTTA,
            Material.RED_TERRACOTTA, Material.BLACK_TERRACOTTA,
            // === 羊毛 ===
            Material.WHITE_WOOL, Material.ORANGE_WOOL, Material.MAGENTA_WOOL,
            Material.LIGHT_BLUE_WOOL, Material.YELLOW_WOOL, Material.LIME_WOOL,
            Material.PINK_WOOL, Material.GRAY_WOOL, Material.LIGHT_GRAY_WOOL,
            Material.CYAN_WOOL, Material.PURPLE_WOOL, Material.BLUE_WOOL,
            Material.BROWN_WOOL, Material.GREEN_WOOL, Material.RED_WOOL,
            Material.BLACK_WOOL,
            // === 其他固体方块 ===
            Material.BOOKSHELF, Material.MOSS_BLOCK, Material.SPONGE,
            Material.WET_SPONGE, Material.PUMPKIN, Material.CARVED_PUMPKIN,
            Material.MELON, Material.HAY_BLOCK, Material.HONEY_BLOCK,
            Material.SLIME_BLOCK, Material.TARGET, Material.DRIED_KELP_BLOCK,
            Material.COPPER_BLOCK, Material.EXPOSED_COPPER,
            Material.WEATHERED_COPPER, Material.OXIDIZED_COPPER,
            Material.WAXED_COPPER_BLOCK, Material.WAXED_EXPOSED_COPPER,
            Material.WAXED_WEATHERED_COPPER, Material.WAXED_OXIDIZED_COPPER,
            Material.CUT_COPPER, Material.EXPOSED_CUT_COPPER,
            Material.WEATHERED_CUT_COPPER, Material.OXIDIZED_CUT_COPPER,
            Material.WAXED_CUT_COPPER, Material.WAXED_EXPOSED_CUT_COPPER,
            Material.WAXED_WEATHERED_CUT_COPPER, Material.WAXED_OXIDIZED_CUT_COPPER,
            Material.AMETHYST_BLOCK, Material.BUDDING_AMETHYST,
            Material.SCULK, Material.SCULK_CATALYST, Material.SCULK_SHRIEKER,
            // === 玻璃（仅保留普通玻璃和玻璃板）===
            Material.GLASS, Material.GLASS_PANE,
            // === 楼梯 ===
            Material.OAK_STAIRS, Material.SPRUCE_STAIRS, Material.BIRCH_STAIRS,
            Material.JUNGLE_STAIRS, Material.ACACIA_STAIRS, Material.DARK_OAK_STAIRS,
            Material.MANGROVE_STAIRS, Material.CHERRY_STAIRS, Material.BAMBOO_STAIRS,
            Material.BAMBOO_MOSAIC_STAIRS, Material.COBBLESTONE_STAIRS,
            Material.STONE_STAIRS, Material.STONE_BRICK_STAIRS,
            Material.MOSSY_COBBLESTONE_STAIRS, Material.MOSSY_STONE_BRICK_STAIRS,
            Material.ANDESITE_STAIRS, Material.POLISHED_ANDESITE_STAIRS,
            Material.DIORITE_STAIRS, Material.POLISHED_DIORITE_STAIRS,
            Material.GRANITE_STAIRS, Material.POLISHED_GRANITE_STAIRS,
            Material.DEEPSLATE_BRICK_STAIRS, Material.DEEPSLATE_TILE_STAIRS,
            Material.COBBLED_DEEPSLATE_STAIRS, Material.POLISHED_DEEPSLATE_STAIRS,
            Material.BRICK_STAIRS, Material.MUD_BRICK_STAIRS,
            Material.SANDSTONE_STAIRS, Material.SMOOTH_SANDSTONE_STAIRS,
            Material.RED_SANDSTONE_STAIRS, Material.SMOOTH_RED_SANDSTONE_STAIRS,
            Material.QUARTZ_STAIRS, Material.SMOOTH_QUARTZ_STAIRS,
            Material.PRISMARINE_STAIRS, Material.PRISMARINE_BRICK_STAIRS,
            Material.DARK_PRISMARINE_STAIRS,
            Material.PURPUR_STAIRS,
            Material.NETHER_BRICK_STAIRS, Material.RED_NETHER_BRICK_STAIRS,
            Material.BLACKSTONE_STAIRS, Material.POLISHED_BLACKSTONE_STAIRS,
            Material.POLISHED_BLACKSTONE_BRICK_STAIRS,
            Material.END_STONE_BRICK_STAIRS,
            Material.CUT_COPPER_STAIRS, Material.EXPOSED_CUT_COPPER_STAIRS,
            Material.WEATHERED_CUT_COPPER_STAIRS, Material.OXIDIZED_CUT_COPPER_STAIRS,
            Material.WAXED_CUT_COPPER_STAIRS, Material.WAXED_EXPOSED_CUT_COPPER_STAIRS,
            Material.WAXED_WEATHERED_CUT_COPPER_STAIRS, Material.WAXED_OXIDIZED_CUT_COPPER_STAIRS
    };
    private final Random random = new Random();

    // ========== 方块中文名称映射（保留但不再用于玩家消息，可用于其他用途） ==========
    private static final Map<Material, String> chineseNames = new HashMap<>();
    static {
        // ... 原有完整映射保持不变（此处省略以节省篇幅，请确保保留你的实际代码）
        // 建议保留你的原样，因为不影响功能。
    }

    // 格式化方块名为首字母大写的英文（如 STONE -> Stone, OAK_STAIRS -> Oak Stairs）
    private String formatMaterialName(Material material) {
        String name = material.name().toLowerCase().replace('_', ' ');
        String[] words = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1)).append(' ');
            }
        }
        return sb.toString().trim();
    }

    public void testDisguise(Player player, Material blockType) {
        if (playerMode.containsKey(player.getUniqueId())) {
            removeDisguise(player);
        }
        startArmorStandMode(player, blockType);
        player.getInventory().setItemInMainHand(null);
        player.sendMessage(lang.getMessage(player, "disguise.forced", formatMaterialName(blockType)));
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() != Material.CARROT_ON_A_STICK) return;

        RegionManager regionManager = HideAndSeek.getInstance().getRegionManager();
        Region region = regionManager.getRegionAt(player.getLocation());
        Material[] availableBlocks;
        if (region != null && !region.getBlocks().isEmpty()) {
            availableBlocks = region.getBlocks().toArray(new Material[0]);
        } else {
            availableBlocks = RANDOM_BLOCKS;
        }

        if (playerMode.containsKey(player.getUniqueId())) {
            if (event.getAction().name().contains("RIGHT_CLICK")) {
                Material randomBlock = availableBlocks[random.nextInt(availableBlocks.length)];
                removeDisguise(player);
                startArmorStandMode(player, randomBlock);
                player.getInventory().setItemInMainHand(null);
                player.sendMessage(lang.getMessage(player, "disguise.random_reminder", formatMaterialName(randomBlock)));
                event.setCancelled(true);
            }
            return;
        }

        if (event.getAction().name().contains("RIGHT_CLICK")) {
            Material randomBlock = availableBlocks[random.nextInt(availableBlocks.length)];
            startArmorStandMode(player, randomBlock);
            player.getInventory().setItemInMainHand(null);
            player.sendMessage(lang.getMessage(player, "disguise.random", formatMaterialName(randomBlock)));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        event.setCancelled(true);

        if (!playerMode.containsKey(player.getUniqueId())) return;

        Mode current = playerMode.get(player.getUniqueId());
        if (current == Mode.ARMOR_STAND) {
            switchToFallingBlock(player);
        } else {
            switchToArmorStand(player);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uid = player.getUniqueId();
        Mode mode = playerMode.get(uid);
        if (mode == null) return;

        if (mode == Mode.FALLING_BLOCK) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (to != null && (to.getX() != from.getX() || to.getY() != from.getY() || to.getZ() != from.getZ())) {
                event.setCancelled(true);
            }
            return;
        }

        ArmorStand as = armorStandMap.get(uid);
        if (as != null && as.isValid()) {
            Location target = player.getLocation().clone();
            target.setY(target.getY() + ARMOR_STAND_Y_OFFSET);
            as.teleport(target);
        }
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (playerMode.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeDisguise(event.getPlayer());
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity damaged = event.getEntity();
        Entity damager = event.getDamager();

        if (damager instanceof Player) {
            Player pDamager = (Player) damager;
            if (damaged instanceof ArmorStand) {
                ArmorStand as = (ArmorStand) damaged;
                if (as.getPersistentDataContainer().has(disguiseKey, PersistentDataType.BOOLEAN)) {
                    for (Map.Entry<UUID, ArmorStand> entry : armorStandMap.entrySet()) {
                        if (entry.getValue().equals(as) && entry.getKey().equals(pDamager.getUniqueId())) {
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
            } else if (damaged instanceof FallingBlock) {
                FallingBlock fb = (FallingBlock) damaged;
                if (fb.getPersistentDataContainer().has(disguiseKey, PersistentDataType.BOOLEAN)) {
                    for (Map.Entry<UUID, FallingBlock> entry : fallingBlockMap.entrySet()) {
                        if (entry.getValue().equals(fb) && entry.getKey().equals(pDamager.getUniqueId())) {
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
            }
        }

        if (damaged instanceof ArmorStand) {
            ArmorStand as = (ArmorStand) damaged;
            if (as.getPersistentDataContainer().has(disguiseKey, PersistentDataType.BOOLEAN)) {
                for (Map.Entry<UUID, ArmorStand> entry : armorStandMap.entrySet()) {
                    if (entry.getValue().equals(as)) {
                        Player player = Bukkit.getPlayer(entry.getKey());
                        if (player != null && player.isOnline()) {
                            as.getWorld().playSound(as.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
                            as.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, as.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.1);
                            event.setCancelled(true);
                            player.damage(event.getDamage(), event.getDamager());
                        }
                        break;
                    }
                }
            }
        } else if (damaged instanceof FallingBlock) {
            FallingBlock fb = (FallingBlock) damaged;
            if (fb.getPersistentDataContainer().has(disguiseKey, PersistentDataType.BOOLEAN)) {
                for (Map.Entry<UUID, FallingBlock> entry : fallingBlockMap.entrySet()) {
                    if (entry.getValue().equals(fb)) {
                        Player player = Bukkit.getPlayer(entry.getKey());
                        if (player != null && player.isOnline()) {
                            fb.getWorld().playSound(fb.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
                            fb.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, fb.getLocation().add(0, 0.5, 0), 10, 0.3, 0.5, 0.3, 0.1);
                            event.setCancelled(true);
                            player.damage(event.getDamage(), event.getDamager());
                        }
                        break;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof FallingBlock) {
            FallingBlock fb = (FallingBlock) event.getEntity();
            if (fb.getPersistentDataContainer().has(disguiseKey, PersistentDataType.BOOLEAN)) {
                event.setCancelled(true);
            }
        }
    }

    private void startArmorStandMode(Player player, Material blockType) {
        removeDisguise(player);

        Location loc = player.getLocation().clone();
        loc.setY(loc.getY() + ARMOR_STAND_Y_OFFSET);

        ArmorStand as = player.getWorld().spawn(loc, ArmorStand.class);
        as.setVisible(false);
        as.setGravity(false);
        as.setSmall(false);
        as.setBasePlate(false);
        as.setArms(false);
        as.setCollidable(true);

        as.getPersistentDataContainer().set(disguiseKey, PersistentDataType.BOOLEAN, true);
        if (as.getEquipment() != null) {
            as.getEquipment().setHelmet(new ItemStack(blockType));
        }

        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false, false));

        armorStandMap.put(player.getUniqueId(), as);
        playerMode.put(player.getUniqueId(), Mode.ARMOR_STAND);
        blockTypeMap.put(player.getUniqueId(), blockType);

        player.sendMessage(lang.getMessage(player, "disguise.mode_movement", formatMaterialName(blockType)));
    }

    private void switchToFallingBlock(Player player) {
        UUID uid = player.getUniqueId();

        ArmorStand as = armorStandMap.remove(uid);
        if (as != null && as.isValid()) as.remove();

        Material blockType = blockTypeMap.get(uid);
        if (blockType == null) return;

        int blockX = player.getLocation().getBlockX();
        int blockY = player.getLocation().getBlockY();
        int blockZ = player.getLocation().getBlockZ();

        Location fbLoc = new Location(player.getWorld(), blockX + 0.5, blockY + 0.5, blockZ + 0.5);
        BlockData blockData = blockType.createBlockData();
        FallingBlock fb = player.getWorld().spawnFallingBlock(fbLoc, blockData);
        fb.setGravity(false);
        fb.setDropItem(false);
        fb.setHurtEntities(false);
        fb.setInvulnerable(true);
        fb.setPersistent(true);
        fb.setTicksLived(Integer.MAX_VALUE);

        fb.getPersistentDataContainer().set(disguiseKey, PersistentDataType.BOOLEAN, true);

        fallingBlockMap.put(uid, fb);
        playerMode.put(uid, Mode.FALLING_BLOCK);

        double targetY = fbLoc.getY() + 0.5;
        Location topLoc = fbLoc.clone();
        topLoc.setY(targetY);
        topLoc.setYaw(player.getLocation().getYaw());
        topLoc.setPitch(player.getLocation().getPitch());
        player.teleport(topLoc);

        player.setAllowFlight(true);
        player.setFlying(true);
        player.setGravity(false);

        BukkitRunnable lockTask = new BukkitRunnable() {
            @Override
            public void run() {
                FallingBlock currentFb = fallingBlockMap.get(uid);
                if (currentFb == null || !currentFb.isValid() || !player.isOnline()) {
                    this.cancel();
                    lockTasks.remove(uid);
                    return;
                }
                Location target = currentFb.getLocation().clone();
                target.setY(target.getY() + 0.5);
                target.setYaw(player.getLocation().getYaw());
                target.setPitch(player.getLocation().getPitch());

                if (player.getLocation().distanceSquared(target) > 0.1) {
                    player.teleport(target);
                }
            }
        };
        lockTask.runTaskTimer(HideAndSeek.getInstance(), 0L, 1L);
        lockTasks.put(uid, lockTask);

        player.sendMessage(lang.getMessage(player, "disguise.mode_frozen", formatMaterialName(blockType)));
    }

    private void switchToArmorStand(Player player) {
        UUID uid = player.getUniqueId();

        BukkitRunnable lockTask = lockTasks.remove(uid);
        if (lockTask != null) lockTask.cancel();

        FallingBlock fb = fallingBlockMap.remove(uid);
        if (fb != null && fb.isValid()) fb.remove();

        Material blockType = blockTypeMap.get(uid);
        if (blockType == null) return;

        player.setAllowFlight(false);
        player.setFlying(false);
        player.setGravity(true);

        startArmorStandMode(player, blockType);
        player.sendMessage(lang.getMessage(player, "disguise.mode_unfrozen"));
    }

    public void removeDisguise(Player player) {
        UUID uid = player.getUniqueId();

        BukkitRunnable lockTask = lockTasks.remove(uid);
        if (lockTask != null) lockTask.cancel();

        ArmorStand as = armorStandMap.remove(uid);
        if (as != null && as.isValid()) as.remove();

        FallingBlock fb = fallingBlockMap.remove(uid);
        if (fb != null && fb.isValid()) fb.remove();

        player.removePotionEffect(PotionEffectType.INVISIBILITY);

        // 根据游戏模式恢复飞行权限
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            player.setAllowFlight(true);
        } else {
            player.setAllowFlight(false);
            player.setFlying(false);
        }
        player.setGravity(true);

        playerMode.remove(uid);
        blockTypeMap.remove(uid);
    }

    public void cleanupDisguises() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof ArmorStand || entity instanceof FallingBlock) {
                    if (entity.getPersistentDataContainer().has(disguiseKey, PersistentDataType.BOOLEAN)) {
                        entity.remove();
                    }
                }
            }
        }
    }
}