package studio.magemonkey.fusion.gui;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.api.DelayedCommand;
import studio.magemonkey.codex.api.Replacer;
import studio.magemonkey.codex.util.ItemUtils;
import studio.magemonkey.codex.util.messages.MessageData;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.api.FusionAPI;
import studio.magemonkey.fusion.cfg.Cfg;
import studio.magemonkey.fusion.cfg.CraftingRequirementsCfg;
import studio.magemonkey.fusion.cfg.ProfessionsCfg;
import studio.magemonkey.fusion.data.player.PlayerLoader;
import studio.magemonkey.fusion.data.professions.pattern.Category;
import studio.magemonkey.fusion.data.professions.pattern.InventoryPattern;
import studio.magemonkey.fusion.data.queue.CraftingQueue;
import studio.magemonkey.fusion.data.queue.QueueItem;
import studio.magemonkey.fusion.data.recipes.CalculatedRecipe;
import studio.magemonkey.fusion.data.recipes.CraftingTable;
import studio.magemonkey.fusion.data.recipes.Recipe;
import studio.magemonkey.fusion.data.recipes.RecipeItem;
import studio.magemonkey.fusion.gui.recipe.IngredientFingerprint;
import studio.magemonkey.fusion.gui.recipe.InventoryFingerprint;
import studio.magemonkey.fusion.gui.recipe.RecipeCacheKey;
import studio.magemonkey.fusion.gui.slot.Slot;
import studio.magemonkey.fusion.util.ChatUT;
import studio.magemonkey.fusion.util.ExperienceManager;
import studio.magemonkey.fusion.util.PlayerUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class RecipeGui implements Listener {

    private final   Player                             player;
    private final   CraftingTable                      table;
    @Getter
    protected final String                             name;
    @Getter
    private final   String                             inventoryName;
    private final   Category                           category;
    private         InventoryPattern                   pattern;
    private final   HashMap<Integer, CalculatedRecipe> recipes;

    private int page = 0;
    private int nextPage;
    private int prevPage;
    private boolean isLoaded = false;

    /* Queue Crafting Mode */
    private int           queuePage = 0;
    private int           prevQueuePage;
    private int           nextQueuePage;
    private CraftingQueue queue;
    private int lastQueueSecond = -1;                   // <<< track last‐seen wall‐clock second
    private int lastQueueSize = 0;                       // <<< track last‐seen queue size

    /* Manual Crafting Mode */
    private       BukkitTask            craftingTask;
    private       BukkitTask            barTask;
    private       BossBar               bar;
    private final Collection<ItemStack> refund          = new ArrayList<>();
    private       ItemStack             previousCursor;
    private       boolean               craftingSuccess = true;
    private       Recipe                craftingRecipe  = null;


    /* Inventory Slot Handling */
    @Getter
    @Setter
    private       Inventory          inventory;
    private       Slot[]             slots;
    private final ArrayList<Integer> resultSlots  = new ArrayList<>(20);
    private final ArrayList<Integer> blockedSlots = new ArrayList<>(20);
    private final ArrayList<Integer> queuedSlots  = new ArrayList<>(20);

    // Caches all previously built CalculatedRecipe objects with a size limit:
    private static final Map<RecipeCacheKey, CalculatedRecipe> recipeCache = Collections.synchronizedMap(
            new LinkedHashMap<>(100, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<RecipeCacheKey, CalculatedRecipe> eldest) {
                    return size() > 100; // Limit cache size to 100 entries
                }
            }
    );

    // Last‐seen “inventory fingerprint” so we know if we truly need to recalc:
    private byte[] lastInventoryHash = new byte[0];
    private int lastSeenLevel = -1;
    private double lastSeenMoney = -1.0;

    // Last page counts so we skip redraw unless page or queuePage also changed:
    private int lastPageCount = -1, lastQueuePageCount = -1;

    public RecipeGui(Player player, CraftingTable table, Category category) {
        this.player = player;
        this.table = table;
        this.name = table.getName();
        this.inventoryName = ChatUT.hexString(table.getInventoryName());
        this.recipes = new HashMap<>(20);
        this.category = category != null ? category : new Category("master", "PAPER", this.table.getRecipePattern(), 1);
        if (this.category.getPattern() == null)
            this.category.setPattern(table.getRecipePattern());

        if (this.category.getName().equals("master")) {
            this.category.getRecipes().addAll(this.table.getRecipes().values());
        }
        setPattern();
        if (Cfg.craftingQueue && pattern != null) {
            this.queue = FusionAPI.getPlayerManager().getPlayer(player).getQueue(table.getName(), this.category);
        }
        Fusion.registerListener(this);
        initialize();
    }

    private void mapSlots() {
        this.resultSlots.clear();
        this.queuedSlots.clear();
        this.slots = new Slot[pattern.getPattern().length * 9];
        int k             = -1;
        int prevPage      = -1, nextPage = -1;
        int prevQueuePage = -1, nextQueuePage = -1;
        for (String row : this.pattern.getPattern()) {
            for (char c : row.toCharArray()) {
                k++;
                switch (c) {
                    case '=', 'o' -> {
                        this.slots[k] = Slot.BASE_RESULT_SLOT;
                        this.resultSlots.add(k);
                    }
                    case '>' -> {
                        this.slots[k] = Slot.BLOCKED_SLOT;
                        nextPage = k;
                    }
                    case '<' -> {
                        this.slots[k] = Slot.BLOCKED_SLOT;
                        prevPage = k;
                    }
                    case '-' -> {
                        this.slots[k] = Slot.QUEUED_SLOT;
                        this.queuedSlots.add(k);
                    }
                    case '}' -> {
                        this.slots[k] = Slot.BLOCKED_SLOT;
                        nextQueuePage = k;
                    }
                    case '{' -> {
                        this.slots[k] = Slot.BLOCKED_SLOT;
                        prevQueuePage = k;
                    }
                    default -> {
                        this.slots[k] = Slot.BLOCKED_SLOT;
                        this.blockedSlots.add(k);
                    }
                }
            }
        }
        this.nextPage = nextPage;
        this.prevPage = prevPage;
        this.nextQueuePage = nextQueuePage;
        this.prevQueuePage = prevQueuePage;
    }

    public void updateBlockedSlots(MessageData[] data) {
        int totalItems       = category.getRecipes().size();
        int queuedTotalItems = queue != null ? queue.getQueue().size() : 0;
        int fullPages        = totalItems / resultSlots.size();
        int rest             = totalItems % resultSlots.size();
        int pages            = (rest == 0) ? fullPages : (fullPages + 1);

        boolean includeBack = category.hasPrevious();
        int     queuedPages = -1;

        if (!queuedSlots.isEmpty()) {
            int queuedFullPages = queuedTotalItems / queuedSlots.size();
            int queuedRest      = queuedTotalItems % queuedSlots.size();
            queuedPages = (queuedRest == 0) ? queuedFullPages : (queuedFullPages + 1);
        }

        int                           k     = -1;
        HashMap<Character, ItemStack> items = pattern.getItems();

        ArrayList<Integer> leaveBlank = new ArrayList<>();
        ArrayList<Integer> fill       = new ArrayList<>();
        for (String row : pattern.getPattern()) {
            for (char c : row.toCharArray()) {
                k++;
                ItemStack item = ItemUtils.replaceText(items.get(c), data);
                if (!includeBack && c == '<' && page <= 0) {
                    leaveBlank.add(k);
                    continue;
                }
                if (c == '>' && page + 1 >= pages) {
                    leaveBlank.add(k);
                    continue;
                }
                if (c == '{' && (Cfg.craftingQueue && queuePage <= 0)) {
                    fill.add(k);
                    continue;
                }
                if (c == '}' && (Cfg.craftingQueue && ((queuedPages > -1) && (queuePage + 1 >= queuedPages)))) {
                    fill.add(k);
                    continue;
                }
                if (item != null && (c != '-')) {
                    inventory.setItem(k, item.clone());
                } else if (item != null) {
                    if (queue != null && queue.getQueuedItems().containsKey(k)) {
                        inventory.setItem(k, queue.getQueuedItems().get(k).getIcon());
                    } else {
                        inventory.setItem(k, ProfessionsCfg.getQueueSlot(name));
                    }
                }
            }
        }

        for (Integer index : leaveBlank) {
            if (inventory.getSize() > index + 1)
                inventory.setItem(index, inventory.getItem(index + 1));
            else
                inventory.setItem(index, inventory.getItem(index - 1));
        }
        for (Integer index : fill) {
            inventory.setItem(index, ProfessionsCfg.getFillItem(name));
        }
    }

    public void initialize() {
        this.inventory = Bukkit.createInventory(null, pattern.getInventorySize(), inventoryName);
        mapSlots();
    }

    public void reloadRecipes() {
        if (!player.isOnline()) return;

        try {
            //
            // ─── 1) Compute new “fingerprint” of the player’s current inventory + level + money ───
            //
            byte[] newHash   = InventoryFingerprint.fingerprint(player);
            int newLevel     = table.getLevelFunction().getLevel(player);
            double newMoney  = (CodexEngine.get().getVault() == null)
                    ? 0.0
                    : CodexEngine.get().getVault().getBalance(player);

            boolean invChanged   = !Arrays.equals(newHash, lastInventoryHash);
            boolean levelChanged = (newLevel != lastSeenLevel);
            boolean moneyChanged = (newMoney != lastSeenMoney);

            lastInventoryHash = newHash;
            lastSeenLevel     = newLevel;
            lastSeenMoney     = newMoney;

            //
            // ─── 2) Re-obtain the recipe list & calculate total pages ───
            //
            setPattern();  // (exactly as before)
            ItemStack fill = table.getFillItem();

            Collection<Recipe> allRecipesCollection = new ArrayList<>(category.getRecipes());
            allRecipesCollection.removeIf(r -> r.isHidden(player));

            int pageSize   = resultSlots.size();
            int totalItems = allRecipesCollection.size();
            int page       = this.page;

            int fullPages = (pageSize == 0) ? 0 : totalItems / pageSize;
            int rest      = (pageSize == 0) ? 0 : totalItems % pageSize;
            int pages     = (rest == 0) ? fullPages : (fullPages + 1);

            if (page >= pages && pages > 0) {
                // Clamp page if out-of-range
                this.page = pages - 1;
                if (this.page != page) {
                    // Avoid infinite recursion
                    this.reloadRecipes();
                }
                return;
            }

            //
            // ─── 3) Compute total queue pages (if craftingQueue is enabled) ───
            //
            List<QueueItem> allQueuedItems = (Cfg.craftingQueue && queue != null)
                    ? new ArrayList<>(queue.getQueue())
                    : Collections.emptyList();
            int queueSize      = allQueuedItems.size();
            int queuePageSize  = queuedSlots.size();
            int fullQueuePages = (queuePageSize == 0) ? 0 : queueSize / queuePageSize;
            int restQueue      = (queuePageSize == 0) ? 0 : queueSize % queuePageSize;
            int queuePages     = (restQueue == 0) ? fullQueuePages : (fullQueuePages + 1);

            if (queuePage >= queuePages && queuePages > 0) {
                this.queuePage = queuePages - 1;
                this.reloadRecipes();
                return;
            }

            //
            // ─── 4) Bail out early if nothing changed _and_ no unfinished queued items ───
            //
            boolean hasUnfinishedQueue = (Cfg.craftingQueue && queue != null && !queue.getQueuedItems().isEmpty());
            boolean queueSizeChanged   = (queueSize != lastQueueSize);            // <<< check if queue length changed
            lastQueueSize = queueSize;                                            // <<< update lastQueueSize

            if (!invChanged && !levelChanged && !moneyChanged
                    && lastPageCount == page
                    && lastQueuePageCount == queuePage
                    && !hasUnfinishedQueue
                    && !queueSizeChanged)                                            // <<< also require queue size unchanged
            {
                return;
            }
            lastPageCount      = page;
            lastQueuePageCount = queuePage;

            //
            // ─── 5) Build a single Map<IngredientFingerprint,Integer> of the player’s entire inventory ───
            //
            Map<IngredientFingerprint, Integer> invCounts = new HashMap<>();
            for (ItemStack is : player.getInventory().getContents()) {
                if (is == null || is.getType() == Material.AIR) continue;
                IngredientFingerprint fp = IngredientFingerprint.of(is);
                invCounts.merge(fp, is.getAmount(), Integer::sum);
            }

            //
            // ─── 6) Clear out any “result” slots from the previous page ───
            //
            Integer[] resultSlotArray = resultSlots.toArray(new Integer[0]);
            for (Integer slotIndex : resultSlotArray) {
                if (slotIndex != null) {
                    inventory.setItem(slotIndex, null);
                }
            }
            recipes.clear();

            //
            // ─── 7) Re-populate this page’s recipe icons, using a cache key to avoid repeated recalculation ───
            //
            Recipe[] allRecipesArray = allRecipesCollection.toArray(new Recipe[0]);
            int startIndex = page * pageSize;
            int endIndex   = Math.min(startIndex + pageSize, totalItems);

            for (int i = startIndex, idx = 0; i < endIndex; i++, idx++) {
                Recipe recipe    = allRecipesArray[i];
                int slotIndex    = resultSlotArray[idx];
                RecipeCacheKey cacheKey = new RecipeCacheKey(
                        recipe.getRecipePath(),
                        newHash,
                        newLevel,
                        newMoney
                );

                CalculatedRecipe calc;
                if (recipeCache.containsKey(cacheKey)) {
                    calc = recipeCache.get(cacheKey);
                } else {
                    CalculatedRecipe fresh = CalculatedRecipe.create(
                            recipe,
                            new HashMap<>(invCounts),
                            player,
                            table
                    );
                    recipeCache.put(cacheKey, fresh);
                    calc = fresh;
                }

                recipes.put(slotIndex, calc);
                inventory.setItem(slotIndex, calc.getIcon().clone());
            }

            //
            // ─── 8) Fill anything not set yet with the “fill” background ───
            //
            for (int k = 0; k < inventory.getSize(); k++) {
                ItemStack it = inventory.getItem(k);
                if (it == null || it.getType() == Material.AIR) {
                    inventory.setItem(k, fill.clone());
                }
            }

            //
            // ─── 9) If crafting-queue mode is enabled, clear + rebuild queued slots
            //           ─ only when the wall-clock second or queueSize changed ─
            //
            if (Cfg.craftingQueue && queue != null) {
                int nowSec = (int) (System.currentTimeMillis() / 1000L);
                if (nowSec != lastQueueSecond || queueSizeChanged) {        // <<< MODIFIED
                    lastQueueSecond = nowSec;                                // <<< MODIFIED

                    // 9a) Clear all queue slots to the “empty queue” icon
                    Integer[] queuedIndices = queuedSlots.toArray(new Integer[0]);
                    for (int qIndex : queuedIndices) {
                        inventory.setItem(qIndex, ProfessionsCfg.getQueueSlot(table.getName()));
                    }
                    this.queue.getQueuedItems().clear();

                    // 9b) Place each queued item onto its slot for the current queuePage
                    if (!allQueuedItems.isEmpty() && queuePageSize > 0) {
                        int j       = 0;
                        int qStart  = queuePage * queuePageSize;
                        int qEnd    = Math.min(qStart + queuePageSize, queueSize);
                        QueueItem[] allQueueItemsArray = allQueuedItems.toArray(new QueueItem[0]);
                        Integer[] qSlots = queuedIndices;

                        for (int q = qStart; q < qEnd && j < qSlots.length; q++, j++) {
                            QueueItem qi = allQueueItemsArray[q];
                            int slot     = qSlots[j];
                            this.queue.getQueuedItems().put(slot, qi);
                            qi.updateIcon();
                            inventory.setItem(slot, qi.getIcon().clone());
                        }
                    }
                }
                // (Otherwise, same second / queueSize, so skip rebuilding this block.)
            }

            //
            // ─── 10) Finally, update “arrows” / “fill” / etc. exactly as before ───
            //
            updateBlockedSlots(new MessageData[]{
                    new MessageData("level",    table.getLevelFunction().getLevel(player)),
                    new MessageData("category", category),
                    new MessageData("gui",      getName()),
                    new MessageData("player",   player.getName()),
                    new MessageData("bal",
                            CodexEngine.get().getVault() == null
                                    ? 0
                                    : CodexEngine.get().getVault().getBalance(player))
            });

            this.isLoaded = true;
        }
        catch (Exception e) {
            // On any exception, clear the inventory and close it to avoid partial states
            this.inventory.clear();
            Bukkit.getScheduler().runTask(Fusion.getInstance(), this.player::closeInventory);
            throw new RuntimeException(
                    "Exception was thrown when reloading recipes for: " + this.player.getName(), e
            );
        }
        finally {
            // If queue-mode is on and there are unfinished items, re-schedule another reload in 1 second
            if (Cfg.craftingQueue && queue != null && !queue.getQueuedItems().isEmpty()) {
                boolean requiresUpdate = false;
                for (Map.Entry<Integer, QueueItem> entry : queue.getQueuedItems().entrySet()) {
                    if (!entry.getValue().isDone()) {
                        requiresUpdate = true;
                        break;
                    }
                }
                if (requiresUpdate) {
                    Bukkit.getScheduler().runTaskLater(Fusion.getInstance(), this::reloadRecipes, 20L);
                }
                this.isLoaded = true;
            }
        }
    }

    public void reloadRecipesTask() {
        Bukkit.getScheduler().runTaskLater(Fusion.getInstance(), this::reloadRecipes, 1L);
    }

    private boolean validatePageCount() {
        if (this.page <= 0) {
            this.reloadRecipesTask();
            return false;
        }
        Collection<Recipe> allRecipes     = table.getRecipes().values();
        int                pageSize       = resultSlots.size();
        int                allRecipeCount = allRecipes.size();
        int                page           = this.page;

        int fullPages = allRecipeCount / pageSize;
        int rest      = allRecipeCount % pageSize;
        int pages     = (rest == 0) ? fullPages : (fullPages + 1);
        if (page >= pages) {
            this.page = pages;
            this.reloadRecipesTask();
            return false;
        }
        return true;
    }

    private void prevPage() {
        if (this.page <= 0) {
            cancel(true);
            ProfessionsCfg.getGUI(name).open(player);
            return;
        }
        this.page--;
        if (this.validatePageCount()) {
            this.reloadRecipesTask();
        }
    }

    private void nextPage() {
        this.page++;
        if (this.validatePageCount()) {
            this.reloadRecipesTask();
        }
    }

    private boolean validateQueuePageCount() {
        if (this.queuePage <= 0) {
            this.reloadRecipesTask();
            return false;
        }
        Collection<QueueItem> allQueuedItems = queue.getQueue();
        int                   pageSize       = queuedSlots.size();
        int                   count          = allQueuedItems.size();
        int                   page           = this.queuePage;

        int fullPages = count / pageSize;
        int rest      = count % pageSize;
        int pages     = (rest == 0) ? fullPages : (fullPages + 1);
        if (page >= pages) {
            this.queuePage = pages;
            this.reloadRecipesTask();
            return false;
        }
        return true;
    }

    private void prevQueuePage() {
        if (this.queuePage <= 0)
            return;
        this.queuePage--;
        if (this.validateQueuePageCount()) {
            this.reloadRecipesTask();
        }
    }

    private void nextQueuePage() {
        this.queuePage++;
        if (this.validateQueuePageCount()) {
            this.reloadRecipesTask();
        }
    }

    public void setPattern() {
        this.pattern = category.getPattern();
        if (!pattern.getItems().containsKey('<') || pattern.getItems().containsKey('>') || pattern.getItems()
                .containsKey('{') || pattern.getItems().containsKey('}')) {
            this.pattern.setItems(table.getRecipePattern().getItems());
        }
        mapSlots();
    }

    public void resetPattern() {
        this.pattern = category.getPattern();
        mapSlots();
    }

    public void setSlot(int i, Slot slot) {
        this.slots[i] = slot;
    }

    public Slot getSlot(int i) {
        return this.slots[i];
    }

    public void open(Player player) {
        ProfessionGuiRegistry.getLatestRecipeGui().put(player.getUniqueId(), this);
        if (!isLoaded)
            reloadRecipes();
        player.openInventory(inventory);
    }

    /* Manage Crafting Algorithm */
    private boolean canCraft(CalculatedRecipe calculatedRecipe, int slot) {
        Recipe recipe = calculatedRecipe.getRecipe();
        if (calculatedRecipe.getRecipe().getConditions().isMastery() && !PlayerLoader.getPlayer(player)
                .hasMastered(table.getName())) {
            CodexEngine.get().getMessageUtil().sendMessage("fusion.error.noMastery",
                    player,
                    new MessageData("craftingTable", ProfessionsCfg.getTable(table.getName())));
            return false;
        }
        if (!calculatedRecipe.isCanCraft()) {
            player.sendMessage(CraftingRequirementsCfg.getCanCraft(false));
            return false;
        }

        if (!Objects.equals(this.recipes.get(slot), calculatedRecipe)) {
            return false;
        }
        if (table.getLevelFunction().getLevel(player) < recipe.getConditions().getProfessionLevel()) {
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("fusion.error.noLevel", player, new MessageData("recipe", recipe));
            return false;
        }
        if (ExperienceManager.getTotalExperience(this.player) < recipe.getConditions().getExpCost()) {
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("fusion.error.noXP", player, new MessageData("recipe", recipe));
            return false;
        }
        if (recipe.getConditions().getMoneyCost() != 0 && CodexEngine.get().getVault() != null
                && !CodexEngine.get().getVault().canPay(this.player, recipe.getConditions().getMoneyCost())) {
            CodexEngine.get()
                    .getMessageUtil()
                    .sendMessage("fusion.error.noFunds", player, new MessageData("recipe", recipe));
            return false;
        }
        return true;
    }

    private boolean craft(int slot, boolean addToCursor) {
        if (!recipes.containsKey(slot)) {
            return false;
        }
        CalculatedRecipe calculatedRecipe = this.recipes.get(slot);
        Recipe recipe = calculatedRecipe.getRecipe();
        if (craftingRecipe != null && craftingRecipe.equals(recipe)) {
            cancel(true);
            return false;
        }

        cancel(true);
        if (!canCraft(calculatedRecipe, slot)) return false;

        RecipeItem recipeResult = recipe.getSettings().getRecipeItem();
        ItemStack  resultItem   = recipeResult.getItemStack();

        // Add "Crafted by" lore if the player has permission
        if (player.hasPermission("fusion.craftedby." + recipe.getName())) {
            ItemMeta meta = resultItem.getItemMeta();
            List<String> lore = (meta != null && meta.hasLore()) ? meta.getLore() : new ArrayList<>();
            lore.add(ChatColor.WHITE + " - " + ChatColor.YELLOW + "Crafted by: " + ChatColor.WHITE + player.getName());
            meta.setLore(lore);
            resultItem.setItemMeta(meta);
        }

        // If adding directly to cursor, ensure enough room
        if (addToCursor) {
            ItemStack cursor = this.player.getItemOnCursor();
            if (resultItem.isSimilar(cursor)) {
                if ((resultItem.getAmount() + cursor.getAmount()) > resultItem.getMaxStackSize()) {
                    return false;
                }
            } else if (cursor.getType() != Material.AIR) {
                return false;
            }
        }

        //
        // ─── 1) Build a local copy of the ingredient list ───
        //
        List<ItemStack> requiredItems = new ArrayList<>(recipe.getItemsToTake());
        // Track exactly what we remove, so we can refund on failure
        List<ItemStack> removedSoFar = new ArrayList<>();

        PlayerInventory inv = this.player.getInventory();
        boolean missingSomething = false;

        //
        // ─── 2) For each required ingredient, manually drain across all matching slots ───
        //
        for (ItemStack required : requiredItems) {
            int need = required.getAmount();
            ItemStack neededTemplate = required.clone(); // same material+meta

            // Iterate through every inventory slot to match via isSimilar()
            for (int slotIndex = 0; slotIndex < inv.getSize() && need > 0; slotIndex++) {
                ItemStack slotStack = inv.getItem(slotIndex);
                if (slotStack == null || slotStack.getType() == Material.AIR) continue;

                // Use CalculatedRecipe.isSimilar() to match custom NBT/lore
                if (!CalculatedRecipe.isSimilar(neededTemplate, slotStack)) {
                    continue;
                }

                int available = slotStack.getAmount();
                int take = Math.min(available, need);
                // Subtract “take” from that slot
                slotStack.setAmount(available - take);
                if (slotStack.getAmount() <= 0) {
                    inv.setItem(slotIndex, null);
                } else {
                    inv.setItem(slotIndex, slotStack);
                }

                // Track exactly what we removed
                ItemStack actuallyTaken = neededTemplate.clone();
                actuallyTaken.setAmount(take);
                removedSoFar.add(actuallyTaken);

                need -= take;
            }

            if (need > 0) {
                // Could not find enough of “required” across all slots
                missingSomething = true;

                // ─── Roll back everything we already removed ───
                for (ItemStack alreadyRemoved : removedSoFar) {
                    Map<Integer, ItemStack> overflow = inv.addItem(alreadyRemoved.clone());
                    for (ItemStack drop : overflow.values()) {
                        this.player.getWorld().dropItemNaturally(this.player.getLocation(), drop);
                    }
                }
                break;
            }
        }

        if (missingSomething) {
            // At least one ingredient was short → inform player and abort
            CodexEngine.get().getMessageUtil()
                    .sendMessage("fusion.error.insufficientItems", player, new MessageData("recipe", recipe));
            cancel(true);
            return false;
        }

        // All ingredients were successfully removed; add those to refund list
        refund.addAll(removedSoFar);

        //
        // ─── 3) Proceed with cooldown / boss‐bar / giving the result ───
        //
        if (!Cfg.craftingQueue) {
            double modifier = Fusion.getInstance().getPlayerCooldown(player);
            int cooldown = modifier == 0d
                    ? recipe.getCraftingTime()
                    : (int) Math.round(
                    recipe.getCraftingTime() - (recipe.getCraftingTime() * modifier)
            );

            showBossBar(this.player, recipe.getSettings().getRecipeItem().getItemStack(), cooldown);

            if (cooldown != 0) {
                previousCursor = player.getOpenInventory().getCursor();
                player.getOpenInventory().setCursor(new ItemStack(Material.BARRIER));
            }

            craftingSuccess = false;
            craftingRecipe = recipe;
            craftingTask = Fusion.getInstance().runTaskLater(cooldown, () -> {
                craftingSuccess = true;

                if (recipe.getResults().getCommands().isEmpty()) {
                    if (addToCursor) {
                        ItemStack cursor = this.player.getItemOnCursor();
                        if (cursor.isSimilar(recipe.getSettings().getRecipeItem().getItemStack())) {
                            if (cursor.getAmount() < cursor.getMaxStackSize()
                                    && cursor.getAmount() + recipe.getSettings().getRecipeItem().getAmount()
                                    <= cursor.getMaxStackSize()) {
                                cursor.setAmount(cursor.getAmount() + recipe.getSettings().getRecipeItem().getAmount());
                                this.player.setItemOnCursor(cursor);
                            } else {
                                craftingSuccess = false;
                            }
                        } else if (cursor.getType() == Material.AIR) {
                            this.player.setItemOnCursor(resultItem);
                        } else {
                            craftingSuccess = false;
                        }
                    } else {
                        boolean fits = calcWillFit(resultItem);
                        if (fits) {
                            HashMap<Integer, ItemStack> notAdded = inv.addItem(resultItem);
                            if (!notAdded.isEmpty()) {
                                for (ItemStack stack : notAdded.values()) {
                                    this.player.getWorld().dropItemNaturally(this.player.getLocation(), stack);
                                }
                            }
                        } else {
                            craftingSuccess = false;
                        }
                    }
                }

                if (craftingSuccess) {
                    cancel(false);
                    CodexEngine.get().getVault().take(this.player, recipe.getConditions().getMoneyCost());
                    // Commands
                    DelayedCommand.invoke(Fusion.getInstance(), player, recipe.getResults().getCommands());

                    // Experience
                    long professionExp =
                            recipe.getResults().getProfessionExp() + (long) (recipe.getResults().getProfessionExp()
                                    * PlayerUtil.getProfessionExpBonusThroughPermissions(player, table.getName()));
                    if (professionExp > 0) {
                        FusionAPI.getEventServices()
                                .getProfessionService()
                                .giveProfessionExp(player, table, professionExp);
                    }
                    if (recipe.getResults().getVanillaExp() > 0) {
                        player.giveExp(recipe.getResults().getVanillaExp());
                    }

                    // Restart the crafting sequence if auto-crafting is enabled
                    if (PlayerLoader.getPlayer(player).isAutoCrafting() && !this.recipes.isEmpty()) {
                        reloadRecipesTask();
                        boolean success = craft(slot, addToCursor); //Call this method again recursively
                        if (!success)
                            CodexEngine.get().getMessageUtil().sendMessage("fusion.autoCancelled", player);
                    }
                } else {
                    cancel(true);
                }
            });
        } else {
            if (recipe.getConditions().getMoneyCost() != 0 && CodexEngine.get().getVault() != null)
                CodexEngine.get().getVault().take(this.player, recipe.getConditions().getMoneyCost());
            this.queue.addRecipe(this.recipes.get(slot).getRecipe());
        }
        return true;
    }

    private boolean calcWillFit(ItemStack item) {
        Inventory inv = Bukkit.createInventory(null, InventoryType.PLAYER);
        inv.setContents(inventory.getContents());
        return inv.addItem(item).isEmpty();
    }

    public void executeCommands(Character c, HumanEntity player) {
        Collection<DelayedCommand> patternCommands = getPattern().getCommands(c);
        if (patternCommands != null && !patternCommands.isEmpty()) {
            DelayedCommand.invoke(Fusion.getInstance(), player, patternCommands,
                    Replacer.replacer("{crafting}", getName()),
                    Replacer.replacer("{inventoryName}", getInventoryName()));
        }
    }

    /* Manual Crafting Options */
    private void showBossBar(Player target, ItemStack item, double cooldown) {
        if (cooldown == 0)
            return;

        bar = Bukkit.createBossBar(CraftingRequirementsCfg.getBossBarTitle(item),
                BarColor.BLUE,
                BarStyle.SOLID,
                BarFlag.PLAY_BOSS_MUSIC);
        bar.setProgress(0d);
        bar.addPlayer(target);
        barTask = new BukkitRunnable() {
            int count = 0;

            public void run() {
                if (count >= cooldown * 20) {
                    this.cancel();
                    bar.removeAll();
                }

                bar.setProgress(Math.min(1d, count / (cooldown * 20)));

                count++;
            }
        }.runTaskTimer(Fusion.getInstance(), 1L, 1L);
    }

    private void cancel(boolean refundAll) {
        if (!Cfg.craftingQueue) {
            if (craftingTask == null) return;
            craftingRecipe = null;
            if (barTask != null) {
                barTask.cancel();
                barTask = null;
                bar.removeAll();
                bar = null;
            }

            if (!craftingSuccess && PlayerLoader.getPlayer(player).isAutoCrafting()) {
                CodexEngine.get().getMessageUtil().sendMessage("fusion.autoCancelled", player);
            }

            if (player.getOpenInventory().getCursor() != null
                    && player.getOpenInventory().getCursor().getType() == Material.BARRIER) {
                if (previousCursor != null) {
                    player.getOpenInventory().setCursor(previousCursor);
                    previousCursor = null;
                } else {
                    player.getOpenInventory().setCursor(new ItemStack(Material.AIR));
                }
            }

            if (craftingTask != null)
                craftingTask.cancel();
            craftingTask = null;

            if (!refundAll || craftingSuccess)
                return;

            PlayerInventory       inventory = player.getInventory();
            Collection<ItemStack> notAdded  = inventory.addItem(this.refund.toArray(new ItemStack[0])).values();
            if (!notAdded.isEmpty()) {
                for (ItemStack item : notAdded) {
                    player.getLocation().getWorld().dropItem(player.getLocation(), item);
                }
            }
            this.refund.clear();
        }
    }

    /* Event-Handling */
    public void click(InventoryClickEvent event) {
        event.setCancelled(true);
        if ((event.getRawSlot() >= slots.length)) {
            if (event.getCursor().getType() == Material.BARRIER)
                event.setCancelled(true);
            return;
        }
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
            return;
        }

        Character c = pattern.getSlot(event.getRawSlot());
        executeCommands(c, event.getWhoClicked());

        // Close on click
        if (pattern.getCloseOnClickSlots().contains(c)) {
            Bukkit.getScheduler().runTask(Fusion.getInstance(), () -> event.getWhoClicked().closeInventory());
        }

        if (slots[event.getRawSlot()].equals(Slot.BLOCKED_SLOT)) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
            if ((nextPage != -1) && (event.getSlot() == nextPage)) {
                this.nextPage();
                return;
            }
            if (prevPage != -1 && event.getSlot() == prevPage) {
                this.prevPage();
                return;
            }
            if ((nextQueuePage != -1) && event.getSlot() == nextQueuePage) {
                // Open the next queue page
                this.nextQueuePage();
                return;
            }
            if (prevQueuePage != -1 && event.getSlot() == prevQueuePage) {
                // Open the previous queue page
                this.prevQueuePage();
            }
            return;
        }
        if (slots[event.getRawSlot()].equals(Slot.BASE_RESULT_SLOT)) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
            Fusion.getInstance().runSync(() -> {
                this.reloadRecipes();
                this.craft(event.getRawSlot(), false);
                this.reloadRecipesTask();
            });
            return;
        }
        if (slots[event.getRawSlot()].equals(Slot.QUEUED_SLOT)) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
            if (queuedSlots.contains(event.getSlot())) {
                // Interact with a queued item
                QueueItem item = queue.getQueuedItems().get(event.getSlot());
                if (item == null) return;
                if (item.isDone()) {
                    if (event.isLeftClick()) {
                        queue.finishRecipe(item);
                        this.reloadRecipes();
                    } else if (event.isRightClick()) {
                        int queueSize =
                                queue.getQueue().size(); // Estimated time that is required to finish all recipes
                        queue.finishAllRecipes();
                        Bukkit.getScheduler().runTaskLater(Fusion.getInstance(), this::reloadRecipes, queueSize + 1);
                    }
                } else {
                    queue.removeRecipe(item, true);
                }
            }
            return;
        }
        if (event.getCursor().getType() != Material.AIR) {
            if (Slot.SPECIAL_CRAFTING_SLOT.canHoldItem(event.getCursor()) == null) {
                event.setResult(Event.Result.DENY);
                return;
            }
        }
        this.reloadRecipesTask();
    }

    public void close(Player p, Inventory inv) {
        if (inv == null) {
            return;
        }
        Inventory pInventory = p.getInventory();
        if (inv.equals(this.inventory)) {
            for (int i = 0; i < this.slots.length; i++) {
                if (this.slots[i].equals(Slot.BLOCKED_SLOT) ||
                        this.slots[i].equals(Slot.BASE_RESULT_SLOT) ||
                        this.slots[i].equals(Slot.QUEUED_SLOT)) {
                    continue;
                }
                ItemStack it = inv.getItem(i);
                if (it != null) {
                    pInventory.addItem(it)
                            .values()
                            .stream()
                            .filter(Objects::nonNull)
                            .forEach(itemStack -> p.getWorld().dropItem(p.getLocation(), itemStack));
                }
            }
            cancel(true);
            inv.clear();
        }
    }

    /*
     Event to prevent the player from dragging items into the crafting slots
     while doing manual crafting
     */
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) {
            return;
        }
        if (e.getInventory().equals(this.inventory) && !Cfg.craftingQueue) {
            if (e.getOldCursor().getType() == Material.BARRIER)
                e.setCancelled(true);
            if (e.getRawSlots()
                    .stream()
                    .anyMatch(i -> (i < this.slots.length) &&
                            (!Objects.equals(this.slots[i], Slot.SPECIAL_CRAFTING_SLOT)))) {
                e.setResult(Event.Result.DENY);
                return;
            }

            if (e.getNewItems().values().stream().anyMatch(i ->
                    Slot.SPECIAL_CRAFTING_SLOT.canHoldItem(i) == null)) {
                e.setResult(Event.Result.DENY);
            }
            reloadRecipesTask();
        }
    }

    /*
        Event to prevent the player from dropping items into the crafting slots
        while doing manual crafting
     */
    public void drop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (this.getInventory().getViewers().contains(player) && !Cfg.craftingQueue) {
            ItemStack stack = event.getItemDrop().getItemStack();
            if (stack.getType() == Material.BARRIER) {
                event.getItemDrop().remove();
                if (player.getOpenInventory().getCursor() == null
                        || player.getOpenInventory().getCursor().getType() == Material.AIR)
                    player.getOpenInventory().setCursor(stack);

                cancel(true);
            }
        }
    }

    public static void resetRecipeHashes() {
        recipeCache.clear();
    }
}
