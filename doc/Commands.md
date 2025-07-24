# Commands Reference

This document provides a complete reference for all Fusion commands, including player commands, administrative commands, and force commands.

## Table of Contents

- [Player Commands](#player-commands)
- [Administrative Commands](#administrative-commands)
- [Force Commands](#force-commands)
- [Editor Commands](#editor-commands)
- [Command Permissions](#command-permissions)
- [Tab Completion](#tab-completion)

## Player Commands

These commands are available to all players (subject to permissions).

### Basic Profession Commands

#### `/craft browse`
Opens the profession browser GUI where players can see all available professions.

**Permission**: `fusion.browse` (default: true)
**Usage**: `/craft browse`

#### `/craft join <profession>`
Join a specific profession. Players must meet the requirements and pay any costs.

**Permission**: `fusion.use.<profession>` or `fusion.use.*`
**Usage**: `/craft join <profession>`
**Example**: `/craft join armor_smithing`

#### `/craft use <profession> [category]`
Open the crafting GUI for a profession. Optionally specify a category if the profession uses categories.

**Permission**: `fusion.use.<profession>` or `fusion.use.*`
**Usage**: 
- `/craft use <profession>`
- `/craft use <profession>:<category>`

**Examples**: 
- `/craft use armor_smithing`
- `/craft use weapon_smithing:wooden_weapons`

#### `/craft forget <profession>`
Leave a profession. Requires confirmation with `/craft confirm`.

**Permission**: None (any player can leave professions they've joined)
**Usage**: `/craft forget <profession>`
**Example**: `/craft forget armor_smithing`

#### `/craft confirm`
Confirm leaving a profession after using `/craft forget`.

**Permission**: None
**Usage**: `/craft confirm`

#### `/craft stats`
Display your profession statistics including levels, experience, and mastery status.

**Permission**: None
**Usage**: `/craft stats`

### Advanced Player Commands

#### `/craft master <profession>`
Master a profession if you meet the level requirements and have the mastery fee.

**Permission**: None (available to any player who joined the profession)
**Usage**: `/craft master <profession>`
**Example**: `/craft master armor_smithing`

#### `/craft auto`
Toggle automatic crafting mode for manual crafting mechanism.

**Permission**: `fusion.auto`
**Usage**: `/craft auto`

#### `/craft show`
Show recipes that use the item you're currently holding as an ingredient.

**Permission**: `fusion.show`
**Usage**: `/craft show` (while holding an item)

## Administrative Commands

These commands are available to administrators and require specific permissions.

### Plugin Management

#### `/craft reload`
Reload the entire Fusion plugin configuration.

**Permission**: `fusion.reload`
**Usage**: `/craft reload`

#### `/craft storage <type>`
Change the storage type between local (SQLite) and SQL (MySQL).

**Permission**: `fusion.admin`
**Usage**: `/craft storage <type>`
**Options**: `local`, `sql`
**Examples**:
- `/craft storage local` - Switch to SQLite
- `/craft storage sql` - Switch to MySQL

### Player Management

#### `/craft exp <action> <player> <profession> <amount>`
Manage player profession experience.

**Permission**: `fusion.admin`
**Usage**: `/craft exp <action> <player> <profession> <amount>`
**Actions**: `add`, `set`, `take`
**Examples**:
- `/craft exp add Steve armor_smithing 100` - Add 100 exp
- `/craft exp set Steve armor_smithing 500` - Set exp to 500
- `/craft exp take Steve armor_smithing 50` - Remove 50 exp

#### `/craft level <action> <player> <profession> <amount>`
Manage player profession levels.

**Permission**: `fusion.admin`
**Usage**: `/craft level <action> <player> <profession> <amount>`
**Actions**: `add`, `set`, `take`
**Examples**:
- `/craft level add Steve armor_smithing 5` - Add 5 levels
- `/craft level set Steve armor_smithing 10` - Set level to 10
- `/craft level take Steve armor_smithing 2` - Remove 2 levels

## Force Commands

Force commands are powerful administrative tools that bypass normal restrictions and requirements. They're designed for server automation and NPC interactions.

**Global Permission**: `fusion.admin.force`

### `/craft forcejoin <player> <profession>`
Force a player to join a profession without checking requirements or costs.

**Usage**: `/craft forcejoin <player> <profession>`
**Example**: `/craft forcejoin Steve blacksmith`

**Features**:
- Bypasses all join requirements
- Bypasses costs (money, items, experience)
- Bypasses profession limits
- Works with offline players if they have data

### `/craft forceleave <player> <profession>`
Force a player to leave a profession without confirmation prompts.

**Usage**: `/craft forceleave <player> <profession>`
**Example**: `/craft forceleave Steve blacksmith`

**Features**:
- No confirmation required
- Immediately removes profession
- Preserves player data for rejoining

### `/craft forcestats <player>`
Display profession statistics for any player.

**Usage**: `/craft forcestats <player>`
**Example**: `/craft forcestats Steve`

**Shows**:
- All professions the player has joined
- Levels and experience for each profession
- Mastery status
- Join dates and other metadata

### `/craft forcemaster <player> <profession>`
Force a player to master a profession without level or fee requirements.

**Usage**: `/craft forcemaster <player> <profession>`
**Example**: `/craft forcemaster Steve blacksmith`

**Features**:
- Bypasses level requirements
- Bypasses mastery fees
- Immediately grants mastery status
- Unlocks mastery recipes

### `/craft forceshow <player>`
Force the ingredient usage GUI to open for a player based on their held item.

**Usage**: `/craft forceshow <player>`
**Example**: `/craft forceshow Steve`

**Requirements**:
- Target player must be online
- Target player must be holding an item
- Shows recipes that use the held item as ingredient

## Editor Commands

### `/fusion-editor [profession]`
Open the in-game GUI editor for professions and browse configuration.

**Permission**: `fusion.admin` (implied by editor access)
**Usage**:
- `/fusion-editor` - Open main editor
- `/fusion-editor <profession>` - Open specific profession editor

**Examples**:
- `/fusion-editor` - Open editor main menu
- `/fusion-editor armor_smithing` - Edit armor smithing profession

See [Editor Usage](EditorUsage.md) for detailed editor documentation.

## Command Permissions

### Player Permission Nodes

| Permission | Default | Description |
|------------|---------|-------------|
| `fusion.browse` | true | Access to `/craft browse` command |
| `fusion.auto` | false | Access to `/craft auto` command |
| `fusion.show` | true | Access to `/craft show` command |
| `fusion.use.*` | false | Access to all professions |
| `fusion.use.<profession>` | false | Access to specific profession |
| `fusion.limit.<amount>` | - | Maximum number of professions player can join |

### Administrative Permission Nodes

| Permission | Default | Description |
|------------|---------|-------------|
| `fusion.admin` | false | General admin permissions |
| `fusion.reload` | false | Access to reload command |
| `fusion.admin.force` | false | Access to all force commands |

### Recipe and Limitation Permissions

| Permission | Description |
|------------|-------------|
| `fusion.recipes` | Access to all recipes |
| `fusion.recipe.*` | Access to all recipes (same as above) |
| `fusion.recipe.<recipe>` | Access to specific recipe |
| `fusion.limit.<amount>` | Maximum professions limit |
| `fusion.queue.limit.<amount>` | Global queue limit |
| `fusion.queue.<profession>.limit.<amount>` | Profession-specific queue limit |
| `fusion.queue.<profession>.<category>.limit.<amount>` | Category-specific queue limit |

See [Permissions](features/Permissions.md) for the complete permissions reference.

## Tab Completion

All commands support intelligent tab completion:

### Basic Tab Completion

- **Command names** - Type `/craft ` and press Tab to see all available commands
- **Player names** - For admin commands, tab completes online player names
- **Profession names** - Tab completes available profession names based on context

### Advanced Tab Completion

#### Profession Commands
- `/craft use ` - Shows professions you've joined
- `/craft join ` - Shows professions you haven't joined yet
- `/craft forget ` - Shows professions you can leave

#### Admin Commands
- `/craft exp ` - Shows: add, set, take
- `/craft level ` - Shows: add, set, take
- `/craft storage ` - Shows: local, sql

#### Force Commands
- All force commands tab complete player names for the player parameter
- Profession parameters tab complete available profession names

### Category Support

When using professions with categories:
- `/craft use weapon_smithing:` - Tab completes available categories
- Categories appear as `profession:category` in tab completion

## Command Examples

### Common Player Workflows

```bash
# Browse and join a profession
/craft browse
/craft join armor_smithing

# Use the profession
/craft use armor_smithing
/craft stats

# Leave a profession
/craft forget armor_smithing
/craft confirm
```

### Administrator Workflows

```bash
# Give a player experience
/craft exp add Steve armor_smithing 1000

# Force join a player to a profession
/craft forcejoin Steve weapon_smithing

# Check a player's stats
/craft forcestats Steve

# Reload configuration
/craft reload
```

### NPC Integration Example

For NPC plugins like Citizens, you can use force commands:

```bash
# NPC command to join profession
/craft forcejoin %player% blacksmith

# NPC command to show player stats
/craft forcestats %player%
```

## Error Handling

All commands include comprehensive error handling:

- **Permission Errors** - Clear messages about missing permissions
- **Invalid Arguments** - Help text for incorrect command usage
- **Player Not Found** - Specific errors for offline/invalid players
- **Profession Errors** - Clear feedback about profession state issues
- **Requirement Failures** - Detailed information about unmet requirements

## Getting Help

For additional command help:
- Use `/craft` without arguments to see the help message
- Check [Permissions](features/Permissions.md) for permission setup
- See [Features](features/Features.md) for configuration details
- Join our [Discord](https://discord.gg/6UzkTe6RvW) for support

---

**Next Recommended Reading**: [Permissions](features/Permissions.md) - Set up permission systems