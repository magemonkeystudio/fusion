package com.gotofinal.darkrise.crafting.cfg;

import com.gotofinal.darkrise.crafting.cfg.PlayerConfig;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class PConfigManager {

    private static HashMap<UUID, PlayerConfig> pConfig = new HashMap<>();

    public static boolean hasMastery(CommandSender sender, String skill) {
        Player player = (Player) sender;
        return getPlayerConfig(player).hasMastery(skill);
//        return true;
    }

    public static PlayerConfig getPlayerConfig(Player player) {
        if (!pConfig.containsKey(player.getUniqueId()))
            pConfig.put(player.getUniqueId(), new PlayerConfig(player));

        return pConfig.get(player.getUniqueId());
    }
}
