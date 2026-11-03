<div align="center">

<img src="assets/banner.svg" alt="YsmDisplay" width="100%">

# YsmDisplay

**YSM 模型展示与时装商店** —— 在游戏内用分页 GUI 展示玩家的 YSM（YesSteveModel）模型，支持积分定价购买，接 DeluxeMenus/DC 菜单。

![Platform](https://img.shields.io/badge/Platform-Spigot%20%2F%20Paper-62bda8?style=flat-square&logo=minecraft&logoColor=white)
![Minecraft](https://img.shields.io/badge/MC-1.21.x-8a9a5b?style=flat-square)
![Java](https://img.shields.io/badge/Java-21-f89820?style=flat-square&logo=openjdk&logoColor=white)
![Depends](https://img.shields.io/badge/depend-NBTAPI-e2b90f?style=flat-square)

[功能](#-功能) · [命令](#-命令) · [配置](#️-配置)

</div>

## ✨ 功能

- **时装商店 GUI**：分页界面（`时装商店 #页码`），上一页/下一页/返回/公告按钮齐全，图标与槽位可配
- **模型定价**：`min-price` / `max-price` 控制模型价格区间，配合积分系统（`PointsModelsGenerator`）生成商品
- **积分指令**：`/ydp` 系列命令处理模型使用与积分占位（`YdpUsePlaceholder`）
- **生态联动**：softdepend PlaceholderAPI 与 DeluxeMenus，按钮可直接 `dc open` 跳转 DC 菜单；NBTAPI 负责 NBT 读写

## 📜 命令

| 命令 | 说明 |
|------|------|
| `/ydp test` | 测试命令 |

## ⚙️ 配置

```yaml
min-price: 90
max-price: 320
menu:
  title: "📦 时装商店 #"
  last:  { name: "⬅️ &f上一页", icon: lime_stained_glass_pane, slot: 45 }
  next:  { name: "🔜 &f下一页", icon: lime_stained_glass_pane, slot: 53 }
  back:  { name: "⬅️ &f返回", command: "dc open menu %player_name%", slot: 49 }
  announcement: { name: "🔈&f公告", slot: 48 }
```

## 🔨 构建

```bash
./gradlew build
```
