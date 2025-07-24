# Fusion API Documentation

This guide covers the Fusion API for developers who want to integrate with Fusion in their plugins. The API provides access to profession management, player data, events, and more.

## Table of Contents

- [Getting Started](#getting-started)
- [API Overview](#api-overview)
- [Events](#events)
- [Profession Manager](#profession-manager)
- [Player Manager](#player-manager)
- [Event Services](#event-services)
- [Code Examples](#code-examples)
- [Best Practices](#best-practices)

## Getting Started

### Adding Fusion as a Dependency

#### Maven
```xml
<repository>
    <id>sonatype</id>
    <url>https://central.sonatype.com/repository/maven-snapshots/</url>
</repository>

<dependency>
    <groupId>studio.magemonkey</groupId>
    <artifactId>fusion</artifactId>
    <version>1.0.1-R0.18-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

#### Gradle
```gradle
repositories {
    maven {
        url 'https://central.sonatype.com/repository/maven-snapshots/'
    }
}

dependencies {
    compileOnly 'studio.magemonkey:fusion:1.0.1-R0.18-SNAPSHOT'
}
```

### Plugin Dependencies

Add Fusion as a dependency in your `plugin.yml`:

```yaml
depend: [Fusion]
# or
softdepend: [Fusion]
```

## API Overview

### Accessing the API

The main entry point to the Fusion API is through the `Fusion.getInstance()` method:

```java
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.api.FusionAPI;

// Get the API instance
FusionAPI api = Fusion.getInstance();
```

### Main API Components

The FusionAPI provides access to three main managers:

1. **ProfessionManager** - Handle professions and their GUIs
2. **PlayerManager** - Manage player data and profession progress
3. **EventServices** - Trigger custom events programmatically

## Events

Fusion provides several events that you can listen to in your plugins. All events are cancellable where appropriate.

### Available Events

| Event | Description | Cancellable |
|-------|-------------|-------------|
| `ProfessionJoinEvent` | Called when a player joins a profession | Yes |
| `ProfessionLeaveEvent` | Called when a player leaves a profession | Yes |
| `ProfessionGainXpEvent` | Called when a player gains profession experience | Yes |
| `ProfessionLevelUpEvent` | Called when a player levels up a profession | Yes |
| `ProfessionMasteryEvent` | Called when a player masters a profession | Yes |
| `QueueItemAddedEvent` | Called when a queue item is added to a profession queue | Yes |
| `QueueItemRemovedEvent` | Called when a queue item is removed from a profession queue | Yes |
| `QueueItemFinishedEvent` | Called when a queue item is finished and ready for collection | No |

### Event Usage Examples

#### Listening to Profession Events

```java
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import studio.magemonkey.fusion.events.ProfessionJoinEvent;
import studio.magemonkey.fusion.events.ProfessionLevelUpEvent;

public class FusionListener implements Listener {
    
    @EventHandler
    public void onProfessionJoin(ProfessionJoinEvent event) {
        Player player = event.getPlayer();
        String profession = event.getProfession().getName();
        
        // Send welcome message
        player.sendMessage("Welcome to " + profession + "!");
        
        // Cancel the event if needed
        if (someCondition) {
            event.setCancelled(true);
            event.setCancelMessage("You cannot join this profession!");
        }
    }
    
    @EventHandler
    public void onLevelUp(ProfessionLevelUpEvent event) {
        Player player = event.getPlayer();
        String profession = event.getProfession().getName();
        int newLevel = event.getNewLevel();
        
        // Broadcast level up
        Bukkit.broadcastMessage(player.getName() + " reached level " + newLevel + " in " + profession + "!");
        
        // Give rewards
        if (newLevel == 10) {
            player.getInventory().addItem(new ItemStack(Material.DIAMOND, 5));
        }
    }
}
```

#### Queue Event Handling

```java
@EventHandler
public void onQueueItemAdded(QueueItemAddedEvent event) {
    Player player = event.getPlayer();
    String recipeName = event.getRecipe().getName();
    
    player.sendMessage("Started crafting: " + recipeName);
}

@EventHandler
public void onQueueItemFinished(QueueItemFinishedEvent event) {
    Player player = event.getPlayer();
    String recipeName = event.getRecipe().getName();
    
    // Send notification
    player.sendMessage("§a" + recipeName + " is ready for collection!");
    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
}
```

## Profession Manager

The `ProfessionManager` is responsible for handling professions and their GUI interactions.

### Accessing the Profession Manager

```java
ProfessionManager professionManager = Fusion.getInstance().getProfessionManager();
```

### Key Methods

#### Opening Profession GUIs

```java
// Open the profession GUI for a player
professionManager.openProfessionGui("armor_smithing", player);

// Open a specific category in a profession
professionManager.openProfessionGui("weapon_smithing", "wooden_weapons", player);
```

#### Getting Profession Data

```java
// Get a profession instance
CraftingTable profession = professionManager.getProfession("armor_smithing");

if (profession != null) {
    String name = profession.getName();
    String displayName = profession.getInventoryName();
    int masteryLevel = profession.getMasteryUnlock();
    // ... work with profession data
}
```

#### Managing Profession GUIs

```java
// Get the profession GUI registry
ProfessionGuiRegistry guiRegistry = professionManager.getProfessionGui("armor_smithing");

if (guiRegistry != null) {
    // Close the profession GUI for all players
    guiRegistry.closeAll();
    
    // Open the GUI for a specific player
    guiRegistry.open(player);
    
    // Check if a player has the GUI open
    boolean isOpen = guiRegistry.isOpen(player);
}
```

### Profession Manager Examples

```java
public class ProfessionUtils {
    
    public void openProfessionSafely(Player player, String professionName) {
        ProfessionManager manager = Fusion.getInstance().getProfessionManager();
        
        // Check if profession exists
        if (manager.getProfession(professionName) == null) {
            player.sendMessage("§cProfession not found: " + professionName);
            return;
        }
        
        // Open the GUI
        manager.openProfessionGui(professionName, player);
    }
    
    public List<String> getAvailableProfessions() {
        ProfessionManager manager = Fusion.getInstance().getProfessionManager();
        List<String> professions = new ArrayList<>();
        
        // Get all profession names (you'll need to implement this based on actual API)
        // This is a conceptual example
        for (CraftingTable profession : manager.getAllProfessions()) {
            professions.add(profession.getName());
        }
        
        return professions;
    }
}
```

## Player Manager

The `PlayerManager` handles player-specific profession data including levels, experience, and mastery status.

### Accessing the Player Manager

```java
PlayerManager playerManager = Fusion.getInstance().getPlayerManager();
```

### Working with Player Data

#### Getting Player Information

```java
import studio.magemonkey.fusion.data.player.FusionPlayer;

// Get player data
FusionPlayer fusionPlayer = playerManager.getPlayer(player);

if (fusionPlayer != null) {
    // Check if player has joined a profession
    boolean hasJoined = fusionPlayer.hasProfession("armor_smithing");
    
    if (hasJoined) {
        // Get profession-specific data
        Profession playerProfession = fusionPlayer.getProfession("armor_smithing");
        int level = playerProfession.getLevel();
        int experience = playerProfession.getExp();
        boolean isMastered = playerProfession.isMastered();
        boolean isJoined = playerProfession.isJoined();
    }
}
```

#### Modifying Player Data

```java
// Join a profession
fusionPlayer.setJoined("armor_smithing", true);

// Leave a profession
fusionPlayer.setJoined("armor_smithing", false);

// Set mastery status
fusionPlayer.getProfession("armor_smithing").setMastered(true);

// Modify experience (if such methods exist)
Profession profession = fusionPlayer.getProfession("armor_smithing");
// Note: The exact methods for modifying XP/levels depend on the actual API
```

#### Saving Player Data

```java
// Save a specific player's data
playerManager.savePlayer(player);

// Save all players' data
playerManager.saveAllPlayers();

// Clear player data from cache
playerManager.clearAllPlayers();
```

### Player Manager Examples

```java
public class PlayerProfessionUtils {
    
    public void giveProfessionXP(Player player, String profession, int amount) {
        PlayerManager manager = Fusion.getInstance().getPlayerManager();
        FusionPlayer fusionPlayer = manager.getPlayer(player);
        
        if (fusionPlayer != null && fusionPlayer.hasProfession(profession)) {
            // Use EventServices to properly give XP (triggers events)
            Fusion.getInstance().getEventServices().gainExperience(
                profession, player, amount
            );
        }
    }
    
    public Map<String, Integer> getPlayerProfessionLevels(Player player) {
        PlayerManager manager = Fusion.getInstance().getPlayerManager();
        FusionPlayer fusionPlayer = manager.getPlayer(player);
        Map<String, Integer> levels = new HashMap<>();
        
        if (fusionPlayer != null) {
            for (Profession profession : fusionPlayer.getProfessions()) {
                levels.put(profession.getName(), profession.getLevel());
            }
        }
        
        return levels;
    }
    
    public boolean hasRequiredLevel(Player player, String profession, int requiredLevel) {
        PlayerManager manager = Fusion.getInstance().getPlayerManager();
        FusionPlayer fusionPlayer = manager.getPlayer(player);
        
        if (fusionPlayer != null && fusionPlayer.hasProfession(profession)) {
            return fusionPlayer.getProfession(profession).getLevel() >= requiredLevel;
        }
        
        return false;
    }
}
```

## Event Services

The `EventServices` class allows you to trigger Fusion events programmatically, which is useful for custom profession interactions.

### Accessing Event Services

```java
EventServices eventServices = Fusion.getInstance().getEventServices();
```

### Available Event Methods

#### Profession Management

```java
// Make a player join a profession
eventServices.joinProfession("armor_smithing", player, 0.0, 0); // Required money and vanilla exp

// Make a player leave a profession
CraftingTable profession = Fusion.getInstance().getProfessionManager().getProfession("armor_smithing");
eventServices.leaveProfession(profession, player);

// Master a profession
eventServices.masterProfession(profession, player, true); // true = master, false = remove mastery

// Remove mastery
eventServices.masterProfession(profession, player, false);
```

#### Experience and Leveling

```java
// Give profession experience
eventServices.gainExperience("armor_smithing", player, 100);

// Level up a profession (if such method exists)
// Note: This depends on the actual API implementation
```

### Event Services Examples

```java
public class CustomProfessionRewards {
    
    public void rewardPlayerProfession(Player player, String profession, int xpAmount) {
        EventServices eventServices = Fusion.getInstance().getEventServices();
        
        // Give XP through the event system (triggers ProfessionGainXpEvent)
        eventServices.gainExperience(profession, player, xpAmount);
        
        player.sendMessage("§aYou gained " + xpAmount + " " + profession + " experience!");
    }
    
    public void forceJoinProfession(Player player, String profession) {
        EventServices eventServices = Fusion.getInstance().getEventServices();
        
        try {
            // Join profession without costs
            eventServices.joinProfession(profession, player, 0.0, 0);
            player.sendMessage("§aYou have been granted access to " + profession + "!");
        } catch (Exception e) {
            player.sendMessage("§cFailed to join profession: " + e.getMessage());
        }
    }
    
    public void grantMastery(Player player, String professionName) {
        ProfessionManager professionManager = Fusion.getInstance().getProfessionManager();
        EventServices eventServices = Fusion.getInstance().getEventServices();
        
        CraftingTable profession = professionManager.getProfession(professionName);
        if (profession != null) {
            eventServices.masterProfession(profession, player, true);
            player.sendMessage("§6You have mastered " + professionName + "!");
        }
    }
}
```

## Code Examples

### Complete Integration Example

Here's a comprehensive example showing how to create a plugin that integrates with Fusion:

```java
package com.yourplugin.fusion;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import studio.magemonkey.fusion.Fusion;
import studio.magemonkey.fusion.api.FusionAPI;
import studio.magemonkey.fusion.data.player.FusionPlayer;
import studio.magemonkey.fusion.events.ProfessionJoinEvent;
import studio.magemonkey.fusion.events.ProfessionLevelUpEvent;

public class FusionIntegration extends JavaPlugin implements Listener {
    
    private FusionAPI fusionAPI;
    
    @Override
    public void onEnable() {
        // Check if Fusion is available
        if (!getServer().getPluginManager().isPluginEnabled("Fusion")) {
            getLogger().severe("Fusion not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Get the Fusion API
        this.fusionAPI = Fusion.getInstance();
        
        // Register events
        getServer().getPluginManager().registerEvents(this, this);
        
        getLogger().info("Successfully integrated with Fusion!");
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (command.getName().equalsIgnoreCase("professioninfo")) {
            showProfessionInfo(player);
            return true;
        }
        
        return false;
    }
    
    private void showProfessionInfo(Player player) {
        FusionPlayer fusionPlayer = fusionAPI.getPlayerManager().getPlayer(player);
        
        if (fusionPlayer == null) {
            player.sendMessage("§cNo profession data found!");
            return;
        }
        
        player.sendMessage("§6=== Your Professions ===");
        
        for (Profession profession : fusionPlayer.getProfessions()) {
            if (profession.isJoined()) {
                String masteryStatus = profession.isMastered() ? "§a[MASTERED]" : "§7[Not Mastered]";
                player.sendMessage(String.format("§e%s §7- Level %d (XP: %d) %s",
                    profession.getName(),
                    profession.getLevel(),
                    profession.getExp(),
                    masteryStatus
                ));
            }
        }
    }
    
    // Event handlers
    @EventHandler
    public void onProfessionJoin(ProfessionJoinEvent event) {
        Player player = event.getPlayer();
        String profession = event.getProfession().getName();
        
        // Give welcome bonus
        fusionAPI.getEventServices().gainExperience(profession, player, 50);
        player.sendMessage("§aWelcome to " + profession + "! Here's 50 bonus XP to get you started!");
    }
    
    @EventHandler
    public void onProfessionLevelUp(ProfessionLevelUpEvent event) {
        Player player = event.getPlayer();
        String profession = event.getProfession().getName();
        int newLevel = event.getNewLevel();
        
        // Give rewards every 5 levels
        if (newLevel % 5 == 0) {
            player.sendMessage("§6Milestone reached! Level " + newLevel + " in " + profession + "!");
            // Give additional rewards here
        }
    }
}
```

### Custom Recipe Condition Example

```java
public class CustomRecipeChecker {
    
    public boolean canPlayerCraftRecipe(Player player, String profession, String recipeName) {
        FusionPlayer fusionPlayer = Fusion.getInstance().getPlayerManager().getPlayer(player);
        
        if (fusionPlayer == null || !fusionPlayer.hasProfession(profession)) {
            return false;
        }
        
        Profession playerProfession = fusionPlayer.getProfession(profession);
        
        // Custom conditions
        if (profession.equals("armor_smithing")) {
            // Require level 10 for advanced recipes
            if (recipeName.contains("Diamond") && playerProfession.getLevel() < 10) {
                return false;
            }
            
            // Require mastery for legendary recipes
            if (recipeName.contains("Legendary") && !playerProfession.isMastered()) {
                return false;
            }
        }
        
        return true;
    }
}
```

## Best Practices

### 1. Null Checking

Always check for null values when working with the API:

```java
FusionPlayer fusionPlayer = playerManager.getPlayer(player);
if (fusionPlayer != null && fusionPlayer.hasProfession("armor_smithing")) {
    // Safe to proceed
    Profession profession = fusionPlayer.getProfession("armor_smithing");
    // ... work with profession
}
```

### 2. Event Handling

When listening to Fusion events, consider performance and edge cases:

```java
@EventHandler(priority = EventPriority.HIGH)
public void onProfessionJoin(ProfessionJoinEvent event) {
    // Check if event is already cancelled
    if (event.isCancelled()) {
        return;
    }
    
    // Your logic here
    
    // Cancel with a reason if needed
    if (someCondition) {
        event.setCancelled(true);
        event.setCancelMessage("Custom cancel reason");
    }
}
```

### 3. Data Persistence

Save player data when making changes:

```java
// After modifying player profession data
fusionPlayer.setJoined("armor_smithing", true);

// Always save after modifications
Fusion.getInstance().getPlayerManager().savePlayer(player);
```

### 4. Thread Safety

Be aware of thread safety when working with player data:

```java
// If working from async context, schedule sync task
if (!Bukkit.isPrimaryThread()) {
    Bukkit.getScheduler().runTask(plugin, () -> {
        // Work with Fusion API on main thread
        fusionAPI.getProfessionManager().openProfessionGui("armor_smithing", player);
    });
}
```

### 5. Plugin Dependencies

Handle plugin loading order properly:

```java
@Override
public void onEnable() {
    // Check if Fusion is loaded
    if (!Bukkit.getPluginManager().isPluginEnabled("Fusion")) {
        getLogger().warning("Fusion not found - some features will be disabled");
        return;
    }
    
    // Initialize Fusion integration
    initializeFusionIntegration();
}

private void initializeFusionIntegration() {
    try {
        this.fusionAPI = Fusion.getInstance();
        getLogger().info("Successfully integrated with Fusion API");
    } catch (Exception e) {
        getLogger().severe("Failed to integrate with Fusion: " + e.getMessage());
    }
}
```

## Getting Help

### Documentation

- **[Getting Started](GettingStarted.md)** - Basic setup and usage
- **[Commands](Commands.md)** - Command reference
- **[Placeholders](Placeholders.md)** - PlaceholderAPI integration

### Support Channels

- **Discord**: [Join our Discord server](https://discord.gg/6UzkTe6RvW)
- **GitHub Issues**: [Report API issues](https://github.com/magemonkeystudio/fusion/issues)
- **GitHub Discussions**: [Ask questions](https://github.com/magemonkeystudio/fusion/discussions)

### Contributing

If you encounter API limitations or bugs:

1. Check existing issues on GitHub
2. Create a detailed issue report with code examples
3. Consider submitting a pull request with improvements

---

**Next Steps**: Explore the [Placeholders](Placeholders.md) documentation for PlaceholderAPI integration or check out [Commands](Commands.md) for admin tools.