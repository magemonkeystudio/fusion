### Provided Events of Fusion
Important things first: Here are the provided events by Fusion that are existent yet:

| Event                    | Description                                                                                  |
|--------------------------|----------------------------------------------------------------------------------------------|
| `ProfessionJoinEvent`    | Called when a player joins a profession                                                      |
| `ProfessionLeaveEvent`   | Called when a player leaves a profession                                                     |
| `ProfessionGainXpEvent`  | Called when a player gains profession xp                                                     |
| `ProfessionLevelUpEvent` | Called when a player levels up a profession                                                  |
| `ProfessionMasteryEvent` | Called when a player masters a profession                                                    |
| `QueueItemAddedEvent`    | Called when a queue item is added by the player into the queue of a profession               |
| `QueueItemRemovedEvent`  | Called when a queue item is removed by the player from the queue of a profession.            |
| `QueueItemFinishedEvent` | Called when a queue item is finished and taken by the player from the queue of a profession. |

If you're lacking a specific event, let us know so that we can look through it.

## FusionAPI Instance
The comfortable way of handling Fusion is by using the `FusionAPI` instance. This instance is provided by the `Fusion` class and can be accessed by calling `Fusion.getInstance()`.
It provides you with the following functionalities:

### `FusionAPI#getProfessionManager()`
The profession manager is responsible for interacting with professions and guis of players regarding their opened professions.
You can see some examples below:

```java
ProfessionManager professionManager = FusionApi.getProfessionManager();

// Open the profession gui for the player
professionManager.openProfessionGui("myProfession", player);

// Get the profession instance
CraftingTable table = professionManager.getProfession("myProfession");
//... do stuff with the table (profession)

// Get the profession gui registry to handle player guis
ProfessionGuiRegistry guiRegistry = professionManager.getProfessionGui("myProfession");
// Close the profession gui for all players
guiRegistry.closeAll();
// Open the gui for a specific player
guiRegistry.open(player); // and so on
[...]
```

### `FusionAPI#getPlayerManager()`
The player manager is responsible for handling player data and their professions.
All of their data will be stored in sql (mysql) or sqlite and you can modify those here.

You can see some examples below:

```java
import studio.magemonkey.fusion.data.player.FusionPlayer;

PlayerManager playerManager = FusionApi.getPlayerManager();

// Get the player data of a player
FusionPlayer fusionPlayer = playerManager.getPlayer(player);
//... do stuff with the player data
// Like joining a profession:
FusionAPI.getPlayerManager().getPlayer(player).setJoined("myProfession", true);
// Or leaving a profession:
FusionAPI.getPlayerManager().getPlayer(player).setJoined("myProfession", false);
// Mastering a profession:
FusionAPI.getPlayerManager().getPlayer(player).getProfession("myProfession").setMastered(true);


// Save and reload the player data
playerManager.savePlayer(player);

// Save all players
playerManager.saveAllPlayers();

// Remove all players from cache
playerManager.clearAllPlayers();
[...]
```

### `FusionAPI#getEventManager`
The event manager is responsible for handling events and calling them.
Those are usually natively integrated in the plugin, but in case you want to fire them customized, you can use it's methods.

You can see some examples below:

```java
EventManager eventManager = FusionApi.getEventManager();

// Fire a profession join event
eventManager.callProfessionJoinEvent("myProfession", player, /* Required money (Vault)*/ 0.0, /* Required vanilla exp */ 0);

// Fire a profession leave event
eventManager.callProfessionLeaveEvent(FusionApi.getProfessionManager().getProfession("myProfession"), player);

// Fire a profession mastery event
eventManager.callProfessionMasteryEvent(FusionApi.getProfessionManager().getProfession("myProfession"), player, true); // Masters the profession
eventManager.callProfessionMasteryEvent(FusionApi.getProfessionManager().getProfession("myProfession"), player, false); // Removes the mastery from the player
[...]
```