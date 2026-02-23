Minecraft-HideAndSeek
A fun hide-and-seek minigame plugin for Minecraft 1.20.1, supporting Bukkit and hybrid servers (e.g., Mohist).
一款为 Minecraft 1.20.1 服务器设计的趣味躲猫猫小游戏插件，支持 Bukkit 及 Mohist 等混合端。

https://img.shields.io/github/v/release/MChaha165/Minecraft-HideAndSeek
https://img.shields.io/github/license/MChaha165/Minecraft-HideAndSeek

Developed with AI assistance. 
本插件使用 AI 辅助开发。


<h1 style="font-size: 3em;"> 📖 English</h1>

Features
Players use a carrot on a stick to randomly disguise as blocks within defined regions.

Customizable game regions with automatic block scanning.

Press F to toggle between movement mode (armor stand) and frozen mode (falling block).

Hunters have a delay before they can start moving.

Built‑in scoreboard displaying remaining time, hiders, and hunters.

Multiple regions can run games simultaneously.

Disconnect & reconnect system: players who disconnect have a configurable time to rejoin before being removed.

Item saving: when joining a game, all items are saved and cleared; they are restored upon leaving.

Multi‑language support (Chinese / English) – automatically switches based on client locale.

Dynamic scoreboard titles – translated into the player's language.

Commands
Command	Description	Permission
/hs join <region>	Join the waiting queue of a region	Everyone
/hs ready	Mark yourself ready	Everyone
/hs leave	Leave the current game (returns items, teleports to lobby)	Everyone
/hs startgame <region>	Force start a game in the region (OP only)	OP
/hs setlobby	Set the waiting lobby location	OP
/hs addspawn <region> <hider/hunter>	Add a spawn point for the given region	OP
/hs region ...	Region management (see below)	OP
/hs give <player>	Give a disguise item (carrot on a stick) to a player	OP
/hs testblock <block>	Test disguise with a specific block	OP
/hs undisguise [player]	Remove disguise from yourself or another player	OP
/hs cleanup	Remove all leftover disguise entities	OP
/hs help	Show this help message	Everyone
Region Management Subcommands
Command	Description
/hs region wand	Get the golden axe selection tool
/hs region create <name>	Create a region using the two selected points
/hs region list	List all defined regions
/hs region info <name>	Show details of a region
/hs region scan <name>	Asynchronously scan the region for usable blocks
/hs region setblocks <name> <block list>	Manually set the block list (comma‑separated, e.g. STONE,GRASS_BLOCK)
/hs region remove <name>	Delete a region
Installation
Download the latest HideAndSeek-*.jar from the Releases page.

Place the jar file into your server's plugins folder.

Restart the server.

Configuration
On first start the plugin generates config.yml. You can adjust:

yaml
# GAME-SETTINGS
game-settings:
  hunters-per-players: 4   # how many players per hunter (e.g. 4 → 1 hunter for every 4 players)
  min-players: 2           # minimum players required to start
  default-game-time: 300   # game duration in seconds

# offline timeout (seconds) – players have this time to reconnect before being removed
offline-timeout-seconds: 60

# lobby location (set with /hs setlobby)
lobby: {}

# regions (created with /hs region commands)
regions: {}
Spawn points, scanned blocks, and the lobby are saved automatically.

Terms of Use
✅ Allowed
Personal learning and research.

Free use on personal (non‑commercial) servers.

Reporting issues and suggesting improvements.

❌ Not Allowed
Commercial use without a license.

Redistribution, reselling, or claiming as your own.

Removing or altering the copyright notice.

💼 Commercial License
If you wish to use this plugin on a commercial server, please contact the author for a license:

Email: 1654199310@qq.com

Author
MChaha165

GitHub: MChaha165

Project: https://github.com/MChaha165/Minecraft-HideAndSeek

Bilibili：https://space.bilibili.com/3546665524922590

爱发电: https://afdian.com/a/MChaha165

<h1 style="font-size: 3em;"> 🇨🇳 中文介绍</h1>

功能特点
玩家使用胡萝卜钓竿随机变成区域内的方块。

支持自定义游戏区域，自动扫描可用方块。

按 F 键切换移动模式（盔甲架）与冻结模式（下落方块）。

猎人需等待延迟时间后才能行动。

内置计分板显示剩余时间、躲藏者与猎人数量。

支持多区域同时进行游戏。

掉线重连系统：玩家掉线后可在配置时间内重新加入，超时则视为退出。

物品暂存：加入游戏时自动保存并清空背包，离开时归还。

多语言支持（中文/英文）——根据客户端语言自动切换。

动态计分板标题——随玩家语言翻译。

命令列表
命令	说明	权限
/hs join <区域>	加入指定区域的等待队列	所有人
/hs ready	标记自己准备就绪	所有人
/hs leave	离开当前游戏（归还物品，传送回大厅）	所有人
/hs startgame <区域>	强制开始游戏（仅限 OP）	OP
/hs setlobby	设置等待大厅位置	OP
/hs addspawn <区域> <hider/hunter>	为区域添加出生点	OP
/hs region ...	区域管理（见下文）	OP
/hs give <玩家>	给玩家发放变身道具（胡萝卜钓竿）	OP
/hs testblock <方块>	测试指定方块的伪装	OP
/hs undisguise [玩家]	解除自己或他人的伪装	OP
/hs cleanup	清除残留的伪装实体	OP
/hs help	显示帮助信息	所有人
区域管理子命令
命令	说明
/hs region wand	获取选区工具（金斧）
/hs region create <名称>	用已选的两点创建区域
/hs region list	列出所有已定义区域
/hs region info <名称>	查看区域详细信息
/hs region scan <名称>	异步扫描区域内的方块
/hs region setblocks <名称> <方块列表>	手动设置伪装方块（英文名逗号分隔）
/hs region remove <名称>	删除区域
安装方法
从 Releases 下载最新的 HideAndSeek-*.jar。

将 jar 文件放入服务器的 plugins 文件夹。

重启服务器。

配置文件
首次启动会自动生成 config.yml，你可以调整：

yaml
# 游戏通用设置
game-settings:
  hunters-per-players: 4   # 每多少个玩家出1个猎人（向下取整，至少1）
  min-players: 2           # 最少需要多少玩家才能开始游戏
  default-game-time: 300   # 默认游戏时长（秒），每个区域可单独覆盖

# 离线超时时间（秒），玩家掉线后等待重连的时间，超时后视为退出游戏
offline-timeout-seconds: 60

# 等待大厅坐标（需用 /hs setlobby 设置）
lobby: {}

# 区域配置（通过 /hs region 命令生成）
regions: {}
出生点、扫描的方块和大厅位置会自动保存。

使用条款
✅ 允许的用途
个人学习、研究。

个人服务器的免费使用（非商业性质）。

提出改进建议、报告 Bug。

❌ 禁止的用途
未经授权，禁止在任何商业服务器中使用本插件。

禁止将本插件或其修改版本用于任何盈利目的。

禁止二次分发、转售本插件。

禁止删除或修改代码中的版权声明。

💼 商业授权
如果你希望在商业服务器中使用本插件，请联系作者获取授权：

邮箱：1654199310@qq.com

作者
MChaha165

GitHub：MChaha165

项目地址：https://github.com/MChaha165/Minecraft-HideAndSeek

哔哩哔哩：https://space.bilibili.com/3546665524922590

爱发电：https://afdian.com/a/MChaha165

📄 License
This project is protected by copyright law. All rights reserved.
本代码受著作权法保护，保留所有权利。未经作者书面许可，不得用于商业用途。

