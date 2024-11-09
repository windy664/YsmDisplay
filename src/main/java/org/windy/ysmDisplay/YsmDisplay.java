package org.windy.ysmDisplay;

import de.tr7zw.nbtapi.NBTCompound;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTFile;
public class YsmDisplay extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("YesDisplay Plugin Enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("YesDisplay Plugin Disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;
        if (command.getName().equalsIgnoreCase("ydp")) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("export")) {
                    exportYsmFiles(sender);
                    return true;
                } else if (args[0].equalsIgnoreCase("load")) {
                    loadTextures(sender);
                    return true;
                } else if (args[0].equalsIgnoreCase("player")) {
                    readPlayerData(player);
                    return true;
                } else if (args.length >= 9 && args[0].equalsIgnoreCase("display")) {
                    if (sender.hasPermission("ysmdisplay.display")) { // 假设我们有一个权限节点
                        Player targetPlayer = Bukkit.getPlayer(args[1]);
                        if (targetPlayer != null) {
                            String modelId = args[2];
                            String timeArg = args[3];
                            long duration = parseTime(timeArg); // 解析时间参数
                            if (duration > 0) {
                                String texture = getFirstTexture(modelId); // 从文件中获取纹理
                                if (texture != null) {
                                    // 设置模型
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ysm model set " + args[1] + " " + modelId + " " + texture + " true");

                                    // 安排定时任务，在指定时间后取消模型
                                    scheduleModelReset(targetPlayer, modelId, duration);
                                    sender.sendMessage("Model set for " + targetPlayer.getName() + " with ID " + modelId + " for " + timeArg + ".");
                                } else {
                                    sender.sendMessage("Could not find the texture for the model ID " + modelId + ".");
                                }
                            } else {
                                sender.sendMessage("Invalid time format. Use 30s, 0.5m, 1h, 3d, etc.");
                            }
                        } else {
                            sender.sendMessage("Player " + args[1] + " is not online.");
                        }
                    } else {
                        sender.sendMessage("You do not have permission to use this command.");
                    }
                    return true;
                }
            }
        }
        return false;
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
                    sender.sendMessage("Executed: /ysm export " + nameWithoutExtension);
                });
            }
        } else {
            sender.sendMessage("No .ysm files found.");
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
                    sender.sendMessage("Error reading file: " + fileName);
                    getLogger().severe("Error reading file: " + fileName);
                    e.printStackTrace();
                }
            }

            // 写入到TXT文件
            try {
                Path outputPath = Paths.get(exportDir.getAbsolutePath(), "textures_output.txt");
                Files.write(outputPath, textureList);
                sender.sendMessage("Textures exported to: " + outputPath);
            } catch (IOException e) {
                sender.sendMessage("Error writing to output file.");
                getLogger().severe("Error writing to output file.");
                e.printStackTrace();
            }
        } else {
            sender.sendMessage("No .ysm files found in export directory.");
        }
    }
    private void readPlayerData(Player player) {
        String playerUUID = player.getUniqueId().toString();
        File uuidFile = new File(getDataFolder().getParentFile().getParent(), "earth/playerdata/" + playerUUID + ".dat");

        getLogger().info("Trying to read player data from: " + uuidFile.getAbsolutePath());

        if (uuidFile.exists()) {
            try {
                NBTFile nbtFile = new NBTFile(uuidFile);
                getLogger().info("Successfully loaded NBT file.");

                // 输出整个 NBT 数据以便调试
                String nbtContent = nbtFile.toString();
                getLogger().info("NBT Content: " + nbtContent);

                // 尝试提取 model_id 和 select_texture
                String modelId = extractValue(nbtContent, "yes_steve_model:model_id", "model_id");
                String selectTexture = extractValue(nbtContent, "yes_steve_model:model_id", "select_texture");

                if (modelId != null && selectTexture != null) {
                    player.sendMessage("Your model_id: " + modelId + ", select_texture: " + selectTexture);
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
