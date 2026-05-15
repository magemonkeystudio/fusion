package studio.magemonkey.fusion.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.Objects;

/**
 * Sprawdza, czy gracz posiada wymaganą "stację" (Divinity lub vanilla) gdziekolwiek w swoim ekwipunku.
 */
public class StationChecker {

    private static final NamespacedKey DIVINITY_ITEM_ID = new NamespacedKey("divinity", "item_id");
    private static final String DIVINITY_PREFIX = "divinity:";

    /**
     * Główna metoda – zwraca true, jeśli gracz ma stację określoną przez parametr "station".
     * <p>
     * Format parametru:
     * <ul>
     *   <li>{@code "divinity:twoje_id"} – sprawdza Divinity item z danym item_id</li>
     *   <li>{@code "CRAFTING_TABLE"} – sprawdza dowolny przedmiot o tym materiale (vanilla)</li>
     * </ul>
     * </p>
     */
    public static boolean hasStation(Player player, String station) {
        if (station == null || station.isEmpty()) return true; // brak wymagania

        // Rozróżnienie typu stacji po prefiksie
        if (station.toLowerCase().startsWith(DIVINITY_PREFIX)) {
            String divinityId = station.substring(DIVINITY_PREFIX.length());
            return hasDivinityStation(player, divinityId);
        } else {
            // Vanilla – oczekujemy nazwy materiału (np. CRAFTING_TABLE)
            Material material = Material.getMaterial(station.toUpperCase());
            if (material == null) {
                // Nieznany materiał – logujemy ostrzeżenie i uznajemy, że gracz nie ma stacji
                player.getServer().getLogger().warning("[Fusion] Nieznany materiał stacji: " + station);
                return false;
            }
            return hasVanillaStation(player, material);
        }
    }

    /**
     * Sprawdza, czy gracz ma gdziekolwiek w ekwipunku przedmiot z podanym divinity:item_id.
     */
    private static boolean hasDivinityStation(Player player, String itemId) {
        return anyItemMatches(player, item -> {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return false;
            String id = meta.getPersistentDataContainer().get(DIVINITY_ITEM_ID, PersistentDataType.STRING);
            return itemId.equalsIgnoreCase(id);
        });
    }

    /**
     * Sprawdza, czy gracz ma gdziekolwiek w ekwipunku przedmiot o podanym materiale.
     */
    private static boolean hasVanillaStation(Player player, Material material) {
        return anyItemMatches(player, item -> item.getType() == material);
    }

    /**
     * Pomocnicza – sprawdza wszystkie przedmioty gracza (cały Inventory + main hand)
     * pod kątem warunku zdefiniowanego przez "predicate".
     */
    private static boolean anyItemMatches(Player player, java.util.function.Predicate<ItemStack> predicate) {
        PlayerInventory inv = player.getInventory();

        // Sprawdź wszystkie sloty Inventory (plecak, pancerz, offhand)
        if (Arrays.stream(inv.getContents()).filter(Objects::nonNull).anyMatch(predicate)) {
            return true;
        }

        // Sprawdź przedmiot w głównej ręce (nie jest częścią getContents())
        ItemStack mainHand = inv.getItemInMainHand();
        if (mainHand != null && !mainHand.getType().isAir() && predicate.test(mainHand)) {
            return true;
        }

        return false;
    }
}