# Force Commands for Fusion

This document describes the administrator force commands implemented to support issue #58.

## Overview

Force commands are admin-only commands that bypass normal profession restrictions and requirements. They are designed to help administrators manage player professions programmatically, especially for NPC interactions and server automation.

## Permission

All force commands require the `fusion.admin.force` permission.

## Commands

### `/fusion forcejoin <player> <profession>`
Forces a player to join a profession without checking requirements or costs.

**Usage:**
- `player`: Target player name (must be online)
- `profession`: Name of the profession to join

**Example:**
```
/fusion forcejoin Steve blacksmith
```

### `/fusion forceleave <player> <profession>`
Forces a player to leave a profession without confirmation prompts.

**Usage:**
- `player`: Target player name (must be online)  
- `profession`: Name of the profession to leave

**Example:**
```
/fusion forceleave Steve blacksmith
```

### `/fusion forcestats <player>`
Shows profession statistics for any player.

**Usage:**
- `player`: Target player name (must be online)

**Example:**
```
/fusion forcestats Steve
```

### `/fusion forcemaster <player> <profession>`
Forces a player to master a profession without level or fee requirements.

**Usage:**
- `player`: Target player name (must be online)
- `profession`: Name of the profession to master

**Example:**
```
/fusion forcemaster Steve blacksmith
```

### `/fusion forceshow <player>`
Forces the ingredient usage GUI to open for a player based on their held item.

**Usage:**
- `player`: Target player name (must be online)

**Example:**
```
/fusion forceshow Steve
```

## Tab Completion

All force commands support tab completion for:
- Command names when typing the first argument
- Online player names for the player argument
- Available profession names for profession arguments

## Error Handling

The commands include comprehensive error handling for:
- Missing permissions
- Invalid syntax/argument count
- Player not found/offline
- Invalid profession names
- Player already has/doesn't have profession
- Player already mastered profession
- No item in hand (for forceshow)

## Implementation Notes

- Commands bypass all normal restrictions including costs, requirements, and confirmations
- Uses the same underlying API as regular commands but with forced parameters
- Maintains consistency with existing command patterns and error messages
- Includes proper permission checks to prevent unauthorized usage