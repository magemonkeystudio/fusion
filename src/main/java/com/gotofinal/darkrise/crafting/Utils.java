package com.gotofinal.darkrise.crafting;

import org.bukkit.permissions.Permissible;

public final class Utils
{
    private Utils()
    {
    }

    public static boolean hasCraftingPermission(Permissible permissible, String item)
    {
        return permissible.hasPermission("crafting.recipe." + item) || permissible.hasPermission("crafting.recipes") ||
               permissible.hasPermission("crafting.recipe.*");
    }

    public static boolean hasCraftingUsePermission(Permissible permissible, String item)
    {
        if (item == null)
        {
            return permissible.hasPermission("crafting.use") || permissible.hasPermission("crafting.use.*");
        }
        return permissible.hasPermission("crafting.use." + item) || permissible.hasPermission("crafting.use.*");
    }
}
