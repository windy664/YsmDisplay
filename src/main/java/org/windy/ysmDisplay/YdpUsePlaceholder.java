package org.windy.ysmDisplay;

import me.clip.placeholderapi.PlaceholderHook;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class YdpUsePlaceholder extends PlaceholderExpansion {

    // 重写 onPlaceholderRequest 方法来处理占位符
    @Override
    public String getAuthor() {
        return "windy";  // 填入你的名字或插件作者名
    }

    @Override
    public String getIdentifier() {
        return "ydp";  // 这是占位符的标识符部分
    }

    @Override
    public String getVersion() {
        return "1.0.0";  // 填入你的插件版本
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player.isOnline() && params.equalsIgnoreCase("use")) {
            // 只处理在线玩家的请求
            String modelId = YsmDisplay.getInstance().readPlayerData((Player) player);
            YsmDisplay.getInstance().getLogger().info(modelId);
            if (modelId != null) {
                return modelId;  // 返回提取到的 modelId
            } else {
                return "No model ID found";  // 如果没有找到 modelId
            }
        }
        return null;  // 如果没有匹配到，占位符将返回 null
    }
}


