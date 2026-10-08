package studio.magemonkey.fusion.crafting;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.UnsafeValues;
import org.bukkit.inventory.*;
import org.junit.jupiter.api.Test;
import studio.magemonkey.fusion.data.queue.CraftingReceipt;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CraftingReceiptTest {
    @Test void bukkitItemReceiptsRoundTripWithoutReconstructingRequirements() {
        org.mockbukkit.mockbukkit.MockBukkit.mock();
        try {
            ItemStack original = new ItemStack(Material.DIAMOND, 3);
            var meta = original.getItemMeta();
            meta.setDisplayName("Recorded ingredient");
            var key = new org.bukkit.NamespacedKey("fusion", "receipt-test");
            meta.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.STRING, "original");
            original.setItemMeta(meta);
            CraftingReceipt receipt = new CraftingReceipt(List.of(original), 5, 1.25);
            original.setAmount(20);
            CraftingReceipt loaded = CraftingReceipt.decode(receipt.encode());
            assertEquals(Material.DIAMOND, loaded.getItems().get(0).getType());
            assertEquals(3, loaded.getItems().get(0).getAmount());
            assertEquals("Recorded ingredient", loaded.getItems().get(0).getItemMeta().getDisplayName());
            assertEquals("original", loaded.getItems().get(0).getItemMeta().getPersistentDataContainer()
                    .get(key, org.bukkit.persistence.PersistentDataType.STRING));
            ItemStack returned = loaded.getItems().get(0);
            returned.setAmount(1);
            assertEquals(3, loaded.getItems().get(0).getAmount());
            assertEquals(5, loaded.getExperience());
            assertEquals(1.25, loaded.getMoney());
        } finally { org.mockbukkit.mockbukkit.MockBukkit.unmock(); }
    }
    @Test void malformedReceiptCannotSilentlyBecomeAFreeRefund() {
        assertNull(CraftingReceipt.decode(null));
        assertThrows(IllegalStateException.class, () -> CraftingReceipt.decode("invalid"));
    }

    @Test void removalRecordsActualStackMetadataAndRefundRestoresIt() {
        var server = org.mockbukkit.mockbukkit.MockBukkit.mock();
        try {
            var player = server.addPlayer();
            ItemStack actual = new ItemStack(Material.DIAMOND, 3);
            var meta = actual.getItemMeta();
            var key = new org.bukkit.NamespacedKey("fusion", "source");
            meta.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.STRING, "player-stack");
            actual.setItemMeta(meta);
            player.getInventory().setItem(0, actual);
            // The recipe only requires a material/amount; the receipt must retain the actual PDC.
            CraftingCostService service = new CraftingCostService();
            var removed = service.takeItems(player, List.of(new ItemStack(Material.DIAMOND, 2)));
            assertNotNull(removed);
            assertEquals(1, player.getInventory().getItem(0).getAmount());
            assertEquals("player-stack", removed.get(0).getItemMeta().getPersistentDataContainer()
                    .get(key, org.bukkit.persistence.PersistentDataType.STRING));
            service.giveItems(player, removed);
            assertEquals(3, player.getInventory().getItem(0).getAmount());
        } finally { org.mockbukkit.mockbukkit.MockBukkit.unmock(); }
    }
}
