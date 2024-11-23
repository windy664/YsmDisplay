package org.windy.ysmDisplay;

import de.tr7zw.nbtapi.NBTFile;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;


public final class YsmDisplay extends JavaPlugin implements Listener {
    private String only_player;
    private String not_found_model;
    private String not_format;
    private String error_file;
    private String not_permission;
    private static YsmDisplay instance;
    private int max_price;
    private int min_price;
    private int price;
    public String title;

    @Override
    public void onEnable() {
        instance = this;
        this.saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);

        getCommand("ydp").setTabCompleter(this);

        this.getServer().getConsoleSender().sendMessage(Texts.logo);
        this.getServer().getConsoleSender().sendMessage(Texts.info);


        loadconfig();
        if (Bukkit.getPluginManager().getPlugin("NBTAPI") != null) {
            this.getServer().getConsoleSender().sendMessage("检测到NBTAPI，已依赖！");
        }
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            // 注册自定义占位符扩展
            new YdpUsePlaceholder().register();
        } else {
            getLogger().warning("PlaceholderAPI 插件未安装，无法注册占位符扩展！");
        }


        exportYsmFiles();
        loadTextures();
    }

    public void loadconfig(){
        only_player = getConfig().getString("only_player","仅玩家可使用");
        not_found_model = getConfig().getString("not_found_model","未找到模型");
        not_format = getConfig().getString("not_format","无效的格式");
        not_permission = getConfig().getString("not_permission","无权限");
        error_file = getConfig().getString("error_file","错误的文件");
        max_price = getConfig().getInt("max_price",100);
        min_price = getConfig().getInt("min_price",100);
        title = getConfig().getString("title","📦 时装商店");
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
                    exportYsmFiles();
                    return true;
                } else if (args[0].equalsIgnoreCase("load")) {
                    loadTextures();
                    return true;
                } else if (args[0].equalsIgnoreCase("player")) {
                    if (sender instanceof Player player) {
                        String modelId = YsmDisplay.getInstance().readPlayerData(player);
                        player.sendMessage("你的模型ID: " + modelId);
                    } else {
                        sender.sendMessage(only_player);
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("reload")) {
                        loadconfig();
                        sender.sendMessage("重载完成");

                }else if (args[0].equalsIgnoreCase("open")) {
                        loadconfig();
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                            "deluxemenus:deluxemenu open Points-Models-1 "+ sender.getName());
                    return true;
                } else if (args[0].equalsIgnoreCase("display") && args.length >= 4) {
                    if (sender.hasPermission("ysmdisplay.display")) { // 假设我们有一个权限节点
                        Player targetPlayer = Bukkit.getPlayer(args[1]);
                        if (targetPlayer != null) {
                            String modelId = args[2];
                            String timeArg = args[3];
                            long duration = parseTime(timeArg); // 解析时间参数
                            if (duration > 0) {
                                String fileModel = modelId + ".ysm";
                                String texture = getFirstTexture(fileModel); // 从文件中获取纹理
                                if (texture != null) {
                                    // 设置模型
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                            "ysm model set " + args[1] + " " + modelId + " " + texture + " true");

                                    // 安排定时任务，在指定时间后取消模型
                                    scheduleModelReset(targetPlayer, modelId, duration);
                                    sender.sendMessage("已为玩家 " + targetPlayer.getName() + " 体验模型 " + modelId + "，到期：" + timeArg + ".");
                                } else {
                                    sender.sendMessage(not_found_model + modelId + ".");
                                }
                            } else {
                                sender.sendMessage(not_format);
                            }
                        } else {
                            sender.sendMessage(args[1] + " 不在线.");
                        }
                    } else {
                        sender.sendMessage(not_permission);
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("gen")) { // 新增指令
                    if (sender.hasPermission("ysmdisplay.gen")) {
                        try {
                            PointsModelsGenerator generator = new PointsModelsGenerator();
                            generator.generatePointsModels();
                            sender.sendMessage("成功生成 Points-Models.yml 文件！");
                        } catch (Exception e) {
                            sender.sendMessage("生成文件时发生错误: " + e.getMessage());
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
                completions.addAll(Arrays.asList("export", "load", "player", "display","gen","reload","open"));
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
        // 获取 data.yml 文件路径
        File dataFile = new File(getDataFolder(), "data.yml");

        // 检查文件是否存在
        if (dataFile.exists()) {
            try {
                // 加载 YAML 文件
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);

                // 如果模型 ID 包含 .ysm 后缀，移除它
                if (modelId.endsWith(".ysm")) {
                    modelId = modelId.substring(0, modelId.length() - 4); // 移除 .ysm
                }

                // 获取指定模型 ID 的 Texture 值
                String texture = yaml.getString(modelId + ".Texture");

                if (texture != null) {
                    // 记录成功获取贴图路径的日志
                    getLogger().info("Texture found for model '" + modelId + "': " + texture);
                    return texture;
                } else {
                    // 记录未找到贴图路径的日志
                    getLogger().warning("No texture found for model ID: " + modelId);
                }
            } catch (Exception e) {
                // 记录异常日志
                getLogger().severe("Error reading data.yml for model ID: " + modelId);
                e.printStackTrace();
            }
        } else {
            // 记录文件不存在的日志
            getLogger().warning("data.yml file not found in plugin directory.");
        }

        // 返回 null 表示未找到贴图路径
        return null;
    }


    // 安排模型重置
    private void scheduleModelReset(Player player, String modelId, long duration) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            // 重置模型
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ysm model set " + player.getName() + " default default true");
        }, duration * 20); // 转换为游戏刻
    }

    private void exportYsmFiles() {
        File pluginDir = getDataFolder().getParentFile();
        File configDir = new File(pluginDir.getParentFile(), "config/yes_steve_model/auth");

        // 查找.ysm文件
        String[] ysmFiles = configDir.list((dir, name) -> name.endsWith(".ysm"));

        // 获取目录下的所有文件夹
        File[] folders = configDir.listFiles(File::isDirectory);

        if (ysmFiles!= null) {
            for (String fileName : ysmFiles) {
                String nameWithoutExtension = fileName.substring(0, fileName.length() - 4);
                Bukkit.getScheduler().runTask(this, () -> {
                    // 对.ysm文件去掉扩展名后的名字执行导出命令
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ysm export " + nameWithoutExtension);
                });
            }
        }

        if (folders!= null) {
            for (File folder : folders) {
                Bukkit.getScheduler().runTask(this, () -> {
                    // 对文件夹名字执行导出命令
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ysm export " + folder.getName());
                });
            }
        }
    }
/*
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
    }*/
private void loadTextures() {
    Random rand = new Random();
// 先获取 [0, max_price - min_price] 之间的随机数
    int randomValueInRange = rand.nextInt(max_price - min_price + 1);
// 再加上 min_price 得到最终在 [min_price, max_price] 之间的随机数
    price = randomValueInRange + min_price;
    // 获取 export 目录
    File exportDir = new File(getDataFolder().getParentFile().getParent(), "config/yes_steve_model/export");
    File[] ysmFiles = exportDir.listFiles((dir, name) -> name.endsWith(".ysm"));

    // 初始化一个 Map 用于存储 YAML 数据
    Map<String, Map<String, String>> textureDataMap = new HashMap<>();

    if (ysmFiles != null) {
        for (File ysmFile : ysmFiles) {
            String fileName = ysmFile.getName();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(ysmFile), "UTF-8"))) {
                String line;
                String textureName = null;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("<texture>")) {
                        textureName = line.split(" ")[1]; // 获取 <texture> 后面的内容
                        if (textureName != null) {
                            // 构造此贴图的 YAML 数据
                            Map<String, String> textureData = new HashMap<>();
                            textureData.put("name", fileName.split("\\.")[0]); // 取文件名作为 name
                            textureData.put("price", String.valueOf(price)); // 设置默认价格
                            textureData.put("lore", "简介"); // 设置默认简介
                            textureData.put("Texture", textureName); // 设置贴图路径

                            // 添加到主 Map，以文件名为键
                            textureDataMap.put(textureData.get("name"), textureData);
                        }
                    }
                }
            } catch (IOException e) {
                getLogger().severe(error_file + fileName);
                e.printStackTrace();
            }
        }

        // 写入到 plugins\YsmDisplay\data.yml
        try {
            File dataFile = new File(getDataFolder(), "data.yml");

            // 如果文件不存在，则创建它
            if (!dataFile.exists()) {
                dataFile.createNewFile();
            }

            // 使用 YamlConfiguration 管理 YAML 数据
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);

            // 将贴图数据写入到 YAML 配置中
            for (Map.Entry<String, Map<String, String>> entry : textureDataMap.entrySet()) {
                String textureKey = entry.getKey();
                Map<String, String> textureData = entry.getValue();

                // 转换每个贴图数据为 YAML 结构
                yaml.set(textureKey + ".name", textureData.get("name"));
                yaml.set(textureKey + ".price", textureData.get("price"));
                yaml.set(textureKey + ".lore", textureData.get("lore"));
                yaml.set(textureKey + ".Texture", textureData.get("Texture"));
            }

            // 保存到 data.yml 文件
            yaml.save(dataFile);


        } catch (IOException e) {

            getLogger().severe(error_file);
            e.printStackTrace();
        }
    }
}



    public static YsmDisplay getInstance() {
        return instance;
    }

    public String readPlayerData(Player player) {
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
               //     player.sendMessage("你的模型ID " + modelId + ", 贴图选择 " + selectTexture);
                    // 将 modelId 返回，供 PlaceholderAPI 使用
                    return modelId;
                } else {
                    getLogger().warning("Failed to extract model_id or select_texture for UUID: " + playerUUID);
                }
            } catch (Exception e) {
                getLogger().severe("Error reading player file for UUID: " + playerUUID);
                getLogger().severe("Exception: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            getLogger().warning("Player data file does not exist for UUID: " + playerUUID);
        }
        return null;  // 如果没有提取到数据，返回 null
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
