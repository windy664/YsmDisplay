package org.windy.ysmDisplay;

import me.clip.placeholderapi.PlaceholderHook;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class YdpUsePlaceholder extends PlaceholderExpansion {

    // 重写 onPlaceholderRequest 方法来处理占位符
    @Override
    public String getAuthor() {
        return "YourName";  // 填入你的名字或插件作者名
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
        // 如果 player 为 null，返回默认值
        if (params.equalsIgnoreCase("use")) {
            // 你可以在这里定义根据玩家信息或其他条件返回的内容
            return "Some dynamic value";  // 这里返回你希望的字符串内容
        }
        return null;  // 如果占位符没有匹配到，返回 null
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        // 这里可以处理玩家在线时的占位符请求
        if (params.equalsIgnoreCase("use")) {
            // 你可以根据玩家的状态、数据或其他信息返回内容
            return "Usage info for player " + player.getName();  // 例如返回玩家相关的信息
        }
        return null;  // 如果占位符没有匹配到，返回 null
    }
}


