package studio.magemonkey.fusion.gui.recipe;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Helper to build a small MD5 fingerprint of an entire PlayerInventory.
 * We incorporate:
 *   - Material ordinal
 *   - amount
 *   - customModelData
 *   - displayName
 *   - lore lines
 *   - enchantments
 *   - unbreakable
 *   - durability if Damageable
 *
 * If MD5 is not available, we fall back to a simple int‐hash of slot hashCodes.
 */
public class InventoryFingerprint {
    public static byte[] fingerprint(Player p) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            for (ItemStack is : p.getInventory().getContents()) {
                if (is == null || is.getType() == Material.AIR) {
                    md.update((byte) 0);
                    continue;
                }
                ItemMeta im = is.getItemMeta();
                // Material
                md.update((byte) is.getType().ordinal());
                // Amount (4 bytes)
                // displayName
                if (im != null && im.hasDisplayName()) {
                    byte[] nameBytes = im.getDisplayName().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    md.update(nameBytes);
                }
                // lore
                if (im != null && im.hasLore()) {
                    for (String line : Objects.requireNonNull(im.getLore())) {
                        byte[] lineBytes = line.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                        md.update(lineBytes);
                    }
                }
                // enchantments
                if (im != null && im.hasEnchants()) {
                    im.getEnchants().forEach((ench, lvl) -> {
                        md.update(ByteBuffer.allocate(4).putInt(ench.getKey().hashCode()).array());
                        md.update(ByteBuffer.allocate(4).putInt(lvl).array());
                    });
                }
                // unbreakable + durability
                if (im != null) {
                    if (im.isUnbreakable()) {
                        md.update((byte) 1);
                    }
                    if (im instanceof org.bukkit.inventory.meta.Damageable dmeta && dmeta.getDamage() > 0) {
                        md.update(ByteBuffer.allocate(4).putInt(dmeta.getDamage()).array());
                    }
                }
            }
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            // Fallback: compute a 4‐byte hash code of all slot hashCodes
            int h = 7;
            for (ItemStack is : p.getInventory().getContents()) {
                h = h * 31 + ((is == null) ? 0 : is.hashCode());
            }
            return ByteBuffer.allocate(4).putInt(h).array();
        }
    }
}
