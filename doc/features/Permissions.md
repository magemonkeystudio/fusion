# Permissions Reference

This document provides a complete reference for all Fusion permission nodes, including descriptions, default values, and usage examples.

## Table of Contents

- [Overview](#overview)
- [Player Permissions](#player-permissions)
- [Administrative Permissions](#administrative-permissions)
- [Recipe Permissions](#recipe-permissions)
- [Limit Permissions](#limit-permissions)
- [Queue Permissions](#queue-permissions)
- [Permission Examples](#permission-examples)
- [Default Permissions](#default-permissions)

## Overview

Fusion uses a comprehensive permission system that allows fine-grained control over player access to professions, commands, recipes, and features.

### Permission Categories

- **Player Permissions** - Basic access to commands and features
- **Administrative Permissions** - Server management and admin tools
- **Recipe Permissions** - Control access to specific recipes
- **Limit Permissions** - Set profession and queue limits
- **Queue Permissions** - Control queue system access and limits

## Player Permissions

These permissions control basic player access to Fusion features.

| Permission | Default | Description |
|------------|---------|-------------|
| `fusion.browse` | `true` | Access to `/craft browse` command. When set to `false`, players cannot browse professions |
| `fusion.auto` | `false` | Access to `/craft auto` command for automatic crafting in manual mode |
| `fusion.show` | `true` | Access to `/craft show` command to see ingredient usage |
| `fusion.craftedby` | `false` | When enabled, crafted items are signed with the player's name |

### Usage Examples

```yaml
# LuckPerms examples
/lp group default permission set fusion.browse true
/lp group vip permission set fusion.auto true
/lp user Steve permission set fusion.craftedby true
```

## Administrative Permissions

These permissions are for server administrators and staff members.

| Permission | Default | Description |
|------------|---------|-------------|
| `fusion.admin` | `false` | General admin access including:<br/>• Storage type changes (`/craft storage`)<br/>• Opening GUIs for other players<br/>• Experience and level management |
| `fusion.reload` | `false` | Access to `/craft reload` command |
| `fusion.admin.force` | `false` | Access to all force commands (forcejoin, forceleave, etc.) |

### Administrative Command Permissions

| Permission | Command | Description |
|------------|---------|-------------|
| `fusion.admin` | `/craft storage <type>` | Change storage between SQLite and MySQL |
| `fusion.admin` | `/craft exp <action> <player> <profession> <amount>` | Manage player experience |
| `fusion.admin` | `/craft level <action> <player> <profession> <amount>` | Manage player levels |
| `fusion.reload` | `/craft reload` | Reload plugin configuration |
| `fusion.admin.force` | `/craft forcejoin <player> <profession>` | Force join profession |
| `fusion.admin.force` | `/craft forceleave <player> <profession>` | Force leave profession |
| `fusion.admin.force` | `/craft forcestats <player>` | View any player's stats |
| `fusion.admin.force` | `/craft forcemaster <player> <profession>` | Force master profession |
| `fusion.admin.force` | `/craft forceshow <player>` | Force show ingredient GUI |

## Recipe Permissions

Control access to specific recipes or all recipes.

| Permission | Description |
|------------|-------------|
| `fusion.recipes` | Access to all recipes |
| `fusion.recipe.*` | Access to all recipes (same as above) |
| `fusion.recipe.<recipe_name>` | Access to specific recipe by internal name |

### Recipe Permission Examples

```yaml
# Grant access to all recipes
fusion.recipe.*

# Grant access to specific recipes
fusion.recipe.LeatherHelmet
fusion.recipe.IronChestplate
fusion.recipe.WoodenSword

# Example LuckPerms commands
/lp group blacksmith permission set fusion.recipe.IronSword true
/lp group armorsmith permission set fusion.recipe.DiamondChestplate true
```

## Limit Permissions

Set limits on how many professions players can join simultaneously.

| Permission | Description |
|------------|-------------|
| `fusion.limit.<amount>` | Maximum number of professions a player can join |

### Limit Examples

```yaml
# Allow 1 profession
fusion.limit.1

# Allow 3 professions
fusion.limit.3

# Allow unlimited professions
fusion.limit.999

# LuckPerms examples
/lp group default permission set fusion.limit.2
/lp group vip permission set fusion.limit.5
/lp group admin permission set fusion.limit.999
```

## Queue Permissions

Control access to the queue crafting system and set queue limits.

| Permission | Description |
|------------|-------------|
| `fusion.queue.limit.<amount>` | Global limit for queued recipes across all professions |
| `fusion.queue.<profession>.limit.<amount>` | Profession-specific queue limit |
| `fusion.queue.<profession>.<category>.limit.<amount>` | Category-specific queue limit within a profession |

### Queue Permission Examples

```yaml
# Global queue limits
fusion.queue.limit.5          # Maximum 5 queued items total
fusion.queue.limit.10         # Maximum 10 queued items total

# Profession-specific limits
fusion.queue.armor_smithing.limit.3      # Max 3 items in armor smithing queue
fusion.queue.weapon_smithing.limit.5     # Max 5 items in weapon smithing queue

# Category-specific limits
fusion.queue.weapon_smithing.wooden_weapons.limit.2    # Max 2 wooden weapons queued
fusion.queue.weapon_smithing.iron_weapons.limit.1      # Max 1 iron weapon queued

# LuckPerms examples
/lp group default permission set fusion.queue.limit.3
/lp group vip permission set fusion.queue.limit.10
/lp group premium permission set fusion.queue.armor_smithing.limit.5
```

## Profession Access Permissions

Control which professions players can join and use.

| Permission | Description |
|------------|-------------|
| `fusion.use.*` | Access to join and use all professions |
| `fusion.use.<profession>` | Access to join and use a specific profession |

### Profession Access Examples

```yaml
# Allow all professions
fusion.use.*

# Allow specific professions
fusion.use.armor_smithing
fusion.use.weapon_smithing
fusion.use.alchemy
fusion.use.cooking

# LuckPerms examples - Role-based access
/lp group blacksmith permission set fusion.use.armor_smithing true
/lp group blacksmith permission set fusion.use.weapon_smithing true
/lp group alchemist permission set fusion.use.alchemy true
/lp group chef permission set fusion.use.cooking true
```

## Permission Examples

### Basic Player Setup

```yaml
# Default player permissions
group:
  default:
    permissions:
      - fusion.browse          # Can browse professions
      - fusion.show           # Can see ingredient usage
      - fusion.limit.2        # Can join 2 professions
      - fusion.queue.limit.3  # Can queue 3 items globally
```

### VIP Player Setup

```yaml
# VIP player with enhanced access
group:
  vip:
    permissions:
      - fusion.browse
      - fusion.show
      - fusion.auto           # Can use auto-crafting
      - fusion.craftedby      # Items are signed
      - fusion.limit.4        # Can join 4 professions
      - fusion.queue.limit.8  # Can queue 8 items globally
```

### Staff Member Setup

```yaml
# Staff member with admin access
group:
  moderator:
    permissions:
      - fusion.*              # All player permissions
      - fusion.admin          # Admin commands
      - fusion.reload         # Can reload plugin
      - fusion.limit.999      # Unlimited professions
```

### Administrator Setup

```yaml
# Full administrator access
group:
  admin:
    permissions:
      - fusion.*              # All permissions
      - fusion.admin.force    # Force commands
```

### Profession-Specific Groups

```yaml
# Specialized profession groups
group:
  smiths:
    permissions:
      - fusion.browse
      - fusion.show
      - fusion.use.armor_smithing
      - fusion.use.weapon_smithing
      - fusion.queue.armor_smithing.limit.5
      - fusion.queue.weapon_smithing.limit.5

  crafters:
    permissions:
      - fusion.browse
      - fusion.show
      - fusion.auto
      - fusion.use.alchemy
      - fusion.use.cooking
      - fusion.use.enchanting
```

## Default Permissions

These are the default permission values if not explicitly set:

| Permission | Default Value | Notes |
|------------|---------------|-------|
| `fusion.browse` | `true` | Players can browse by default |
| `fusion.show` | `true` | Players can see ingredient usage |
| `fusion.auto` | `false` | Auto-crafting requires explicit permission |
| `fusion.craftedby` | `false` | Item signing is opt-in |
| `fusion.admin` | `false` | Admin access must be granted |
| `fusion.reload` | `false` | Reload access must be granted |
| `fusion.admin.force` | `false` | Force commands require explicit permission |
| `fusion.use.*` | `false` | Profession access must be granted |
| `fusion.recipe.*` | Based on profession access | Recipe access follows profession access |

## Permission Management Tips

### 1. Use Group-Based Permissions

Instead of setting permissions per player, use permission groups:

```bash
# Create profession-based groups
/lp creategroup blacksmiths
/lp creategroup alchemists
/lp creategroup enchanters

# Assign permissions to groups
/lp group blacksmiths permission set fusion.use.armor_smithing true
/lp group blacksmiths permission set fusion.use.weapon_smithing true

# Add players to groups
/lp user Steve parent add blacksmiths
```

### 2. Set Appropriate Limits

Consider server balance when setting limits:

```yaml
# Conservative limits for competitive servers
fusion.limit.1                    # One profession per player
fusion.queue.limit.2              # Limited queue slots

# Generous limits for creative servers  
fusion.limit.10                   # Multiple professions
fusion.queue.limit.20             # Large queue capacity
```

### 3. Test Permission Changes

Always test permission changes with test accounts:

```bash
# Test as a player
/lp user testplayer permission set fusion.use.armor_smithing true
/lp user testplayer permission check fusion.use.armor_smithing
```

### 4. Use Inheritance

Set up permission inheritance for easier management:

```yaml
# Base group with common permissions
group:
  crafter_base:
    permissions:
      - fusion.browse
      - fusion.show
      - fusion.limit.3

# Specialized groups inherit from base
group:
  smiths:
    inheritance:
      - crafter_base
    permissions:
      - fusion.use.armor_smithing
      - fusion.use.weapon_smithing
```

## Troubleshooting Permissions

### Common Issues

1. **Player Can't Browse Professions**
   - Check: `fusion.browse` permission
   - Default: `true` (should work unless explicitly denied)

2. **Player Can't Join Profession**
   - Check: `fusion.use.<profession>` or `fusion.use.*` permission
   - Check: `fusion.limit.<amount>` if they're at the limit

3. **Player Can't Use Recipes**
   - Check: `fusion.recipe.*` or `fusion.recipe.<recipe>` permission
   - Check: Profession access permissions

4. **Admin Commands Not Working**
   - Check: `fusion.admin` permission for basic admin commands
   - Check: `fusion.admin.force` permission for force commands
   - Check: `fusion.reload` permission for reload command

### Debug Commands

```bash
# Check player's effective permissions
/lp user <player> permission info

# Check specific permission
/lp user <player> permission check fusion.use.armor_smithing

# View group permissions
/lp group <group> permission info
```

## Getting Help

For permission-related issues:

- **Commands Documentation**: [Commands Reference](../Commands.md)
- **Discord Support**: [Join our Discord](https://discord.gg/6UzkTe6RvW)
- **GitHub Issues**: [Report permission bugs](https://github.com/magemonkeystudio/fusion/issues)

---

**Next Recommended Reading**: [Features & Configuration](Features.md) - Learn about profession setup and configuration