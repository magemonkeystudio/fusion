[![Build](https://github.com/magemonkeystudio/fusion/actions/workflows/release.yml/badge.svg?branch=main)](https://repo1.maven.org/maven2/studio/magemonkey/fusion/1.0.1-R0.18-SNAPSHOT)
[![Build](https://github.com/magemonkeystudio/fusion/actions/workflows/devbuild.yml/badge.svg?branch=dev)](https://central.sonatype.com/repository/maven-snapshots/studio/magemonkey/fusion/1.0.1-R0.18-SNAPSHOT)
[![Discord](https://dcbadge.vercel.app/api/server/6UzkTe6RvW?style=flat)](https://discord.gg/6UzkTe6RvW)

# Fusion Wiki

Fusion is a powerful custom crafting and profession system for Minecraft servers. Create intricate profession systems with custom recipes, GUI interfaces, and progression mechanics.

## Quick Navigation

- **[Getting Started](doc/GettingStarted.md)** - Installation, setup, and first steps
- **[Commands](doc/Commands.md)** - Complete command reference
- **[Permissions](doc/features/Permissions.md)** - Permission nodes and configuration
- **[Placeholders](doc/Placeholders.md)** - PlaceholderAPI integration
- **[API Documentation](doc/FusionAPI.md)** - Developer API and events
- **[Features & Configuration](doc/features/Features.md)** - Detailed feature documentation
- **[Editor Usage](doc/EditorUsage.md)** - In-game GUI editor guide
- **[Customizations](doc/features/Customizations.md)** - Advanced recipe and item configuration

## What is Fusion?

Fusion allows server administrators to create custom professions with unique crafting recipes, progression systems, and interactive GUI interfaces. Players can join professions, level up through crafting, unlock new recipes, and master their chosen trades.

### Key Features

- **Custom Professions** - Create unlimited profession types with unique recipes and progression
- **Two Crafting Mechanisms** - Queue-based background crafting or manual active crafting
- **Rich GUI Interface** - Fully customizable GUIs with patterns, categories, and navigation
- **Progression System** - Experience, levels, and mastery unlocks
- **Multi-Plugin Integration** - Supports Vault, mcMMO, Jobs, Fabled, AuraSkills, and more
- **PlaceholderAPI Support** - Full placeholder integration for external plugins
- **In-Game Editor** - Create and modify professions without file editing
- **Force Commands** - Administrative tools for server management

### Supported Crafting Mechanisms

1. **Queue Mechanism** - Queue multiple recipes to craft in the background while doing other activities
2. **Manual Mechanism** - Active crafting that requires staying in the GUI with progress bars

## Installation & Setup

1. **Download** Fusion from [our releases](https://github.com/magemonkeystudio/fusion/releases)
2. **Install Dependencies**:
   - [CodexCore](https://github.com/magemonkeystudio/codex) (Required)
   - [Vault](https://www.spigotmc.org/resources/vault.34315/) (Recommended for economy features)
   - [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) (Optional for placeholders)
3. **Place** the plugin in your `/plugins` folder
4. **Restart** your server
5. **Configure** your first profession using `/fusion-editor` or by editing the YAML files

## Quick Start Example

After installation, you can:

1. **Browse Available Professions**: `/craft browse`
2. **Join a Profession**: `/craft join armor_smithing`
3. **Open Profession GUI**: `/craft use armor_smithing`
4. **View Your Stats**: `/craft stats`

## Developer Information

If you wish to use Fusion as a dependency in your projects, Fusion is available through Maven Central or snapshots through Sonatype.

```xml
<repository>
    <id>sonatype</id>
    <url>https://central.sonatype.com/repository/maven-snapshots/</url>
</repository>
...
<dependency>
    <groupId>studio.magemonkey</groupId>
    <artifactId>fusion</artifactId>
    <version>1.0.1-R0.18-SNAPSHOT</version>
</dependency>
```

### API Example

```java
// Get the Fusion API instance
FusionAPI api = Fusion.getInstance();

// Open a profession GUI for a player
api.getProfessionManager().openProfessionGui("armor_smithing", player);

// Get player's profession data
FusionPlayer fusionPlayer = api.getPlayerManager().getPlayer(player);
fusionPlayer.setJoined("weapon_smithing", true);
```

## Support & Community

- **Discord**: [Join our Discord server](https://discord.gg/6UzkTe6RvW)
- **Issues**: [Report bugs on GitHub](https://github.com/magemonkeystudio/fusion/issues)
- **Documentation**: Browse the wiki links above for detailed information

### A huge thanks to our contributors

<a href="https://github.com/magemonkeystudio/fusion/graphs/contributors">
<img src="https://contrib.rocks/image?repo=magemonkeystudio/fusion"  alt=""/>
</a>
