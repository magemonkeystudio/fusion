package studio.magemonkey.fusion.data.queue;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import java.io.*;
import java.util.*;

/** Immutable snapshot of the resources actually charged for one execution. */
public final class CraftingReceipt {
    private final List<ItemStack> items;
    private final int experience;
    private final double money;

    public CraftingReceipt(Collection<ItemStack> items, int experience, double money) {
        this.items = items.stream().filter(Objects::nonNull).map(ItemStack::clone).toList();
        this.experience = experience;
        this.money = money;
    }

    public List<ItemStack> getItems() { return items.stream().map(ItemStack::clone).toList(); }
    public int getExperience() { return experience; }
    public double getMoney() { return money; }

    public String encode() {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             BukkitObjectOutputStream out = new BukkitObjectOutputStream(bytes)) {
            out.writeInt(1);
            out.writeInt(experience);
            out.writeDouble(money);
            out.writeInt(items.size());
            for (ItemStack item : items) out.writeObject(item);
            out.flush();
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException e) { throw new IllegalStateException("Cannot encode crafting receipt", e); }
    }

    public static CraftingReceipt decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) return null; // legacy row
        try (BukkitObjectInputStream in = new BukkitObjectInputStream(
                new ByteArrayInputStream(Base64.getDecoder().decode(encoded)))) {
            if (in.readInt() != 1) throw new IOException("Unknown receipt version");
            int experience = in.readInt();
            double money = in.readDouble();
            int size = in.readInt();
            if (size < 0 || size > 10000) throw new IOException("Invalid receipt size");
            List<ItemStack> items = new ArrayList<>();
            for (int i = 0; i < size; i++) items.add((ItemStack) in.readObject());
            return new CraftingReceipt(items, experience, money);
        } catch (IOException | ClassNotFoundException | IllegalArgumentException e) {
            throw new IllegalStateException("Cannot decode crafting receipt; refusing an inaccurate refund", e);
        }
    }
}
