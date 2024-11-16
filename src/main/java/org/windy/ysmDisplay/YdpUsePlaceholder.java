package org.windy.ysmDisplay;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;

public class YdpUsePlaceholder extends PlaceholderExpansion {

    @Override
    public String getAuthor() {
        return "windy"; // 填入你的名字或插件作者名
    }

    @Override
    public String getIdentifier() {
        return "ydp"; // 这是占位符的标识符部分
    }

    @Override
    public String getVersion() {
        return "1.0.0"; // 填入你的插件版本
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player.isOnline()) {
            if (params.equalsIgnoreCase("use")) {
                // 获取正在使用的模型 ID
                String modelId = YsmDisplay.getInstance().readPlayerData((Player) player);
                YsmDisplay.getInstance().getLogger().info("Model ID for %ydp_use%: " + modelId);
                return modelId != null ? modelId : "No model ID found";
            } else if (params.equalsIgnoreCase("use_name")) {
                // 获取正在使用的模型 ID
                String modelId = YsmDisplay.getInstance().readPlayerData((Player) player);

                if (modelId != null) {
                    // 移除 .ysm 后缀（如果有）
                    if (modelId.endsWith(".ysm")) {
                        modelId = modelId.substring(0, modelId.length() - 4);
                    }

                    // 加载 data.yml 文件
                    File dataFile = new File(YsmDisplay.getInstance().getDataFolder(), "data.yml");
                    if (dataFile.exists()) {
                        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);

                        // 获取对应的 name
                        String name = yaml.getString(modelId + ".name");
                        if (name != null) {
                            YsmDisplay.getInstance().getLogger().info("Name for %ydp_use_name%: " + name);
                            return name; // 返回对应的名称
                        } else {
                            YsmDisplay.getInstance().getLogger().warning("No name found for model ID: " + modelId);
                        }
                    } else {
                        YsmDisplay.getInstance().getLogger().warning("data.yml not found in plugin directory.");
                    }
                }
                return "No model ID found";
            }
        }
        return null; // 没有匹配到任何请求
    }
}
