package com.gotofinal.darkrise.crafting.util;

import com.gotofinal.darkrise.crafting.DarkRiseCrafting;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

public class PlayerUtil {

    /**
     * Attempts to get the numeric value of the permission starting with 'permission' <br/>
     * For example, if the player has 'perm.test.5' and 'perm.test' is passed in, 5 will be returned.
     * If the permission is not set, or the last part is non-numeric, 0 will be returned.
     * @param player The player to test
     * @param permission The permission to test
     * @return Number representing permission
     */
    public static int getPermOption(Player player, String permission) {
        int ret = 0;
        for (PermissionAttachmentInfo perm : player.getEffectivePermissions()) {
            String ps = perm.getPermission();
            if (ps.startsWith(permission)) {
                String end = ps.substring(ps.lastIndexOf('.')+1);
                try {
                    ret = Math.max(ret, Integer.parseInt(end));
                } catch (NumberFormatException e) {
                    DarkRiseCrafting.log.warning("Could not get numeric permission value from '" + end + "'");
                    continue;
                }
            }
        }

        return ret;
    }

}
