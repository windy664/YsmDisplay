package org.windy.ysmDisplay;

import de.tr7zw.nbtapi.NBTFile;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
public final class YsmDisplay extends JavaPlugin implements Listener {
    private String only_player;
    private String not_found_model;
    private String not_format;
    private String error_file;
    private String not_permission;


    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);

        getCommand("ydp").setTabCompleter(this);

        this.getServer().getConsoleSender().sendMessage(Texts.logo);
        this.getServer().getConsoleSender().sendMessage(Texts.info);


        loadconfig();
        if (Bukkit.getPluginManager().getPlugin("NBTAPI") != null) {
            this.getServer().getConsoleSender().sendMessage("检测到NBTAPI，已依赖！");
        }
    }

    private void loadconfig(){
        only_player = getConfig().getString("only_player","仅玩家可使用");
        not_found_model = getConfig().getString("not_found_model","未找到模型");
        not_format = getConfig().getString("not_format","无效的格式");
        not_permission = getConfig().getString("not_permission","无权限");
        error_file = getConfig().getString("error_file","错误的文件");

    }

    @Override
    public void onDisable() {
        this.getServer().getConsoleSender().sendMessage(Texts.logo);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("ydp")) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("export")) {
                    exportYsmFiles(sender);
                    return true;
                } else if (args[0].equalsIgnoreCase("load")) {
                    loadTextures(sender);
                    return true;
                } else if (args[0].equalsIgnoreCase("player")) {
                    if (sender instanceof Player) {
                        readPlayerData((Player) sender);
                    } else {
                        sender.sendMessage(only_player);
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("display") && args.length >= 4) {
                    if (sender.hasPermission("ysmdisplay.display")) { // 假设我们有一个权限节点
                        Player targetPlayer = Bukkit.getPlayer(args[1]);
                        if (targetPlayer != null) {
                            String modelId = args[2];
                            String timeArg = args[3];
                            long duration = parseTime(timeArg); // 解析时间参数
                            if (duration > 0) {
                                String file_model = modelId + ".ysm";
                                String texture = getFirstTexture(file_model); // 从文件中获取纹理
                                if (texture != null) {
                                    // 设置模型
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ysm model set " + args[1] + " " + modelId + " " + texture + " true");

                                    // 安排定时任务，在指定时间后取消模型
                                    scheduleModelReset(targetPlayer, modelId, duration);
                                    sender.sendMessage("已为玩家" + targetPlayer.getName() + "体验模型" + modelId + "，到期：" + timeArg + ".");
                                } else {
                                    sender.sendMessage(not_found_model + modelId + ".");
                                }
                            } else {
                                sender.sendMessage(not_format);
                            }
                        } else {
                            sender.sendMessage( args[1] + "disonline");
                        }
                    } else {
                        sender.sendMessage(not_permission);
                    }
                    return true;
                }
            }
        }
        return false;
    }
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("ydp")) {
            List<String> completions = new ArrayList<>();

            if (args.length == 1) {
                // 提供子命令的补全
                completions.addAll(Arrays.asList("export", "load", "player", "display"));
            } else if (args.length == 2 && args[0].equalsIgnoreCase("display")) {
                // 提供在线玩家名称的补全
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            } else if (args.length == 3 && args[0].equalsIgnoreCase("display")) {
                // 提供模型ID的补全
                File exportDir = new File(getDataFolder().getParentFile().getParent(), "config/yes_steve_model/export");
                if (exportDir.exists()) {
                    for (File file : exportDir.listFiles()) {
                        if (file.isFile() && file.getName().endsWith(".ysm")) {
                            completions.add(file.getName().replace(".ysm", ""));
                        }
                    }
                }
            } else if (args.length == 4 && args[0].equalsIgnoreCase("display")) {
                // 提供时间单位的补全
                completions.addAll(Arrays.asList("30s", "1m", "30m", "1h", "1d"));
            }

            return completions.stream()
                    .filter(completion -> completion.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
    private String getFirstTexture(String modelId) {
        File texturesFile = new File(getDataFolder().getParentFile().getParent(), "config/yes_steve_model/export/textures_output.txt");
        if (texturesFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(texturesFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith(modelId + ": ")) {
                        return line.split(": ")[1];
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    // 安排模型重置
    private void scheduleModelReset(Player player, String modelId, long duration) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            // 重置模型
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ysm model set " + player.getName() + " default default true");
        }, duration * 20); // 转换为游戏刻
    }

    private void exportYsmFiles(CommandSender sender) {
        File pluginDir = getDataFolder().getParentFile();
        File configDir = new File(pluginDir.getParentFile(), "config/yes_steve_model/auth");

        String[] ysmFiles = configDir.list((dir, name) -> name.endsWith(".ysm"));

        if (ysmFiles != null) {
            for (String fileName : ysmFiles) {
                String nameWithoutExtension = fileName.substring(0, fileName.length() - 4);
                Bukkit.getScheduler().runTask(this, () -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ysm export " + nameWithoutExtension);
                  //  sender.sendMessage("Executed: /ysm export " + nameWithoutExtension);
                });
            }
        } else {
            sender.sendMessage(not_found_model);
        }
    }

    private void loadTextures(CommandSender sender) {
        File exportDir = new File(getDataFolder().getParentFile().getParent(), "config/yes_steve_model/export");
        File[] ysmFiles = exportDir.listFiles((dir, name) -> name.endsWith(".ysm"));

        List<String> textureList = new ArrayList<>();

        if (ysmFiles != null) {
            for (File ysmFile : ysmFiles) {
                String fileName = ysmFile.getName();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(ysmFile), "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("<texture>")) {
                            String texture = line.split(" ")[1]; // 获取<texture>后面的内容
                            textureList.add(fileName + ": " + texture);
                        }
                    }
                } catch (IOException e) {
                    sender.sendMessage(error_file + fileName);
                    getLogger().severe(error_file + fileName);
                    e.printStackTrace();
                }
            }

            // 写入到TXT文件
            try {
                Path outputPath = Paths.get(exportDir.getAbsolutePath(), "textures_output.txt");
                Files.write(outputPath, textureList);
            //    sender.sendMessage("Textures exported to: " + outputPath);
            } catch (IOException e) {
                sender.sendMessage(error_file);
                getLogger().severe(error_file);
                e.printStackTrace();
            }
        } else {
            sender.sendMessage(not_found_model);
        }
    }
    private void readPlayerData(Player player) {
        String playerUUID = player.getUniqueId().toString();
        File uuidFile = new File(getDataFolder().getParentFile().getParent(), "earth/playerdata/" + playerUUID + ".dat");

        getLogger().info("尝试读取 " + uuidFile.getAbsolutePath());

        if (uuidFile.exists()) {
            try {
                NBTFile nbtFile = new NBTFile(uuidFile);
                getLogger().info("成功加载");

                // 输出整个 NBT 数据以便调试
                String nbtContent = nbtFile.toString();
                getLogger().info("内容: " + nbtContent);

                // 尝试提取 model_id 和 select_texture
                String modelId = extractValue(nbtContent, "yes_steve_model:model_id", "model_id");
                String selectTexture = extractValue(nbtContent, "yes_steve_model:model_id", "select_texture");

                if (modelId != null && selectTexture != null) {
                    player.sendMessage("你的模型ID " + modelId + ", 贴图选择 " + selectTexture);
                } else {
                    player.sendMessage("Failed to extract model id or select texture.");
                    getLogger().warning("Failed to extract model_id or select_texture for UUID: " + playerUUID);
                }
            } catch (Exception e) {
                player.sendMessage("Error reading your player file.");
                getLogger().severe("Error reading player file for UUID: " + playerUUID);
                getLogger().severe("Exception: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            player.sendMessage("Your player data file does not exist.");
            getLogger().warning("Player data file does not exist for UUID: " + playerUUID);
        }
    }

    private String extractValue(String text, String key, String subKey) {
        int startIndex = text.indexOf(key);
        if (startIndex == -1) {
            getLogger().warning("Key not found: " + key);
            return null;
        }

        // 找到结束位置
        int endIndex = text.indexOf("}", startIndex);
        if (endIndex == -1) {
            getLogger().warning("End of compound not found for key: " + key);
            return null;
        }

        // 截取该部分文本
        String compound = text.substring(startIndex, endIndex + 1);
        getLogger().info("Extracted compound: " + compound);

        // 提取 model_id 和 select_texture
        String modelId = extractSingleValue(compound, "model_id");
        String selectTexture = extractSingleValue(compound, "select_texture");

        if (modelId == null) {
            getLogger().warning("model_id not found in compound.");
        }
        if (selectTexture == null) {
            getLogger().warning("select_texture not found in compound.");
        }

        return modelId != null ? modelId : selectTexture;
    }

    private String extractSingleValue(String compound, String key) {
        String searchString = key + ":";
        int startIndex = compound.indexOf(searchString);
        if (startIndex == -1) {
            return null;
        }

        startIndex += searchString.length();
        int endIndex = compound.indexOf(",", startIndex);
        if (endIndex == -1) {
            endIndex = compound.indexOf("}", startIndex);
        }

        String value = compound.substring(startIndex, endIndex).trim().replace("\"", "");
        return value;
    }
    // 解析时间参数
// 解析时间参数
    private long parseTime(String time) {
        long multiplier = 1; // 默认为秒
        if (time.endsWith("s")) {
            multiplier = 1;
        } else if (time.endsWith("m")) {
            multiplier = 60; // 分钟转秒
        } else if (time.endsWith("h")) {
            multiplier = 60 * 60; // 小时转秒
        } else if (time.endsWith("d")) {
            multiplier = 24 * 60 * 60; // 天数转秒
        } else {
            return -1; // 无效的时间格式
        }
        // 移除单位并解析数字
        long value = Long.parseLong(time.substring(0, time.length() - 1));
        return value * multiplier;
    }
}
