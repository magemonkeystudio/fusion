# Player-state persistence

Recipe limits, profession state, and auto-crafting settings are read from SQL when requested. Changes are written immediately. Experience and recipe-limit increments use SQL arithmetic rather than replacing a value loaded earlier in the session. Normal logout no longer writes a player-state snapshot.

Crafting queues retain live objects for Bukkit scheduling and GUI references. Enqueue inserts the row immediately and retains its generated ID. The active item is checkpointed every second; waiting items do not require a write every second. Failed checkpoints remain pending for retry while the queue is loaded. Logout completes its final checkpoints synchronously before unloading, and saving an online player does not cancel their queue task.

Reload preserves the persisted crafting duration, progress, paid experience cost, and insertion order. Completed items remain completed. Offline time is distributed sequentially from the first unfinished item's checkpoint, then the resulting progress is saved so another rejoin cannot apply the same interval again. Disabling offline progress leaves saved progress unchanged.

Collecting a queued craft deletes its completed row and consumes its crafting limit in one database transaction. A failed transaction leaves the row available. Rewards are delivered only after that transaction succeeds. Cancellation similarly requires a successful deletion before refunding. Bukkit inventory changes and commands cannot be part of the SQL transaction: a process crash after commit but before reward delivery remains a delivery-loss window.

Calculated recipe eligibility is no longer shared through the global GUI cache. Rendering and craft attempts evaluate current requirements. Static recipe configuration and live queue objects still remain in memory; this is not a mechanism for synchronizing active queues between multiple servers.

The direct SQL calls run on the server thread where Bukkit state is involved. Indexed lookups and active-item checkpoints limit unnecessary work, but database latency now directly affects those operations. MySQL/MariaDB load testing is needed before rolling out to a busy remote-database installation. Existing corrupted values are not reconstructed by this change.

Regression coverage uses temporary SQLite databases and includes repeated saves, persisted durations/costs/timestamps, queue order and category matching, completed reloads, permanent limits and cooldown expiry, stale-object increments, atomic claims and rollback, profession writes, and offline progression enabled/disabled. It does not substitute for a live Minecraft or MySQL/MariaDB integration test.
