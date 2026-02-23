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
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class GameInstance {
    private final String regionName;
    private final Region region;
    private GameState state = GameState.WAITING;
    private final List<UUID> waitingPlayers = new ArrayList<>();
    private final Set<UUID> readyPlayers = new HashSet<>();
    private final List<UUID> hiders = new ArrayList<>();
    private final List<UUID> hunters = new ArrayList<>();
    private final List<UUID> spectators = new ArrayList<>();
    private final Set<UUID> unreleasedHunters = new HashSet<>();
    private final Map<UUID, Integer> offlineTaskIds = new HashMap<>();
    private final Map<UUID, Long> offlinePlayers = new HashMap<>();
    private final LanguageManager lang;

    private int gameTime;
    private int timerTaskId = -1;
    private int startCountdownTaskId = -1;
    private int hunterDelayTaskId = -1;

    public enum GameState { WAITING, STARTING, ACTIVE, ENDED }

    public GameInstance(Region region, int defaultGameTime) {
        this.region = region;
        this.regionName = region.getName();
        this.gameTime = defaultGameTime;
        this.lang = HideAndSeek.getInstance().getLanguageManager();
    }

    // ==================== 等待阶段 ====================
    private void updateWaitingScoreboardForPlayer(Player player) {
        Scoreboard board = player.getScoreboard();
        Objective obj = board.getObjective(DisplaySlot.SIDEBAR);
        if (obj == null) return;
        for (String entry : board.getEntries()) {
            board.resetScores(entry);
        }
        obj.getScore(lang.getMessage(player, "scoreboard.waiting.joined", waitingPlayers.size())).setScore(3);
        obj.getScore(lang.getMessage(player, "scoreboard.waiting.ready", readyPlayers.size())).setScore(2);
        obj.getScore(lang.getMessage(player, "scoreboard.waiting.min_players", GameManager.getInstance().getMinPlayers())).setScore(1);
    }

    private void updateAllWaitingScoreboards() {
        for (UUID uuid : waitingPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                updateWaitingScoreboardForPlayer(p);
            }
        }
    }

    public void addWaitingPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        if (waitingPlayers.contains(uuid)) {
            player.sendMessage(lang.getMessage(player, "game.waiting.already_joined"));
            return;
        }

        GameManager.getInstance().savePlayerData(player);
        waitingPlayers.add(uuid);

        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("waiting", "dummy",
                lang.getMessage(player, "scoreboard.title.waiting"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        player.setScoreboard(board);
        updateWaitingScoreboardForPlayer(player);

        Location lobby = GameManager.getInstance().getLobbyLocation();
        if (lobby != null && lobby.getWorld() != null) {
            player.teleport(lobby);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        } else {
            player.sendMessage(lang.getMessage(player, "game.waiting.lobby_not_set"));
        }

        player.sendMessage(lang.getMessage(player, "game.join.waiting", regionName, waitingPlayers.size()));
        player.sendMessage(lang.getMessage(player, "game.ready.reminder"));

        updateAllWaitingScoreboards();
    }

    public void removeWaitingPlayer(Player player) {
        waitingPlayers.remove(player.getUniqueId());
        readyPlayers.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        GameManager.getInstance().restorePlayerData(player);
        updateAllWaitingScoreboards();
    }

    public void readyPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        if (!waitingPlayers.contains(uuid)) {
            player.sendMessage(lang.getMessage(player, "cmd.ready.not_in_queue"));
            return;
        }
        if (readyPlayers.contains(uuid)) {
            player.sendMessage(lang.getMessage(player, "cmd.ready.already"));
            return;
        }
        readyPlayers.add(uuid);
        player.sendMessage(lang.getMessage(player, "game.ready.success", readyPlayers.size(), waitingPlayers.size()));
        updateAllWaitingScoreboards();

        if (readyPlayers.size() == waitingPlayers.size() && waitingPlayers.size() >= GameManager.getInstance().getMinPlayers()) {
            if (startCountdownTaskId != -1) {
                Bukkit.getScheduler().cancelTask(startCountdownTaskId);
            }
            startStartCountdown();
        }
    }

    private void startStartCountdown() {
        state = GameState.STARTING;
        startCountdownTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(HideAndSeek.getInstance(), new Runnable() {
            int countdown = 10;
            @Override
            public void run() {
                if (countdown <= 0) {
                    Bukkit.getScheduler().cancelTask(startCountdownTaskId);
                    startCountdownTaskId = -1;
                    startGameNow();
                    return;
                }
                for (UUID uuid : waitingPlayers) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) {
                        p.sendMessage(lang.getMessage(p, "game.start.countdown", countdown));
                        if (countdown <= 3) {
                            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                        } else {
                            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                        }
                    }
                }
                countdown--;
            }
        }, 0L, 20L);
    }

    // ==================== 游戏开始 ====================
    public void startGameNow() {
        if (waitingPlayers.isEmpty()) return;
        state = GameState.ACTIVE;

        int total = waitingPlayers.size();
        int hunterCount = Math.max(1, total / GameManager.getInstance().getHuntersPerPlayers());
        List<UUID> shuffled = new ArrayList<>(waitingPlayers);
        Collections.shuffle(shuffled);
        waitingPlayers.clear();
        readyPlayers.clear();

        for (int i = 0; i < total; i++) {
            Player p = Bukkit.getPlayer(shuffled.get(i));
            if (p == null) continue;

            Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
            Team hiderTeam = board.registerNewTeam("hiders");
            Team hunterTeam = board.registerNewTeam("hunters");
            hiderTeam.setAllowFriendlyFire(false);
            hunterTeam.setAllowFriendlyFire(true);
            Objective obj = board.registerNewObjective("game", "dummy",
                    lang.getMessage(p, "scoreboard.title.ingame"));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            p.setScoreboard(board);

            if (i < hunterCount) {
                hunters.add(p.getUniqueId());
                unreleasedHunters.add(p.getUniqueId());
                p.sendMessage(lang.getMessage(p, "hunter.selected"));
                Location loc = GameManager.getInstance().getHunterSpawn(regionName);
                if (loc != null && loc.getWorld() != null) {
                    p.teleport(loc);
                } else {
                    p.sendMessage(lang.getMessage(p, "game.spawn.missing", "hunter"));
                }
                GameManager.getInstance().giveHunterKit(p);
                p.sendMessage(lang.getMessage(p, "hunter.delay", GameManager.HUNTER_DELAY_SECONDS));
            } else {
                hiders.add(p.getUniqueId());
                p.sendMessage(lang.getMessage(p, "hider.selected"));
                Location loc = GameManager.getInstance().getHiderSpawn(regionName);
                if (loc != null && loc.getWorld() != null) {
                    p.teleport(loc);
                } else {
                    p.sendMessage(lang.getMessage(p, "game.spawn.missing", "hider"));
                }
                GameManager.giveItem(p);
            }
            p.setGameMode(GameMode.ADVENTURE);
        }

        updateAllGameScoreboards();
        startTimer();

        hunterDelayTaskId = Bukkit.getScheduler().scheduleSyncDelayedTask(HideAndSeek.getInstance(), () -> {
            for (UUID uuid : hunters) {
                unreleasedHunters.remove(uuid);
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    p.sendMessage(lang.getMessage(p, "hunter.released"));
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                }
            }
        }, GameManager.HUNTER_DELAY_SECONDS * 20L);
    }

    // ==================== 猎人释放状态 ====================
    public boolean isHunterReleased(Player player) {
        return !unreleasedHunters.contains(player.getUniqueId());
    }

    // ==================== 游戏计时 ====================
    private void updateGameScoreboardForPlayer(Player player) {
        Scoreboard board = player.getScoreboard();
        Objective obj = board.getObjective(DisplaySlot.SIDEBAR);
        if (obj == null) return;
        for (String entry : board.getEntries()) {
            board.resetScores(entry);
        }
        obj.getScore(lang.getMessage(player, "scoreboard.ingame.time", formatTime(gameTime))).setScore(3);
        obj.getScore(lang.getMessage(player, "scoreboard.ingame.hiders", getActiveHiders())).setScore(2);
        obj.getScore(lang.getMessage(player, "scoreboard.ingame.hunters", hunters.size())).setScore(1);
    }

    private void updateAllGameScoreboards() {
        Set<UUID> allPlayers = new HashSet<>();
        allPlayers.addAll(hiders);
        allPlayers.addAll(hunters);
        allPlayers.addAll(spectators);
        for (UUID uuid : allPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                updateGameScoreboardForPlayer(p);
            }
        }
    }

    private String formatTime(int seconds) {
        int mins = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    private void startTimer() {
        timerTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(HideAndSeek.getInstance(), new Runnable() {
            int timeLeft = gameTime;
            @Override
            public void run() {
                if (state != GameState.ACTIVE) return;
                timeLeft--;
                updateAllGameScoreboards();
                if (timeLeft <= 0) {
                    endGameByTime();
                }
            }
        }, 0L, 20L);
    }

    private void endGameByTime() {
        if (getActiveHiders() > 0) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(lang.getMessage(p, "game.end.hider_win"));
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
        } else {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(lang.getMessage(p, "game.end.hunter_win"));
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
        }
        endGame();
    }

    // ==================== 游戏结束 ====================
    public void endGame() {
        if (timerTaskId != -1) Bukkit.getScheduler().cancelTask(timerTaskId);
        if (startCountdownTaskId != -1) Bukkit.getScheduler().cancelTask(startCountdownTaskId);
        if (hunterDelayTaskId != -1) Bukkit.getScheduler().cancelTask(hunterDelayTaskId);
        for (int taskId : offlineTaskIds.values()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        offlineTaskIds.clear();
        state = GameState.ENDED;

        for (UUID uuid : hiders) resetPlayer(uuid);
        for (UUID uuid : hunters) resetPlayer(uuid);
        for (UUID uuid : spectators) resetPlayer(uuid);

        hiders.clear();
        hunters.clear();
        spectators.clear();
        unreleasedHunters.clear();
        offlinePlayers.clear();

        GameManager.getInstance().removeGame(regionName);
    }

    private void resetPlayer(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        if (p == null) return;
        Location lobby = GameManager.getInstance().getLobbyLocation();
        if (lobby != null && lobby.getWorld() != null) {
            p.teleport(lobby);
        } else {
            p.sendMessage(lang.getMessage(p, "game.waiting.lobby_not_set"));
        }
        p.setGameMode(GameMode.SURVIVAL);
        p.getInventory().clear();
        p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        p.removePotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY);
        GameManager.getInstance().restorePlayerData(p);
    }

    // ==================== 事件处理 ====================
    public void onPlayerDeath(Player player) {
        UUID uuid = player.getUniqueId();
        if (hiders.remove(uuid) || hunters.remove(uuid)) {
            spectators.add(uuid);
            CombinedDisguise disguise = HideAndSeek.getInstance().getDisguise();
            disguise.removeDisguise(player);
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(lang.getMessage(player, "spectator.death"));
            updateAllGameScoreboards();

            if (getActiveHiders() == 0) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage(lang.getMessage(p, "game.end.hunter_win"));
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                }
                endGame();
            } else if (getActiveHunters() == 0) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage(lang.getMessage(p, "game.end.hider_win"));
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                }
                endGame();
            }
        }
    }

    public void onPlayerQuit(Player player) {
        UUID uuid = player.getUniqueId();
        if (waitingPlayers.contains(uuid)) {
            waitingPlayers.remove(uuid);
            readyPlayers.remove(uuid);
            GameManager.getInstance().restorePlayerData(player);
            updateAllWaitingScoreboards();
            return;
        }

        if (hiders.contains(uuid) || hunters.contains(uuid) || spectators.contains(uuid)) {
            offlinePlayers.put(uuid, System.currentTimeMillis());
            int timeoutSeconds = GameManager.getInstance().getOfflineTimeoutSeconds();

            String roleKey;
            if (hiders.contains(uuid)) roleKey = "role.hider";
            else if (hunters.contains(uuid)) roleKey = "role.hunter";
            else roleKey = "role.spectator";

            for (UUID uid : getAllPlayers()) {
                Player p = Bukkit.getPlayer(uid);
                if (p != null && p.isOnline()) {
                    p.sendMessage(lang.getMessage(p, "offline.player_left", player.getName(), lang.getMessage(p, roleKey), timeoutSeconds));
                }
            }

            int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(HideAndSeek.getInstance(), new Runnable() {
                int secondsLeft = timeoutSeconds;
                @Override
                public void run() {
                    secondsLeft--;
                    if (secondsLeft == 10) {
                        for (UUID uid : getAllPlayers()) {
                            Player p = Bukkit.getPlayer(uid);
                            if (p != null && p.isOnline()) {
                                p.sendMessage(lang.getMessage(p, "offline.player_remind", player.getName(), secondsLeft));
                            }
                        }
                    }
                    if (secondsLeft <= 0) {
                        if (offlinePlayers.containsKey(uuid)) {
                            removePlayerCompletely(uuid);
                        }
                        Bukkit.getScheduler().cancelTask(offlineTaskIds.get(uuid));
                        offlineTaskIds.remove(uuid);
                    }
                }
            }, 0L, 20L);

            offlineTaskIds.put(uuid, taskId);
        }
    }

    public void removePlayerCompletely(UUID uuid) {
        Integer taskId = offlineTaskIds.remove(uuid);
        if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);
        offlinePlayers.remove(uuid);
        boolean wasHider = hiders.remove(uuid);
        boolean wasHunter = hunters.remove(uuid);
        boolean wasSpectator = spectators.remove(uuid);
        unreleasedHunters.remove(uuid);

        if (wasHider || wasHunter || wasSpectator) {
            updateAllGameScoreboards();
            if (getActiveHiders() == 0) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage(lang.getMessage(p, "offline.player_removed_hunter_win"));
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                }
                endGame();
            } else if (getActiveHunters() == 0) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage(lang.getMessage(p, "offline.player_removed_hider_win"));
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                }
                endGame();
            }
        }
    }

    public boolean reconnectPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        if (!offlinePlayers.containsKey(uuid)) return false;

        Integer taskId = offlineTaskIds.remove(uuid);
        if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);
        offlinePlayers.remove(uuid);

        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Team hiderTeam = board.registerNewTeam("hiders");
        Team hunterTeam = board.registerNewTeam("hunters");
        hiderTeam.setAllowFriendlyFire(false);
        hunterTeam.setAllowFriendlyFire(true);
        Objective obj = board.registerNewObjective("game", "dummy",
                lang.getMessage(player, "scoreboard.title.ingame"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        player.setScoreboard(board);

        player.setGameMode(GameMode.ADVENTURE);

        if (hunters.contains(uuid)) {
            GameManager.getInstance().giveHunterKit(player);
            player.sendMessage(lang.getMessage(player, "offline.player_reconnected_role", "hunter"));
        } else if (hiders.contains(uuid)) {
            GameManager.giveItem(player);
            player.sendMessage(lang.getMessage(player, "offline.player_reconnected_role", "hider"));
        } else if (spectators.contains(uuid)) {
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(lang.getMessage(player, "offline.player_reconnected_role", "spectator"));
        }
        updateAllGameScoreboards();

        for (UUID uid : getAllPlayers()) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null && p.isOnline()) {
                p.sendMessage(lang.getMessage(p, "offline.player_reconnected", player.getName()));
            }
        }
        return true;
    }

    public void removePlayerImmediately(Player player) {
        UUID uuid = player.getUniqueId();
        boolean wasHider = hiders.remove(uuid);
        boolean wasHunter = hunters.remove(uuid);
        boolean wasSpectator = spectators.remove(uuid);

        if (wasHider || wasHunter || wasSpectator) {
            CombinedDisguise disguise = HideAndSeek.getInstance().getDisguise();
            disguise.removeDisguise(player);

            GameManager.getInstance().restorePlayerData(player);
            Location lobby = GameManager.getInstance().getLobbyLocation();
            if (lobby != null && lobby.getWorld() != null) {
                player.teleport(lobby);
            }
            player.setGameMode(GameMode.SURVIVAL);
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            player.sendMessage(lang.getMessage(player, "cmd.leave.success"));

            updateAllGameScoreboards();

            if (getActiveHiders() == 0) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage(lang.getMessage(p, "game.end.hunter_win"));
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                }
                endGame();
            } else if (getActiveHunters() == 0) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage(lang.getMessage(p, "game.end.hider_win"));
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                }
                endGame();
            }
        }
    }

    private Set<UUID> getAllPlayers() {
        Set<UUID> all = new HashSet<>();
        all.addAll(waitingPlayers);
        all.addAll(hiders);
        all.addAll(hunters);
        all.addAll(spectators);
        return all;
    }

    public int getOnlineHiders() {
        int count = 0;
        for (UUID uuid : hiders) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) count++;
        }
        return count;
    }

    public int getActiveHiders() {
        int count = 0;
        for (UUID uuid : hiders) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                count++;
            } else if (offlinePlayers.containsKey(uuid)) {
                count++;
            }
        }
        return count;
    }

    public int getActiveHunters() {
        int count = 0;
        for (UUID uuid : hunters) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                count++;
            } else if (offlinePlayers.containsKey(uuid)) {
                count++;
            }
        }
        return count;
    }

    public String getRegionName() { return regionName; }
    public GameState getState() { return state; }
    public boolean containsPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        return waitingPlayers.contains(uuid) || hiders.contains(uuid) || hunters.contains(uuid) || spectators.contains(uuid) || offlinePlayers.containsKey(uuid);
    }

    public boolean canStart() {
        return waitingPlayers.size() >= GameManager.getInstance().getMinPlayers();
    }
}