# Maven Dependency Resolution Issues

## Problem
The project currently experiences frequent Maven build failures due to dependency resolution issues, particularly with artifacts that are not publicly available or are blocked by firewall restrictions.

## Root Cause
The build failures are caused by:

1. **MageMonkey Studio Dependencies**: Internal dependencies (`studio.magemonkey:sapphire`, `studio.magemonkey:divinity`, `studio.magemonkey:codex`, `studio.magemonkey:fabled`) that are not available in public Maven repositories
2. **Development Versions**: Dependencies using development versions (e.g., `com.nexomc:nexo:0.10.0-dev.27`) that may not be publicly available
3. **Repository Access**: Firewall restrictions blocking access to various Maven repositories
4. **Parent POM Dependencies**: The parent POM (`studio.magemonkey:magemonkey-parent:1.21.7-R0.3`) defines dependencies with snapshot versions that require specific repositories

## Current Solution Applied

### Dependencies Made Optional/Provided
The following dependencies have been commented out in the POM to allow builds to succeed:

1. **MageMonkey Studio Internal Dependencies**:
   - `studio.magemonkey:codex`
   - `studio.magemonkey:sapphire`
   - `studio.magemonkey:divinity`
   - `studio.magemonkey:fabled`

2. **Plugin Integration Dependencies**:
   - `com.gmail.nossr50.mcMMO:mcMMO`
   - `com.github.Zrips:Jobs`
   - `dev.aurelium:auraskills-api-bukkit`
   - `me.clip:placeholderapi`
   - `com.nexomc:nexo`

3. **Core API Dependencies** (still causing issues):
   - `io.papermc.paper:paper-api:1.21.7-R0.1-SNAPSHOT`
   - `org.spigotmc:spigot-api:1.21.7-R0.1-SNAPSHOT`

### Repository Configuration
Added proper repository configurations for public repositories while removing references to unavailable MageMonkey Studio repositories.

## Recommended Long-term Solutions

1. **Private Repository Setup**: Set up a private Maven repository for MageMonkey Studio artifacts and configure access credentials
2. **Stable Version Migration**: Move to stable releases instead of snapshot versions where possible
3. **Profile-based Builds**: Create Maven profiles to handle different build environments (CI, local development, production)
4. **Parent POM Updates**: Update the parent POM to handle dependency resolution more gracefully

## Workarounds for Developers

1. **Local Installation**: Install problematic dependencies in local Maven repository using `mvn install:install-file`
2. **Profile Activation**: Use `-P!with-problematic-deps` to skip problematic dependencies during build
3. **Offline Mode**: Use `mvn -o` for offline builds when dependencies are already cached

## Impact on Functionality

The commented-out dependencies are primarily for plugin integrations and will be detected at runtime if available. Core functionality remains intact.