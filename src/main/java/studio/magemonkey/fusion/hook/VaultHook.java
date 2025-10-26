package studio.magemonkey.fusion.hook;

import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import studio.magemonkey.codex.CodexEngine;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class VaultHook {

    private static BukkitTask task;
    private static Map<UUID, Pair<Double, LocalDateTime>> storedBalances = new HashMap<>();
    private static int thresholdSeconds = 10;

    public static double getBalance(Player player) {
        if (storedBalances.containsKey(player.getUniqueId())) {
            Pair<Double, LocalDateTime> balanceData = storedBalances.get(player.getUniqueId());
            double balance = balanceData.getLeft();
            LocalDateTime timestamp = balanceData.getRight();
            if (timestamp.plusSeconds(thresholdSeconds).isBefore(LocalDateTime.now())) {
                double updatedBalance =  CodexEngine.get().getVault() != null ? CodexEngine.get().getVault().getBalance(player) : 0.0;
                storedBalances.put(player.getUniqueId(), Pair.of(updatedBalance, LocalDateTime.now()));
                return updatedBalance;
            } else {
                return balance;
            }
        } else {
            double balance = CodexEngine.get().getVault() != null ? CodexEngine.get().getVault().getBalance(player) : 0.0;
            storedBalances.put(player.getUniqueId(), Pair.of(balance, LocalDateTime.now()));
            return balance;
        }
    }

    public static void startMoneyUpdateTask() {
        // Schedule a repeating task to update all stored balances every thresholdSeconds
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(CodexEngine.get(), () -> {
            for (UUID uuid : storedBalances.keySet()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    getBalanceAsync(player, balance -> {
                        storedBalances.put(uuid, Pair.of(balance, LocalDateTime.now()));
                    });
                }
            }
        }, thresholdSeconds * 20L, thresholdSeconds * 20L);
    }

    public static void cancelMoneyUpdateTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private static void getBalanceAsync(Player player, Consumer<Double> moneyConsumer) {
        Bukkit.getScheduler().runTaskAsynchronously(CodexEngine.get(), () -> {
            double balance = CodexEngine.get().getVault() != null ? CodexEngine.get().getVault().getBalance(player) : 0.0;
            moneyConsumer.accept(balance);
        });
    }
}
