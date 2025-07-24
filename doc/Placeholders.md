# Placeholders

Fusion provides comprehensive PlaceholderAPI integration, allowing you to display profession data in other plugins like scoreboard plugins, chat plugins, and GUI plugins.

## Table of Contents

- [Setup](#setup)
- [Available Placeholders](#available-placeholders)
- [Placeholder Examples](#placeholder-examples)
- [Usage in Other Plugins](#usage-in-other-plugins)
- [Common Use Cases](#common-use-cases)
- [Troubleshooting](#troubleshooting)

## Setup

### Requirements

1. **PlaceholderAPI** must be installed on your server
2. **Fusion** must be installed and enabled
3. No additional configuration is required - placeholders are automatically registered

### Installation

1. Download [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)
2. Install both PlaceholderAPI and Fusion
3. Restart your server
4. Verify installation with `/papi list` - you should see `fusion` in the list

### Verification

Test a placeholder to ensure it's working:
```
/papi parse me %fusion_level_armor_smithing%
```

## Available Placeholders

All Fusion placeholders follow the format: `%fusion_<type>_<profession>%`

### Profession Information Placeholders

These placeholders return information about professions themselves (not player-specific).

#### `%fusion_name_<profession>%`
Returns the display name of a profession.

**Example**: `%fusion_name_armor_smithing%` → `"Armor Smithing"`

#### `%fusion_invname_<profession>%`
Returns the inventory name (GUI title) of a profession.

**Example**: `%fusion_invname_weapon_smithing%` → `"&9Weapon Smithing"`

### Player-Specific Placeholders

These placeholders return player-specific profession data. They return `null` if the player hasn't joined the profession.

#### `%fusion_level_<profession>%`
Returns the player's level in a specific profession.

**Example**: `%fusion_level_armor_smithing%` → `"15"`
**Returns**: `null` if player hasn't joined the profession

#### `%fusion_exp_<profession>%`
Returns the player's current experience in a specific profession.

**Example**: `%fusion_exp_armor_smithing%` → `"1250"`
**Returns**: `null` if player hasn't joined the profession

#### `%fusion_mastered_<profession>%`
Returns whether the player has mastered a specific profession.

**Example**: `%fusion_mastered_armor_smithing%` → `"true"` or `"false"`
**Returns**: `null` if player hasn't joined the profession

#### `%fusion_joined_<profession>%`
Returns whether the player has joined a specific profession.

**Example**: `%fusion_joined_armor_smithing%` → `"true"` or `"false"`
**Returns**: `null` if player hasn't joined the profession

## Placeholder Examples

### Basic Examples

```
Player Level: %fusion_level_armor_smithing%
Player Experience: %fusion_exp_armor_smithing%
Mastery Status: %fusion_mastered_armor_smithing%
Profession Name: %fusion_name_armor_smithing%
```

### Conditional Display

Using placeholders with conditional plugins or scripts:

```
# Show level only if player joined the profession
{if %fusion_joined_armor_smithing% == true}
  Armor Smithing Level: %fusion_level_armor_smithing%
{endif}
```

### Multiple Professions

```
=== Your Professions ===
Armor Smithing: Level %fusion_level_armor_smithing% (XP: %fusion_exp_armor_smithing%)
Weapon Smithing: Level %fusion_level_weapon_smithing% (XP: %fusion_exp_weapon_smithing%)
```

### Mastery Display

```
# Show mastery status with color codes
Armor Smithing: {if %fusion_mastered_armor_smithing% == true}&a&lMastered{else}&cNot Mastered{endif}
```

## Usage in Other Plugins

### Scoreboard Plugins

**FeatherBoard Example:**
```yaml
scoreboard:
  title: "&6&lServer Stats"
  lines:
    - "&7Professions:"
    - "&eArmor: &f%fusion_level_armor_smithing%"
    - "&eWeapon: &f%fusion_level_weapon_smithing%"
    - "&7XP: &f%fusion_exp_armor_smithing%"
```

**ScoreboardManager Example:**
```yaml
scoreboards:
  default:
    title: "&6Stats"
    lines:
      - "Level: %fusion_level_armor_smithing%"
      - "XP: %fusion_exp_armor_smithing%"
```

### Chat Plugins

**ChatControl Example:**
```yaml
format:
  default: "&7[Lv.%fusion_level_armor_smithing%] %player%: %message%"
```

**EssentialsChat Example:**
```yaml
format: "&7[AS:%fusion_level_armor_smithing%] {DISPLAYNAME}: {MESSAGE}"
```

### TabList Plugins

**TAB Plugin Example:**
```yaml
header:
  - "&6Welcome to the Server"
  - "&eYour Profession Level: %fusion_level_armor_smithing%"
footer:
  - "&7Mastery Status: %fusion_mastered_armor_smithing%"
```

### GUI Plugins

**DeluxeMenus Example:**
```yaml
menu_items:
  profession_info:
    material: IRON_CHESTPLATE
    display_name: "&6Armor Smithing"
    lore:
      - "&7Level: &f%fusion_level_armor_smithing%"
      - "&7Experience: &f%fusion_exp_armor_smithing%"
      - "&7Mastered: &f%fusion_mastered_armor_smithing%"
```

### Hologram Plugins

**HolographicDisplays Example:**
```yaml
holograms:
  profession_board:
    location: world:100:70:200
    lines:
      - "&6&lProfession Leaderboard"
      - "&e%player_1%: Level %fusion_level_armor_smithing%"
      - "&e%player_2%: Level %fusion_level_armor_smithing%"
```

## Common Use Cases

### 1. Profession Level Display

Display player's profession levels in chat, scoreboard, or nametags:

```
# Chat format showing highest profession level
&7[&6Lv.%fusion_level_armor_smithing%&7] %player%: %message%

# Scoreboard showing multiple professions
Smithing: %fusion_level_armor_smithing%
Weapons: %fusion_level_weapon_smithing%
```

### 2. Mastery Status

Show mastery achievements:

```
# Conditional mastery display
{if %fusion_mastered_armor_smithing% == true}
  &a&l★ MASTER SMITH ★
{else}
  &7Apprentice Smith
{endif}
```

### 3. Progress Tracking

Track and display profession progress:

```
# Experience bar simulation
Armor Smithing XP: %fusion_exp_armor_smithing%/10000
Progress: [████████░░] %fusion_level_armor_smithing%/50
```

### 4. Conditional Access

Use placeholders to control access to features:

```
# Only show certain GUI items if player has mastered profession
{if %fusion_mastered_weapon_smithing% == true}
  # Show special items menu
{endif}
```

### 5. Leaderboards

Create profession-based leaderboards:

```yaml
# Using a leaderboard plugin
leaderboards:
  armor_smithing:
    query: "SELECT player, level FROM fusion_players WHERE profession = 'armor_smithing'"
    display: "&e{rank}. &f{player} &7- Level {level}"
```

## Troubleshooting

### Common Issues

#### Placeholder Returns Null
**Problem**: Placeholder shows as empty or null
**Solutions**:
1. Verify the player has joined the profession
2. Check the profession name is spelled correctly
3. Ensure PlaceholderAPI is installed and working

#### Profession Name Not Found
**Problem**: `%fusion_level_invalid_profession%` returns null
**Solutions**:
1. Check `/craft browse` to see available profession names
2. Use the internal profession name, not the display name
3. Verify the profession exists in your configuration

#### PlaceholderAPI Not Working
**Problem**: Placeholders show as literal text like `%fusion_level_armor_smithing%`
**Solutions**:
1. Install PlaceholderAPI if not already installed
2. Restart the server after installing PlaceholderAPI
3. Check `/papi list` to verify Fusion is registered
4. Verify the plugin you're using supports PlaceholderAPI

### Debug Commands

```bash
# List all registered placeholder expansions
/papi list

# Test a specific placeholder
/papi parse <player> %fusion_level_armor_smithing%

# Check PlaceholderAPI version
/papi version

# Reload PlaceholderAPI
/papi reload
```

### Verification Steps

1. **Check Plugin Status**:
   ```
   /plugins | grep -i "PlaceholderAPI\|Fusion"
   ```

2. **Test Basic Placeholder**:
   ```
   /papi parse me %fusion_joined_armor_smithing%
   ```

3. **Verify Profession Names**:
   ```
   /craft stats
   ```

4. **Check Configuration**:
   - Ensure profession files exist in `/plugins/Fusion/professions/`
   - Verify profession names match file names (without .yml)

## Advanced Usage

### Custom Conditional Logic

Many plugins support conditional placeholders. Here are some advanced examples:

```yaml
# Complex conditional display
display_name: |
  {if %fusion_mastered_armor_smithing% == true}
    &6&lMaster Smith &7(&eLv.%fusion_level_armor_smithing%&7)
  {elseif %fusion_level_armor_smithing% >= 20}
    &e&lExpert Smith &7(&eLv.%fusion_level_armor_smithing%&7)
  {elseif %fusion_joined_armor_smithing% == true}
    &7Apprentice Smith &7(&eLv.%fusion_level_armor_smithing%&7)
  {else}
    &8Not a Smith
  {endif}
```

### Integration with Economy

```yaml
# Show profession value or worth
worth_display: |
  Profession Value: {math %fusion_level_armor_smithing% * 100} coins
  Experience Worth: {math %fusion_exp_armor_smithing% / 10} points
```

## Support

For placeholder-related issues:

1. **Check PlaceholderAPI Documentation**: [PlaceholderAPI Wiki](https://github.com/PlaceholderAPI/PlaceholderAPI/wiki)
2. **Join Our Discord**: [Fusion Support](https://discord.gg/6UzkTe6RvW)
3. **Report Issues**: [GitHub Issues](https://github.com/magemonkeystudio/fusion/issues)

---

**Next Recommended Reading**: [API Documentation](FusionAPI.md) - Developer integration guide