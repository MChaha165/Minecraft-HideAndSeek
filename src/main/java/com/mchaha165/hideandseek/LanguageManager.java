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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.logging.Level;

public class LanguageManager {
    private final HideAndSeek plugin;
    private final Map<String, ResourceBundle> bundles = new HashMap<>();
    private final String[] supportedLanguages = {"en", "zh"};

    public LanguageManager(HideAndSeek plugin) {
        this.plugin = plugin;
        initLangFiles();        // 强制复制语言文件（覆盖已有）
        loadBundles();          // 从数据文件夹加载语言文件
    }

    // ==================== 初始化语言文件（强制覆盖）====================
    private void initLangFiles() {
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }

        for (String lang : supportedLanguages) {
            File targetFile = new File(langDir, "messages_" + lang + ".properties");
            // 无论文件是否存在，都强制从 jar 包复制（覆盖）
            try (InputStream in = plugin.getResource("lang/messages_" + lang + ".properties")) {
                if (in == null) {
                    plugin.getLogger().warning("在 jar 包中未找到语言文件: lang/messages_" + lang + ".properties");
                    continue;
                }
                // 使用 BufferedReader 确保 UTF-8 读取，再用 BufferedWriter 以 UTF-8 写出
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                     BufferedWriter writer = Files.newBufferedWriter(targetFile.toPath(), StandardCharsets.UTF_8)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        writer.write(line);
                        writer.newLine();
                    }
                    plugin.getLogger().info("已强制覆盖语言文件: messages_" + lang + ".properties");
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "复制语言文件失败: " + lang, e);
            }
        }
    }

    // ==================== 加载语言文件 ====================
    private void loadBundles() {
        bundles.clear();
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) return;

        File[] files = langDir.listFiles((dir, name) -> name.startsWith("messages_") && name.endsWith(".properties"));
        if (files == null) return;

        for (File file : files) {
            String fileName = file.getName();
            String lang = fileName.substring("messages_".length(), fileName.length() - ".properties".length());
            try (FileInputStream fis = new FileInputStream(file);
                 InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
                ResourceBundle bundle = new PropertyResourceBundle(isr);
                bundles.put(lang, bundle);
                plugin.getLogger().info("已加载语言文件: " + fileName);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "加载语言文件失败: " + fileName, e);
            }
        }
    }

    // ==================== 获取消息（玩家专用）====================
    public String getMessage(Player player, String key, Object... args) {
        String lang = getPlayerLanguage(player);
        ResourceBundle bundle = bundles.getOrDefault(lang, bundles.get("en"));
        if (bundle == null) {
            return key; // 无语言文件，返回键名
        }
        if (!bundle.containsKey(key)) {
            return key; // 键不存在，返回键名
        }
        String pattern = bundle.getString(key);
        if (args.length == 0) {
            return pattern;
        }
        return MessageFormat.format(pattern, args);
    }

    // ==================== 获取玩家语言 ====================
    private String getPlayerLanguage(Player player) {
        // 优先使用 Bukkit 自带的 getLocale()（1.12+ 支持）
        try {
            String locale = player.getLocale(); // 返回如 "zh_cn"
            if (locale != null && locale.length() >= 2) {
                String lang = locale.substring(0, 2); // 取前两位，如 "zh"
                if (bundles.containsKey(lang)) {
                    return lang;
                }
            }
        } catch (Exception ignored) {
            // getLocale() 可能在某些版本不可用，忽略
        }

        // 回退：反射获取（兼容旧版）
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object locale = handle.getClass().getField("locale").get(handle);
            String loc = locale.toString().split("_")[0];
            if (bundles.containsKey(loc)) {
                return loc;
            }
        } catch (Exception ignored) {
            // 反射失败，忽略
        }

        // 默认返回英文
        return "en";
    }

    // ==================== 获取默认语言消息（用于控制台等）====================
    public String getMessage(String key, Object... args) {
        ResourceBundle bundle = bundles.get("en");
        if (bundle == null || !bundle.containsKey(key)) return key;
        String pattern = bundle.getString(key);
        return args.length == 0 ? pattern : MessageFormat.format(pattern, args);
    }
}