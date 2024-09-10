package studio.magemonkey.fusion.cfg.hooks;

import studio.magemonkey.fusion.Fusion;

import java.util.LinkedList;
import java.util.List;

public class HookManager {

    private final List<HookType> hookedPlugins;

    public HookManager() {
        hookedPlugins = new LinkedList<>();
        for (HookType hook : HookType.values()) {
            if (Fusion.getInstance().getServer().getPluginManager().getPlugin(hook.toString()) != null
                    && Fusion.getInstance().getServer().getPluginManager().isPluginEnabled(hook.toString())) {
                hookedPlugins.add(hook);
                Fusion.getInstance().getLogger().info("Hooked: " + hook);
            }
        }
    }

    public boolean isHooked(HookType... hookTypes) {
        for (HookType hook : hookTypes) {
            if (!hookedPlugins.contains(hook)) {
                return false;
            }
        }
        return true;
    }
}
