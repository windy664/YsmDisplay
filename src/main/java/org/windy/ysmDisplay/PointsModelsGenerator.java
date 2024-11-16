package org.windy.ysmDisplay;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PointsModelsGenerator {
    private static final String DATA_FILE = "data.yml";
    private static final String OUTPUT_FILE = "Points-Models.yml";

    public void generatePointsModels() throws IOException {

        File dataFile = new File(YsmDisplay.getInstance().getDataFolder(), DATA_FILE);
        if (!dataFile.exists()) {
            throw new IOException("未找到 data.yml 文件！");
        }

        YamlConfiguration dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        YamlConfiguration outputConfig = new YamlConfiguration();

        outputConfig.set("menu_title", "📦 时装商店");

        int slot = 10;
        Set<Integer> excludedSlots = new HashSet<>(Arrays.asList(17, 18, 26, 27, 35, 36)); // 排除的槽位
        int maxSlot = 43; // 最大槽位
        int currentFileIndex = 1; // 当前文件索引，开始为 1
        int nextFileSlotStart = 10; // 第二个文件的起始槽位

        File guiMenusDir = new File(Bukkit.getPluginManager().getPlugin("DeluxeMenus").getDataFolder(), "gui_menus");
        if (!guiMenusDir.exists()) {
            guiMenusDir.mkdirs(); // 创建目录，如果不存在的话
        }

        // 生成每个文件时，都要设置 'filler_item' 和 'create' 配置项
        addFillerItemAndCreate(outputConfig);

        for (String key : dataConfig.getKeys(false)) {
            // 找到下一个可用槽位，跳过被排除的槽位
            while (excludedSlots.contains(slot) || slot > maxSlot) {
                slot++;
                if (slot > maxSlot) {
                    // 如果超过最大槽位，则重置槽位并切换到下一个文件
                    slot = nextFileSlotStart;
                    nextFileSlotStart = 10; // 第二个文件起始槽位
                    // 保存当前文件并准备下一个文件
                    outputConfig.save(new File(guiMenusDir, "Points-Models-" + currentFileIndex + ".yml"));
                    outputConfig = new YamlConfiguration(); // 重置新的配置文件
                    currentFileIndex++; // 文件索引递增
                    outputConfig.set("menu_title", "📦 时装商店"); // 重新设置标题
                    // 每次生成新文件时，也需要重新设置 'filler_item' 和 'create'
                    addFillerItemAndCreate(outputConfig);
                }
            }

            // 读取并调试输出每个模型的属性
            String name = dataConfig.getString(key + ".name");
            String priceStr = dataConfig.getString(key + ".price");
            int price = 0;
            try {
                price = Integer.parseInt(priceStr);
            } catch (NumberFormatException e) {
                YsmDisplay.getInstance().getLogger().warning("价格格式错误: " + priceStr + "，使用默认值 0");
            }

            // 输出调试信息
            YsmDisplay.getInstance().getLogger().info("读取模型: " + key + " 名称: " + name + " 价格: " + price);

            String itemPath = "items." + key;
            outputConfig.set(itemPath + ".material", "kubejs_item73");
            outputConfig.set(itemPath + ".display_name", name);
            outputConfig.set(itemPath + ".slot", slot);
            outputConfig.set(itemPath + ".lore", List.of(
                    " ",
                    "&8• &7价格: &f" + price + " &c点券",
                    " ",
                    "&8| &f➢ 左键 | 购买",
                    "&8| &f➢ 右键 | 预览",
                    ""
            ));
            outputConfig.set(itemPath + ".left_click_commands", List.of(
                    "[console] points take %player_name% " + price,
                    "[console] ysm auth %player_name% add " + key,
                    "[message] 成功购买！快按下Alt+Y领略其中之美吧！"
            ));
            outputConfig.set(itemPath + ".right_click_commands", List.of(
                    "[console] ysmdisplay:ydp display %player_name% " + key + " 5m",
                    "[message] &3时装&b中心 &f➢ 体验时装 " + name
            ));

            slot++; // 递增槽位
        }

        // 保存最后一个文件
        outputConfig.save(new File(guiMenusDir, "Points-Models-" + currentFileIndex + ".yml"));

        // 更新 config.yml
        File configFile = new File(Bukkit.getPluginManager().getPlugin("DeluxeMenus").getDataFolder(), "config.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        // 如果已经存在相同的键值对，则覆盖它
        config.set("gui_menus.Points-Models-" + currentFileIndex + ".file", "Points-Models-" + currentFileIndex + ".yml");

        // 保存 config.yml
        config.save(configFile);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "deluxemenus:deluxemenu reload");
    }

    // 提取的公共方法：每次生成新文件时都要添加 'filler_item' 和 'create' 配置项
    private void addFillerItemAndCreate(YamlConfiguration config) {
        // 添加 'filler_item' 配置
        String fillerItemPath = "items.filler_item";
        config.set(fillerItemPath + ".material", "GRAY_STAINED_GLASS_PANE");
        config.set(fillerItemPath + ".slots", List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 49, 51, 52, 53));

        // 添加 'create' 配置
        String createPath = "items.create";
        config.set(createPath + ".material", "paper");
        config.set(createPath + ".model_data", 10020);
        config.set(createPath + ".display_name", "🩹 &f返回");
        config.set(createPath + ".slot", 50);
        config.set(createPath + ".left_click_commands", List.of(
                "[openguimenu] pointstore"
        ));
    }

    private int getNextAvailableSlot(int currentSlot) {
        while (currentSlot == 17 || currentSlot == 18 || currentSlot == 26 ||
                currentSlot == 27 || currentSlot == 35 || currentSlot == 36) {
            currentSlot++;
        }
        return currentSlot;
    }
}
