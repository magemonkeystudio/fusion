# Getting Started with Fusion

This guide will walk you through installing and setting up Fusion on your Minecraft server, creating your first profession, and understanding the basic concepts.

## Table of Contents

- [Installation](#installation)
- [Dependencies](#dependencies)
- [First Server Startup](#first-server-startup)
- [Understanding Basic Concepts](#understanding-basic-concepts)
- [Your First Profession](#your-first-profession)
- [Player Commands](#player-commands)
- [Next Steps](#next-steps)

## Installation

### 1. Download Required Files

**Required:**
- Download [Fusion](https://github.com/magemonkeystudio/fusion/releases) latest release
- Download [CodexCore](https://github.com/magemonkeystudio/codex/releases) (Required dependency)

**Recommended:**
- Download [Vault](https://www.spigotmc.org/resources/vault.34315/) (For economy features)
- Download [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) (For placeholder support)

### 2. Install Plugins

1. Place `fusion-x.x.x.jar` in your `/plugins` folder
2. Place `codex-x.x.x.jar` in your `/plugins` folder
3. Place any optional dependencies in your `/plugins` folder
4. Restart your server

### 3. Verify Installation

After restart, check your console for:
```
[INFO] [Fusion] Enabling Fusion v1.0.1-R0.19-SNAPSHOT
[INFO] [Fusion] Fusion has been enabled successfully!
```

## Dependencies

### Required Dependencies

| Plugin | Purpose | Download |
|--------|---------|----------|
| CodexCore | Core functionality and utilities | [GitHub](https://github.com/magemonkeystudio/codex/releases) |

### Optional Dependencies

| Plugin | Purpose | Download |
|--------|---------|----------|
| Vault | Economy integration for recipe costs | [SpigotMC](https://www.spigotmc.org/resources/vault.34315/) |
| PlaceholderAPI | Placeholder support for other plugins | [SpigotMC](https://www.spigotmc.org/resources/placeholderapi.6245/) |

### Supported Integrations

Fusion can integrate with these plugins for enhanced conditions and features:

- **mcMMO** - Use mcMMO skill levels as recipe requirements
- **Jobs** - Use Jobs levels as recipe requirements  
- **Fabled** - Use Fabled class levels as recipe requirements
- **AuraSkills** - Use AuraSkills levels as recipe requirements
- **Divinity** - Use custom items from Divinity in recipes

## First Server Startup

When you first start your server with Fusion installed, it will generate:

### Default Configuration Files

```
/plugins/Fusion/
├── config.yml              # Main plugin configuration
├── browse.yml              # Profession browse GUI configuration
├── professions/
│   ├── armor_smithing.yml   # Example armor smithing profession
│   └── weapon_smithing.yml  # Example weapon smithing profession
└── data/
    ├── players.db          # Player data (SQLite by default)
    └── ...
```

### Default Professions

Two example professions are created automatically:

1. **Armor Smithing** - Craft leather helmets, iron chestplates, and more
2. **Weapon Smithing** - Craft wooden swords, crossbows, and various weapons (with categories)

## Understanding Basic Concepts

### Professions

A **profession** is a custom crafting system with:
- Unique recipes that players can craft
- Experience and level progression
- Mastery system for advanced recipes
- Custom GUI interface
- Optional categories to organize recipes

### Crafting Mechanisms

Fusion supports two types of crafting:

#### 1. Queue Mechanism (Background Crafting)
- Players add recipes to a crafting queue
- Items craft automatically in the background
- Players can do other activities while crafting
- Queue slots show progress and allow cancellation

#### 2. Manual Mechanism (Active Crafting)
- Players must stay in the GUI while crafting
- Shows progress bar and requires attention
- Can be automated with `/craft auto` (with permission)

### Recipe Components

Each recipe has:
- **Costs** - Items, money, or experience consumed
- **Conditions** - Requirements that must be met (level, mastery, other plugins)
- **Results** - Items produced, experience gained, commands executed
- **Crafting Time** - How long the recipe takes to complete

## Your First Profession

Let's explore the default armor smithing profession:

### 1. Browse Available Professions

```
/craft browse
```

This opens a GUI showing all available professions. You'll see:
- **Armor Smithing** - Iron chestplate icon
- **Weapon Smithing** - Diamond sword icon

### 2. Join a Profession

Click on "Armor Smithing" or use:
```
/craft join armor_smithing
```

### 3. Open the Profession GUI

```
/craft use armor_smithing
```

This opens the armor smithing interface where you can see:
- Available recipes (leather helmet, iron chestplate, etc.)
- Your current level and experience
- Queue slots (if using queue mechanism)
- Navigation and utility buttons

### 4. Craft Your First Item

1. **Check Requirements** - Hover over the leather helmet recipe
2. **Gather Materials** - Get 5 leather and 2 enchanted books with Unbreaking 3
3. **Start Crafting** - Click on the recipe to begin
4. **Wait or Monitor** - Depending on your mechanism, either wait for the queue or stay in the GUI

### 5. Level Up

As you craft items, you'll gain:
- **Profession Experience** - Shown in the GUI
- **Levels** - Unlock new recipes
- **Access to Mastery** - At level 40 (configurable)

## Player Commands

Here are the essential commands players will use:

### Basic Commands

```bash
/craft browse              # Open profession browser
/craft join <profession>   # Join a profession
/craft use <profession>    # Open profession crafting GUI
/craft stats              # View your profession statistics
/craft forget <profession> # Leave a profession (requires confirmation)
/craft confirm            # Confirm leaving a profession
```

### Advanced Commands

```bash
/craft master <profession> # Master a profession (if eligible)
/craft auto               # Toggle auto-crafting (permission required)
/craft show               # Show recipes using your held item
```

## Next Steps

### For Server Administrators

1. **Read Configuration Guides**:
   - [Features & Configuration](features/Features.md) - Comprehensive configuration guide
   - [Customizations](features/Customizations.md) - Advanced recipe and item setup
   - [Permissions](features/Permissions.md) - Permission system setup

2. **Learn the Editor**:
   - [Editor Usage](EditorUsage.md) - In-game GUI editor tutorial

3. **Explore Advanced Features**:
   - [Commands](Commands.md) - Full command reference including admin commands
   - [API Documentation](FusionAPI.md) - Developer integration

### For Developers

1. **Check out the API**:
   - [API Documentation](FusionAPI.md) - Events, managers, and integration examples

2. **Review Placeholders**:
   - [Placeholders](Placeholders.md) - PlaceholderAPI integration

### Common Next Tasks

1. **Customize Default Professions** - Edit `armor_smithing.yml` and `weapon_smithing.yml`
2. **Create New Professions** - Use `/fusion-editor` or create new YAML files
3. **Configure Economy** - Set up Vault integration for money-based recipes
4. **Set Up Permissions** - Configure permission groups for different profession access
5. **Create Categories** - Organize large profession recipe sets

## Getting Help

- **Discord**: [Join our Discord](https://discord.gg/6UzkTe6RvW) for community support
- **Issues**: [Report bugs on GitHub](https://github.com/magemonkeystudio/fusion/issues)
- **Wiki**: Continue reading the other documentation pages for detailed information

---

**Next Recommended Reading**: [Commands](Commands.md) - Learn all available commands