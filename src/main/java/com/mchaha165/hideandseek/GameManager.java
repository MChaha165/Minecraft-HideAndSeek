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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class GameManager {
    private static GameManager instance;
    private final HideAndSeek plugin;
    private final LanguageManager lang;
    private Location lobbyLocation;
    private int huntersPerPlayers = 4;
    private int minPlayers = 2;
    private int defaultGameTime = 300;
    private int offlineTimeoutSeconds = 60;

    public static final int HUNTER_DELAY_SECONDS = 45;

    private final Map<String, List<Location>> hiderSpawns = new HashMap<>();
    private final Map<String, List<Location>> hunterSpawns = new HashMap<>();
    private List<HunterWeapon> hunterWeapons = new ArrayList<>();
    private final Map<String, GameInstance> activeGames = new HashMap<>();
    private final Map<UUID, PlayerData> storedPlayerData = new HashMap<>();

    private boolean lobbyLoadAttempted = false;

    public GameManager(HideAndSeek plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
        instance = this;
        loadConfig(); // 立即加载，但因为是手动加载，即使世界不存在也不会崩溃

        // 延迟3秒重新检查世界是否已加载（用于后续游戏中的出生点有效性）
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getLogger().info("尝试重新加载出生点和世界坐标（确保世界已加载）...");
            reloadSpawnsAfterWorldsLoaded();
        }, 60L);
    }

    public static GameManager getInstance() {
        return instance;
    }

    // ==================== 配置加载（手动方式，无自动序列化）====================
    public void loadConfig() {
        plugin.reloadConfig(); // 安全：由于我们改变了存储格式，config.yml 中将不再包含自动序列化条目
        FileConfiguration config = plugin.getConfig();

        huntersPerPlayers = config.getInt("game-settings.hunters-per-players", 4);
        minPlayers = config.getInt("game-settings.min-players", 2);
        defaultGameTime = config.getInt("game-settings.default-game-time", 300);
        offlineTimeoutSeconds = config.getInt("offline-timeout-seconds", 60);

        // --- 手动加载大厅坐标 ---
        if (config.contains("lobby.world")) {
            String worldName = config.getString("lobby.world");
            if (worldName != null) {
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    double x = config.getDouble("lobby.x");
                    double y = config.getDouble("lobby.y");
                    double z = config.getDouble("lobby.z");
                    float yaw = (float) config.getDouble("lobby.yaw");
                    float pitch = (float) config.getDouble("lobby.pitch");
                    lobbyLocation = new Location(world, x, y, z, yaw, pitch);
                    plugin.getLogger().info("§a大厅坐标已加载: " + lobbyLocation.toString());
                } else {
                    plugin.getLogger().warning("§c大厅世界 '" + worldName + "' 不存在，请检查配置文件或等待世界加载后重试。");
                }
            }
        } else {
            plugin.getLogger().info("§e未找到大厅坐标配置，请使用 /hs setlobby 设置。");
        }

        // 加载出生点
        loadSpawns();
        // 加载猎人武器
        loadHunterWeapons();
        lobbyLoadAttempted = true;
    }

    // 重新加载出生点（在世界加载后调用，确保世界存在）
    private void reloadSpawnsAfterWorldsLoaded() {
        FileConfiguration config = plugin.getConfig();

        // 重新加载大厅 - 仅在配置中有实际世界名时尝试
        if (config.contains("lobby.world")) {
            String worldName = config.getString("lobby.world");
            if (worldName != null) {
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    double x = config.getDouble("lobby.x");
                    double y = config.getDouble("lobby.y");
                    double z = config.getDouble("lobby.z");
                    float yaw = (float) config.getDouble("lobby.yaw");
                    float pitch = (float) config.getDouble("lobby.pitch");
                    lobbyLocation = new Location(world, x, y, z, yaw, pitch);
                    plugin.getLogger().info("§a大厅坐标重新加载成功: " + lobbyLocation.toString());
                } else {
                    plugin.getLogger().warning("§c重新加载时大厅世界 '" + worldName + "' 仍然不存在。");
                }
            }
        } else {
            // 没有配置 lobby.world，说明用户尚未设置，无需警告
            // 可以忽略
        }

        // 重新加载出生点（清空原列表，重新加载）
        hiderSpawns.clear();
        hunterSpawns.clear();
        loadSpawns(); // loadSpawns 会从配置读取并过滤世界为 null 的坐标，但此时世界已存在
        plugin.getLogger().info("§a出生点重新加载完成。");
    }

    // 手动加载出生点（从配置读取 Map 列表）
    private void loadSpawns() {
        hiderSpawns.clear();
        hunterSpawns.clear();
        FileConfiguration config = plugin.getConfig();

        if (config.contains("spawns")) {
            ConfigurationSection spawns = config.getConfigurationSection("spawns");
            for (String regionName : spawns.getKeys(false)) {
                ConfigurationSection regionSec = spawns.getConfigurationSection(regionName);

                // 加载 hiders
                if (regionSec.contains("hiders")) {
                    List<Map<?, ?>> rawList = regionSec.getMapList("hiders");
                    List<Location> locations = new ArrayList<>();
                    for (Map<?, ?> map : rawList) {
                        String worldName = (String) map.get("world");
                        if (worldName == null) continue;
                        World world = Bukkit.getWorld(worldName);
                        if (world == null) {
                            plugin.getLogger().warning("跳过出生点：世界 " + worldName + " 不存在");
                            continue;
                        }
                        double x = ((Number) map.get("x")).doubleValue();
                        double y = ((Number) map.get("y")).doubleValue();
                        double z = ((Number) map.get("z")).doubleValue();
                        float yaw = ((Number) map.get("yaw")).floatValue();
                        float pitch = ((Number) map.get("pitch")).floatValue();
                        locations.add(new Location(world, x, y, z, yaw, pitch));
                    }
                    hiderSpawns.put(regionName, locations);
                }

                // 加载 hunters
                if (regionSec.contains("hunters")) {
                    List<Map<?, ?>> rawList = regionSec.getMapList("hunters");
                    List<Location> locations = new ArrayList<>();
                    for (Map<?, ?> map : rawList) {
                        String worldName = (String) map.get("world");
                        if (worldName == null) continue;
                        World world = Bukkit.getWorld(worldName);
                        if (world == null) {
                            plugin.getLogger().warning("跳过出生点：世界 " + worldName + " 不存在");
                            continue;
                        }
                        double x = ((Number) map.get("x")).doubleValue();
                        double y = ((Number) map.get("y")).doubleValue();
                        double z = ((Number) map.get("z")).doubleValue();
                        float yaw = ((Number) map.get("yaw")).floatValue();
                        float pitch = ((Number) map.get("pitch")).floatValue();
                        locations.add(new Location(world, x, y, z, yaw, pitch));
                    }
                    hunterSpawns.put(regionName, locations);
                }
            }
        }
    }

    // 手动保存出生点（将 Location 转换为 Map 列表）
    public void saveSpawns() {
        FileConfiguration config = plugin.getConfig();
        config.set("spawns", null); // 清空原有数据

        for (Map.Entry<String, List<Location>> entry : hiderSpawns.entrySet()) {
            String region = entry.getKey();
            List<Map<String, Object>> hiderList = new ArrayList<>();
            for (Location loc : entry.getValue()) {
                if (loc != null && loc.getWorld() != null) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("world", loc.getWorld().getName());
                    map.put("x", loc.getX());
                    map.put("y", loc.getY());
                    map.put("z", loc.getZ());
                    map.put("yaw", (double) loc.getYaw());
                    map.put("pitch", (double) loc.getPitch());
                    hiderList.add(map);
                }
            }
            if (!hiderList.isEmpty()) {
                config.set("spawns." + region + ".hiders", hiderList);
            }
        }

        for (Map.Entry<String, List<Location>> entry : hunterSpawns.entrySet()) {
            String region = entry.getKey();
            List<Map<String, Object>> hunterList = new ArrayList<>();
            for (Location loc : entry.getValue()) {
                if (loc != null && loc.getWorld() != null) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("world", loc.getWorld().getName());
                    map.put("x", loc.getX());
                    map.put("y", loc.getY());
                    map.put("z", loc.getZ());
                    map.put("yaw", (double) loc.getYaw());
                    map.put("pitch", (double) loc.getPitch());
                    hunterList.add(map);
                }
            }
            if (!hunterList.isEmpty()) {
                config.set("spawns." + region + ".hunters", hunterList);
            }
        }

        plugin.saveConfig();
    }

    private void loadHunterWeapons() {
        hunterWeapons.clear();
        FileConfiguration config = plugin.getConfig();
        if (config.contains("hunter-weapons")) {
            List<Map<?, ?>> list = config.getMapList("hunter-weapons");
            for (Map<?, ?> map : list) {
                HunterWeapon weapon = new HunterWeapon();
                weapon.material = Material.valueOf((String) map.get("material"));
                weapon.amount = (int) map.get("amount");
                weapon.slot = (int) map.get("slot");
                hunterWeapons.add(weapon);
            }
        } else {
            hunterWeapons.add(new HunterWeapon(Material.IRON_SWORD, 1, 0));
            hunterWeapons.add(new HunterWeapon(Material.BOW, 1, 1));
            hunterWeapons.add(new HunterWeapon(Material.ARROW, 32, 2));
        }
    }

    // ==================== 出生点管理 ====================
    public void addHiderSpawn(String regionName, Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        hiderSpawns.computeIfAbsent(regionName, k -> new ArrayList<>()).add(loc);
        saveSpawns();
    }

    public void addHunterSpawn(String regionName, Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        hunterSpawns.computeIfAbsent(regionName, k -> new ArrayList<>()).add(loc);
        saveSpawns();
    }

    public Location getHiderSpawn(String regionName) {
        List<Location> list = hiderSpawns.get(regionName);
        if (list == null || list.isEmpty()) return null;
        // 过滤出世界存在的（但理论上应该都已经存在）
        List<Location> validList = new ArrayList<>();
        for (Location loc : list) {
            if (loc != null && loc.getWorld() != null) {
                validList.add(loc);
            }
        }
        if (validList.isEmpty()) return null;
        return validList.get(new Random().nextInt(validList.size()));
    }

    public Location getHunterSpawn(String regionName) {
        List<Location> list = hunterSpawns.get(regionName);
        if (list == null || list.isEmpty()) return null;
        List<Location> validList = new ArrayList<>();
        for (Location loc : list) {
            if (loc != null && loc.getWorld() != null) {
                validList.add(loc);
            }
        }
        if (validList.isEmpty()) return null;
        return validList.get(new Random().nextInt(validList.size()));
    }

    // ==================== 大厅 ====================
    public void setLobbyLocation(Location loc) {
        this.lobbyLocation = loc;
        FileConfiguration config = plugin.getConfig();
        if (loc != null && loc.getWorld() != null) {
            config.set("lobby.world", loc.getWorld().getName());
            config.set("lobby.x", loc.getX());
            config.set("lobby.y", loc.getY());
            config.set("lobby.z", loc.getZ());
            config.set("lobby.yaw", (double) loc.getYaw());
            config.set("lobby.pitch", (double) loc.getPitch());
        } else {
            config.set("lobby", null); // 清空无效数据
        }
        plugin.saveConfig();
    }

    public Location getLobbyLocation() {
        if (lobbyLocation == null && !lobbyLoadAttempted) {
            // 如果尚未尝试加载，可以尝试重新加载配置（但一般不会走到这里）
            loadConfig();
        }
        // 如果世界为 null，尝试从配置重新获取世界（可能世界后来才加载）
        if (lobbyLocation != null && lobbyLocation.getWorld() == null) {
            String worldName = plugin.getConfig().getString("lobby.world");
            if (worldName != null) {
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    lobbyLocation.setWorld(world);
                    plugin.getLogger().info("§a大厅坐标世界已修复: " + lobbyLocation.toString());
                }
            }
        }
        return lobbyLocation;
    }

    // ==================== 物品暂存 ====================
    public void savePlayerData(Player player) {
        PlayerData data = new PlayerData(player);
        storedPlayerData.put(player.getUniqueId(), data);
        data.clear(player);
    }

    public void restorePlayerData(Player player) {
        PlayerData data = storedPlayerData.remove(player.getUniqueId());
        if (data != null) {
            data.restore(player);
        }
    }

    public boolean hasStoredData(Player player) {
        return storedPlayerData.containsKey(player.getUniqueId());
    }

    public void reconnectPlayer(Player player) {
        for (GameInstance game : activeGames.values()) {
            if (game.reconnectPlayer(player)) {
                return;
            }
        }
        restorePlayerDataOnJoin(player);
    }

    public void restorePlayerDataOnJoin(Player player) {
        PlayerData data = storedPlayerData.remove(player.getUniqueId());
        if (data != null) {
            data.restore(player);
            player.sendMessage(lang.getMessage(player, "offline.items_restored"));
        }
    }

    // ==================== 游戏管理 ====================
    public GameInstance getGameByRegion(String regionName) {
        return activeGames.get(regionName);
    }

    public GameInstance getGameByPlayer(Player player) {
        for (GameInstance game : activeGames.values()) {
            if (game.containsPlayer(player)) return game;
        }
        return null;
    }

    public void startGame(String regionName) {
        Region region = plugin.getRegionManager().getRegion(regionName);
        if (region == null) {
            Bukkit.getLogger().warning("尝试开始游戏，但区域 " + regionName + " 不存在！");
            return;
        }
        if (activeGames.containsKey(regionName)) {
            Bukkit.getLogger().warning("区域 " + regionName + " 已有游戏在进行！");
            return;
        }
        GameInstance game = new GameInstance(region, defaultGameTime);
        activeGames.put(regionName, game);
    }

    public void joinGame(Player player, String regionName) {
        Region region = plugin.getRegionManager().getRegion(regionName);
        if (region == null) {
            player.sendMessage(lang.getMessage(player, "cmd.region.not_found", regionName));
            return;
        }

        GameInstance game = activeGames.get(regionName);
        if (game == null) {
            game = new GameInstance(region, defaultGameTime);
            activeGames.put(regionName, game);
            player.sendMessage(lang.getMessage(player, "game.queue.created", regionName));
        }

        if (game.getState() != GameInstance.GameState.WAITING) {
            player.sendMessage(lang.getMessage(player, "game.queue.not_waiting"));
            return;
        }

        game.addWaitingPlayer(player);
    }

    public void leaveGame(Player player) {
        GameInstance game = getGameByPlayer(player);
        if (game != null) {
            if (game.getState() == GameInstance.GameState.WAITING) {
                game.removeWaitingPlayer(player);
            } else {
                game.removePlayerImmediately(player);
            }
        }
    }

    public void removeGame(String regionName) {
        activeGames.remove(regionName);
    }

    public void disableAllGames() {
        for (GameInstance game : new ArrayList<>(activeGames.values())) {
            game.endGame();
        }
        activeGames.clear();
    }

    // ==================== 猎人装备 ====================
    public void giveHunterKit(Player player) {
        player.getInventory().clear();
        for (HunterWeapon weapon : hunterWeapons) {
            ItemStack item = new ItemStack(weapon.material, weapon.amount);
            player.getInventory().setItem(weapon.slot, item);
        }
    }

    // ==================== 发放变身道具 ====================
    public static void giveItem(Player player) {
        ItemStack rod = new ItemStack(Material.CARROT_ON_A_STICK);
        ItemMeta meta = rod.getItemMeta();
        if (meta != null) {
            LanguageManager lang = HideAndSeek.getInstance().getLanguageManager();
            meta.setDisplayName(lang.getMessage(player, "item.name"));

            List<String> lore = new ArrayList<>();
            lore.add(lang.getMessage(player, "item.lore.author", "MChaha165"));
            lore.add(lang.getMessage(player, "item.lore.version", HideAndSeek.getInstance().getDescription().getVersion()));
            lore.add(lang.getMessage(player, "item.lore.usage"));
            meta.setLore(lore);

            rod.setItemMeta(meta);
        }
        player.getInventory().addItem(rod);
    }

    // ==================== 事件转发 ====================
    public void onPlayerDeath(Player player) {
        GameInstance game = getGameByPlayer(player);
        if (game != null) game.onPlayerDeath(player);
    }

    public void onPlayerQuit(Player player) {
        GameInstance game = getGameByPlayer(player);
        if (game != null) game.onPlayerQuit(player);
    }

    // ==================== Getter 配置 ====================
    public int getHuntersPerPlayers() { return huntersPerPlayers; }
    public int getMinPlayers() { return minPlayers; }
    public int getDefaultGameTime() { return defaultGameTime; }
    public int getOfflineTimeoutSeconds() { return offlineTimeoutSeconds; }

    // ==================== 内部类 ====================
    private static class HunterWeapon {
        Material material;
        int amount;
        int slot;
        HunterWeapon() {}
        HunterWeapon(Material material, int amount, int slot) {
            this.material = material;
            this.amount = amount;
            this.slot = slot;
        }
    }
}