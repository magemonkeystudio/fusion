package studio.magemonkey.fusion.util;

import org.bukkit.OfflinePlayer;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.cfg.Cfg;
import studio.magemonkey.fusion.data.player.PlayerLoader;
import studio.magemonkey.fusion.data.recipes.CraftingTable;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class LevelFunction {
    private static final Map<Integer, Double> map = new HashMap<>();

    public static double getXP(int level) {
        return map.get(level);
    }

    public static int getLevel(double xp) {
        boolean                    seen = false;
        Map.Entry<Integer, Double> acc  = null;
        for (Map.Entry<Integer, Double> e : map.entrySet()) {
            if (e.getValue() <= xp) {
                if (!seen) {
                    seen = true;
                    acc = e;
                } else {
                    acc = e;
                }
            }
        }
        Optional<Map.Entry<Integer, Double>> val = seen ? Optional.of(acc) : Optional.empty();

        return val.isPresent() ? val.get().getKey() : 0;
    }

    public static int getLevel(OfflinePlayer player, CraftingTable craftingTable) {
        return getLevel(PlayerLoader.getPlayer(player.getUniqueId()).getExperience(craftingTable));
    }

    public static void generate(int levels) {
        map.clear();

        DecimalFormat format = new DecimalFormat("0");
        for (int level = 1; level <= levels; level++) {
            double xp = 0;

            for (int n = 1; n < level; n++) {
                xp += Maths.eval(Cfg.recursive.replace("n",
                        Integer.toString(n)));//Math.floor(n + 300 * Math.pow(2, n / 7));
            }

            try {
                xp = Maths.eval(Cfg.finalMod.replace("x", format.format(xp)));
            } catch (RuntimeException e) {
                Fusion.getInstance()
                        .getLogger()
                        .info("Added levels up to " + (level - 1) + " before reaching Java limitations.");
                break;
            }
//            xp = Math.floor(xp);
//            xp /= 4;

            map.put(level, xp);
        }
    }
}
