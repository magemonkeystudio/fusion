//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package studio.magemonkey.fusion.cfg;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.util.FileUT;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

@SuppressWarnings("unused")
public class YamlParser extends YamlConfiguration implements IValuesReloadable {

    private static final Logger  logger = Fusion.getInstance().getLogger();
    private final        File    file;
    private              boolean isChanged;

    private static final List<IValuesReloadable> valuesReloadables = new LinkedList<>();

    public YamlParser(@NotNull File file) {
        this.isChanged = false;
        FileUT.create(file);
        this.file = file;
        valuesReloadables.add(this);
        reload();
    }

    public @NotNull File getFile() {
        return this.file;
    }

    public void save() {
        try {
            this.save(this.file);
        } catch (IOException var2) {
            logger.warning("Could not save config: " + this.file.getName());
        }

    }

    public void set(@NotNull String path, @Nullable Object value) {
        super.set(path, value);
        isChanged = true;
    }

    public void saveChanges() {
        if (this.isChanged) {
            this.save();
            this.isChanged = false;
        }
    }

    public void reload() {
        try {
            this.load(this.file);
            this.isChanged = false;
        } catch (IOException | InvalidConfigurationException var3) {
            logger.warning("The reload went wrong: " + var3.getMessage());
        }

    }

    public static FileConfiguration getDefaultConfig(String filePath) {
        Reader fixReader = new InputStreamReader(Objects.requireNonNull(Fusion.getInstance().getResource(filePath)));
        return YamlConfiguration.loadConfiguration(fixReader);
    }

    public static @NotNull YamlParser loadOrExtract(JavaPlugin plugin, @NotNull String filePath) {
        if (!plugin.getDataFolder().exists()) {
            FileUT.mkdir(plugin.getDataFolder());
        }

        if (!filePath.startsWith("/")) {
            filePath = "/" + filePath;
        }

        File var10002 = plugin.getDataFolder();
        File file     = new File(var10002 + filePath);
        if (!file.exists()) {
            FileUT.create(file);

            try {
                Bukkit.getConsoleSender().sendMessage("Extracting default configuration from resource: " + filePath);
                InputStream input = plugin.getClass().getResourceAsStream(filePath);
                if (input != null) {
                    logger.info("Extracting default configuration from resource: " + filePath);
                    FileUT.copy(input, file);
                } else {
                    logger.warning("Failed to find resource: " + filePath);
                }
            } catch (Exception var4) {
                logger.warning("The loading or extraction went wrong: " + var4.getMessage());
            }
        }

        return new YamlParser(file);
    }

    public void addMissing(@NotNull String path, @Nullable Object val) {
        if (!this.contains(path)) {
            this.set(path, val);
            isChanged = true;
        }
    }

    public boolean remove(@NotNull String path) {
        if (!this.contains(path)) {
            return false;
        } else {
            this.set(path, null);
            return true;
        }
    }

    public @NotNull Set<String> getSection(@NotNull String path) {
        ConfigurationSection section = this.getConfigurationSection(path);
        return section == null ? Collections.emptySet() : section.getKeys(false);
    }

    public String getString(@NotNull String path) {
        if (!isSet(path)) return "";
        String str = super.getString(path);
        return str != null && !str.isEmpty() ? str.replace("&", "§") : "";
    }

    public String getString(@NotNull String path, @Nullable String def) {
        return Objects.requireNonNull(super.getString(path, def));
    }

    public @NotNull List<String> getStringList(@NotNull String path) {
        if (!isSet(path)) return List.of();
        return super.getStringList(path);
    }

    public List<String> getStringList(@NotNull String path, List<String> def) {
        if (!isSet(path)) return def;
        return super.getStringList(path);
    }

    public Map<Enchantment, Integer> getEnchantmentSection(@NotNull String path) {
        Map<Enchantment, Integer> entries = new HashMap<>();
        ConfigurationSection      section = getConfigurationSection(path);
        if (section != null) {
            for (String key : section.getKeys(false)) {
                Enchantment ench = Enchantment.getByKey(NamespacedKey.fromString(key));
                if (ench == null) continue;
                entries.put(ench, getInt(path + "." + key, 1));
            }
        }
        return entries;
    }

    public List<ItemFlag> getItemFlags(@NotNull String path) {
        List<ItemFlag>       entries = new ArrayList<>();
        ConfigurationSection section = getConfigurationSection(path);
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ItemFlag flag = ItemFlag.valueOf(key.toUpperCase());
                entries.add(flag);
            }
        }
        return entries;
    }

    @Override
    public String getConfig() {
        return file.getName();
    }

    @Override
    public void reloadValues() {
        reload();
    }

    public static void reload(String config) {
        for (IValuesReloadable reloadable : valuesReloadables)
            if (reloadable.getConfig().equals(config)) {
                reloadable.reloadValues();
                return;
            }
    }

    public static void reloadAll(boolean message) {
        if (message)
            Bukkit.getConsoleSender().sendMessage("Reloading Configuration..");
        for (IValuesReloadable reloadable : valuesReloadables) {
            reloadable.reloadValues();
        }
    }

    public static List<String> getConfigNames() {
        List<String> entries = new ArrayList<>();
        for (IValuesReloadable reloadable : valuesReloadables)
            entries.add(reloadable.getConfig());
        return entries;
    }
}
