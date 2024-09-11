package studio.magemonkey.fusion;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.data.player.FusionPlayer;
import studio.magemonkey.fusion.data.player.PlayerLoader;

public class FusionPlaceholders extends PlaceholderExpansion {

    @Override
    public @NotNull String getIdentifier() {
        return "fusion";
    }

    @Override
    public @NotNull String getAuthor() {
        return "MageMonkeyStudio";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        FusionPlayer fusionPlayer = PlayerLoader.getPlayer(player.getUniqueId());
        if(fusionPlayer == null) return null;
        String[] identifiers = params.split("_", 2);
        if(identifiers.length == 2) {
            /* Profession related placeholders */
            if(identifiers[0].equalsIgnoreCase("name")) {
                String profession = identifiers[1];
                if(ProfessionsCfg.getGUI(profession) == null) return null;
                return ProfessionsCfg.getTable(profession).getName();
            }
            if(identifiers[0].equalsIgnoreCase("invname")) {
                String profession = identifiers[1];
                if(ProfessionsCfg.getGUI(profession) == null) return null;
                return ProfessionsCfg.getTable(profession).getInventoryName();
            }

            /* Player related placeholders */
            if(identifiers[0].equalsIgnoreCase("level")) {
                String profession = identifiers[1];
                if(ProfessionsCfg.getGUI(profession) == null) return null;
                if(!fusionPlayer.hasProfession(profession)) return null;
                return String.valueOf(fusionPlayer.getProfession(profession).getLevel());
            }
            if(identifiers[0].equalsIgnoreCase("exp")) {
                String profession = identifiers[1];
                if(ProfessionsCfg.getGUI(profession) == null) return null;
                if(!fusionPlayer.hasProfession(profession)) return null;
                return String.valueOf(fusionPlayer.getProfession(profession).getExp());
            }
            if(identifiers[0].equalsIgnoreCase("mastered")) {
                String profession = identifiers[1];
                if(ProfessionsCfg.getGUI(profession) == null) return null;
                if(!fusionPlayer.hasProfession(profession)) return null;
                return String.valueOf(fusionPlayer.getProfession(profession).isMastered());
            }
            if(identifiers[0].equalsIgnoreCase("joined")) {
                String profession = identifiers[1];
                if(ProfessionsCfg.getGUI(profession) == null) return null;
                if(!fusionPlayer.hasProfession(profession)) return null;
                return String.valueOf(fusionPlayer.getProfession(profession).isJoined());
            }
        }
        return null;
    }
}
