package org.windy.ysmDisplay;

import org.bukkit.Bukkit;

public class Texts {
    static String pluginVersion = YsmDisplay.getPlugin(YsmDisplay.class).getDescription().getVersion();
    static String minecraftVersion = Bukkit.getVersion();
    static String serverType = Bukkit.getServer().getName();
    public static final String logo = """

                                                                                               \s
            _|      _|  _|            _|_|_|    _|                      _|                     \s
              _|  _|          _|_|_|  _|    _|        _|_|_|  _|_|_|    _|    _|_|_|  _|    _| \s
                _|      _|  _|_|      _|    _|  _|  _|_|      _|    _|  _|  _|    _|  _|    _| \s
                _|      _|      _|_|  _|    _|  _|      _|_|  _|    _|  _|  _|    _|  _|    _| \s
                _|      _|  _|_|_|    _|_|_|    _|  _|_|_|    _|_|_|    _|    _|_|_|    _|_|_| \s
                                                              _|                            _| \s
                                                              _|                        _|_|   \s
                                                              
                        """;

    public static final String info =
            "\n"+
            " §a| 插件版本    : §f" + pluginVersion + "\n" +
            " §a| 游戏版本 : §f" + minecraftVersion + "\n" +
            " §a| 服务器核心       : §f" + serverType + "\n" +
            " §a";
}
