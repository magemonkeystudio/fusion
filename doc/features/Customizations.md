# Advanced Customizations

This guide covers advanced customization options for Fusion, including detailed recipe configuration, item builders, condition systems, and plugin integrations.

## Table of Contents

- [Recipe Configuration](#recipe-configuration)
- [Results Section](#results-section)
- [Costs Section](#costs-section)
- [Conditions Section](#conditions-section)
- [Item Builder System](#item-builder-system)
- [Delayed Commands](#delayed-commands)
- [Plugin Integrations](#plugin-integrations)
- [Advanced Examples](#advanced-examples)

## Recipe Configuration

Recipes are the core of any profession and can be extensively customized with various parameters and sections.

### Basic Recipe Structure

Every recipe follows this basic structure:

```yaml
recipes:
  - name: <recipe_name>              # Internal recipe identifier
    craftingTime: 5                  # Time in seconds to craft
    craftingLimit: 1                 # How many times player can craft this recipe
    category: <category_name>        # Category (if using categories)
    hiding:                          # Recipe visibility options
      noPermission: true             # Hide if player lacks permission
      recipeLimitReached: true       # Hide if crafting limit reached
    results: {...}                   # What the player receives
    costs: {...}                     # What the player must pay
    conditions: {...}                # Requirements that must be met
```

### Recipe Parameters

#### Hiding Options

The `hiding` section controls when recipes are visible to players:

```yaml
hiding:
  noPermission: true              # Hide recipe if player lacks permission
  recipeLimitReached: true        # Hide recipe if player reached crafting limit
```

**Important Notes:**
- Hiding options are completely optional
- They override global configuration settings
- If not needed, remove the entire `hiding` section
- Global settings apply when individual recipe settings are not specified

**Example Use Cases:**
```yaml
# Recipe only visible to VIPs
- name: VIPSword
  hiding:
    noPermission: true            # Hides from players without fusion.recipe.VIPSword
  
# Recipe disappears after crafting once
- name: UniqueItem
  craftingLimit: 1
  hiding:
    recipeLimitReached: true      # Hides after player crafts it once
```

## Results Section

The results section defines what players receive when they complete a recipe.

### Supported Result Types

- **Items** - Physical items added to inventory
- **Vanilla Experience** - Standard Minecraft experience points
- **Profession Experience** - Experience in the current profession
- **Commands** - Execute commands when recipe completes

### Basic Results Example

```yaml
results:
  vanillaExp: 0                    # Vanilla experience gained
  item: CROSSBOW:1                 # Item given (material:amount)
  professionExp: 25                # Profession experience gained
  commands:                        # Commands executed on completion
    - delay: 0
      as: PLAYER
      cmd: give {player} minecraft:stick 1
```

### Advanced Results Examples

#### Multiple Items Result
```yaml
results:
  vanillaExp: 15
  professionExp: 50
  item: DIAMOND_SWORD:1
  commands:
    - delay: 0
      as: CONSOLE
      cmd: give {player} minecraft:diamond 2
    - delay: 20                    # 1 second delay
      as: PLAYER
      cmd: say I crafted a diamond sword!
```

#### Experience Only Result
```yaml
results:
  vanillaExp: 100
  professionExp: 200
  item: AIR:0                      # No physical item
  commands:
    - delay: 0
      as: CONSOLE
      cmd: title {player} title "§6Level Up!"
```

## Costs Section

The costs section defines what players must pay to craft a recipe. These resources are consumed during crafting.

### Supported Cost Types

- **Money** - Economy integration (requires Vault)
- **Vanilla Experience** - Standard Minecraft experience points
- **Items** - Physical items consumed from inventory

### Basic Costs Example

```yaml
costs:
  money: 15.0                      # Money cost (requires Vault, use decimals!)
  exp: 3                           # Vanilla experience cost
  items:                           # Items consumed
    - STICK:3
    - STRING:2
    - FLINT:2
```

### Advanced Costs Examples

#### Complex Item Requirements
```yaml
costs:
  money: 50.0
  exp: 10
  items:
    - DIVINITY_cool_item:1         # Custom item from Divinity
    - STRING:2                     # Vanilla string
    - material: ENCHANTED_BOOK     # Enchanted book with specific enchants
      amount: 1
      enchants:
        unbreaking: 3
    - material: STICK              # Custom stick with name and enchants
      amount: 1
      name: 'Mighty Stick'
      enchants:
        sharpness: 5
```

#### Economy-Heavy Recipe
```yaml
costs:
  money: 1000.0                    # Expensive recipe
  exp: 0                           # No vanilla experience
  items:                           # Minimal physical items
    - PAPER:1
```

### Important Notes for Costs

- **Money amounts must use decimals** (e.g., `15.0` not `15`)
- Zero cost is expressed as `0.0` for money and `0` for experience
- Items use the [Item Builder System](#item-builder-system) for advanced specifications

## Conditions Section

Conditions are requirements that must be met before a recipe can be crafted. Unlike costs, conditions are not consumed.

### Supported Condition Types

- **Profession Level** - Level in current or other professions
- **Mastery** - Whether the profession is mastered
- **Multi-Profession Requirements** - Levels in multiple professions
- **Plugin Integration** - Requirements from other plugins

### Basic Conditions Example

```yaml
conditions:
  professionLevel: 15              # Level 15 in current profession
  mastery: false                   # Mastery not required
  professions: {}                  # No other profession requirements
```

### Multi-Profession Conditions

```yaml
conditions:
  professionLevel: 0               # No level requirement in current profession
  mastery: false                   # Mastery not required
  professions:
    armor_smithing: 5              # Level 5+ in armor smithing
    weapon_smithing: 15            # Level 15+ in weapon smithing
```

### Plugin Integration Conditions

```yaml
conditions:
  professionLevel: 0
  mastery: false
  professions:
    armor_smithing: 5
  mcmmo:                           # mcMMO skill requirements
    archery: 50
  jobs:                            # JobsReborn level requirements
    miner: 25
  fabled:                          # Fabled class requirements
    warrior: 5
  aura_abilities:                  # AuraSkills ability requirements
    fireball: 3
```

### Available Plugin Integrations

| Plugin | Condition Key | Values | Description |
|--------|---------------|--------|-------------|
| [Fusion](../) | `professions` | Any profession name | Levels in other Fusion professions |
| [Fabled](https://www.spigotmc.org/resources/fabled-an-rpg-engine-by-magemonkey-studio-formerly-proskillapi.91913/) | `fabled` | Class IDs | Fabled class levels |
| [mcMMO](https://www.spigotmc.org/resources/official-mcmmo-original-author-returns.64348/) | `mcmmo` | Skill names | mcMMO skill levels |
| [JobsReborn](https://www.spigotmc.org/resources/jobs-reborn.4216/) | `jobs` | Job names | Jobs level requirements |
| [AuraSkills](https://www.spigotmc.org/resources/auraskills.81069/) | `aura_abilities` | Ability names | AuraSkills ability levels |
| [AuraSkills](https://www.spigotmc.org/resources/auraskills.81069/) | `aura_mana_abilities` | Mana ability names | AuraSkills mana ability levels |
| [AuraSkills](https://www.spigotmc.org/resources/auraskills.81069/) | `aura_skills` | Skill names | AuraSkills skill levels |
| [AuraSkills](https://www.spigotmc.org/resources/auraskills.81069/) | `aura_stats` | Stat names | AuraSkills stat requirements |

## Item Builder System

The Item Builder system allows you to create detailed custom items for use in recipes, GUIs, and rewards.

### Basic Item Structure

```yaml
material: OAK_SIGN                 # Base material
amount: 1                          # Quantity
durability: 0                      # Custom durability
unbreakable: false                 # Unbreakable flag
name: '§6Custom Item Name'         # Display name (supports color codes)
lore:                              # Item description
  - ' §7Line 1 of lore'
  - ' §7Line 2 of lore'
flags: []                          # Item flags (HIDE_ENCHANTS, etc.)
enchants: {}                       # Enchantments
data: null                         # Additional metadata
```

### Advanced Item Examples

#### Enchanted Weapon
```yaml
material: DIAMOND_SWORD
amount: 1
durability: 0
unbreakable: true
name: '&6&lLegendary Blade'
lore:
  - '&7A sword of legendary power'
  - '&8Crafted by master smiths'
  - ' '
  - '&eSharpness V'
  - '&eUnbreaking III'
flags:
  - HIDE_ENCHANTS
enchants:
  sharpness: 5
  unbreaking: 3
  fire_aspect: 2
```

#### Custom Armor Piece
```yaml
material: DIAMOND_BOOTS
amount: 1
durability: 40                     # Custom durability (damaged)
unbreakable: false
name: '&6Magic Boots'
lore:
  - '&7Boots imbued with magic'
  - '&7Durability: &c40/429'
  - ' '
  - '&eProtection IV'
  - '&eFeather Falling IV'
flags:
  - HIDE_ATTRIBUTES
enchants:
  protection: 4
  feather_falling: 4
```

#### Enchanted Book Ingredient
```yaml
material: ENCHANTED_BOOK
amount: 3
enchants:
  efficiency: 5
  unbreaking: 3
```

### Item Types

#### Vanilla Items
```yaml
# Simple format
- STONE:5                          # 5 stone blocks
- STICK:1                          # 1 stick
- DIAMOND:3                        # 3 diamonds

# Explicit format (same result)
- VANILLA_STONE:5
- VANILLA_STICK:1
- VANILLA_DIAMOND:3
```

#### Custom Items Using Item Builder
```yaml
- material: DIAMOND_PICKAXE
  amount: 1
  name: '&bMiner\'s Friend'
  enchants:
    efficiency: 5
    unbreaking: 3
```

#### Divinity Integration
```yaml
# Divinity custom items
- DIVINITY_custom_items:mysword:1
- DIVINITY_gems:fire_gem~level:5:2
- DIVINITY_runes:power_rune:1
```

### Divinity Module Support

Fusion integrates with [Divinity](https://www.spigotmc.org/resources/divinity-custom-item-generator-by-magemonkey-studio-formerly-prorpgitems.99713/) custom items:

```
DIVINITY_arrows:[item][~level:<level>][:<amount>]
DIVINITY_consumables:[item][~level:<level>][:<amount>] 
DIVINITY_custom_items:[item][:<amount>] 
DIVINITY_dismantle:[item][~level:<level>][:<amount>] 
DIVINITY_essences:[item][~level:<level>][:<amount>] 
DIVINITY_extractor:[item][~level:<level>][:<amount>] 
DIVINITY_fortify:[item][~level:<level>][:<amount>] 
DIVINITY_gems:[item][~level:<level>][:<amount>] 
DIVINITY_identify:[item][~level:<level>][:<amount>] 
DIVINITY_item_generator:[item][~level:<level>][~material:<type>][:<amount>] 
DIVINITY_magic_dust:[item][~level:<level>][:<amount>] 
DIVINITY_runes:[item][~level:<level>][:<amount>]
```

### Reference Links

- **Enchantments**: [Bukkit Enchantment Reference](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/enchantments/Enchantment.html)
- **Item Flags**: [Bukkit ItemFlag Reference](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/inventory/ItemFlag.html)

## Delayed Commands

Commands can be executed when recipes complete, with configurable delays and execution contexts.

### Command Structure

```yaml
commands:
  - delay: 5                       # Delay in ticks (20 ticks = 1 second)
    as: CONSOLE                    # Executor: PLAYER or CONSOLE
    cmd: say Hello World!          # Command to execute
```

### Command Examples

#### Simple Command
```yaml
commands:
  - delay: 0                       # Execute immediately
    as: PLAYER                     # Execute as the player
    cmd: say I crafted something!
```

#### Multiple Commands
```yaml
commands:
  - delay: 0                       # Immediate
    as: CONSOLE
    cmd: give {player} minecraft:diamond 1
  - delay: 20                      # 1 second later
    as: PLAYER
    cmd: say Thanks for the diamond!
  - delay: 40                      # 2 seconds later
    as: CONSOLE
    cmd: title {player} title "§6Congratulations!"
```

#### Advanced Command Sequence
```yaml
commands:
  - delay: 0
    as: CONSOLE
    cmd: playsound minecraft:entity.player.levelup player {player}
  - delay: 10
    as: CONSOLE
    cmd: particle minecraft:firework {player_location}
  - delay: 20
    as: PLAYER
    cmd: broadcast {player} just crafted something amazing!
```

### Command Placeholders

Commands support various placeholders:

- `{player}` - Player's name
- `{player_location}` - Player's location (for some commands)
- Additional placeholders may be available depending on context

### Execution Context

- **PLAYER** - Command executed as if the player typed it
- **CONSOLE** - Command executed from server console (full permissions)

## Advanced Examples

### Complete Recipe Examples

#### Legendary Weapon Recipe
```yaml
- name: LegendarySword
  craftingTime: 300                # 5 minutes
  craftingLimit: 1                 # Once per player
  category: legendary_items
  hiding:
    noPermission: true
    recipeLimitReached: true
  results:
    vanillaExp: 1000
    professionExp: 500
    item:
      material: DIAMOND_SWORD
      amount: 1
      unbreakable: true
      name: '&6&lExcalibur'
      lore:
        - '&7The legendary sword of kings'
        - '&7Only the worthy may wield it'
        - ' '
        - '&eSharpness X'
        - '&eUnbreaking V'
        - '&eLooting III'
      enchants:
        sharpness: 10
        unbreaking: 5
        looting: 3
    commands:
      - delay: 0
        as: CONSOLE
        cmd: broadcast &6{player} has forged the legendary Excalibur!
      - delay: 20
        as: CONSOLE
        cmd: playsound minecraft:ui.toast.challenge_complete player {player}
  costs:
    money: 10000.0
    exp: 100
    items:
      - DIAMOND:10
      - GOLD_BLOCK:5
      - material: ENCHANTED_BOOK
        amount: 1
        enchants:
          sharpness: 5
      - NETHER_STAR:1
  conditions:
    professionLevel: 50
    mastery: true
    professions:
      enchanting: 40
    mcmmo:
      swords: 1000
```

#### Multi-Stage Crafting Recipe
```yaml
- name: MagicStaff_Stage1
  craftingTime: 60
  results:
    item:
      material: STICK
      name: '&5Enchanted Rod'
      lore: ['&7Stage 1 of Magic Staff']
    professionExp: 50
  costs:
    items:
      - STICK:1
      - LAPIS_LAZULI:5
  conditions:
    professionLevel: 25

- name: MagicStaff_Stage2
  craftingTime: 120
  results:
    item:
      material: BLAZE_ROD
      name: '&5Magic Staff'
      lore: 
        - '&7A powerful magical focus'
        - '&7Channels arcane energies'
    professionExp: 100
  costs:
    items:
      - material: STICK
        name: '&5Enchanted Rod'
        amount: 1
      - ENDER_PEARL:3
      - BLAZE_POWDER:5
  conditions:
    professionLevel: 35
    mastery: false
```

### Complex Profession Setup

#### Master Enchanter Profession
```yaml
name: master_enchanting
icon: ENCHANTING_TABLE
inventoryName: '&5&lMaster Enchanting'
useCategories: true
masteryUnlock: 60
masteryFee: 25000

categories:
  - name: basic_enchants
    icon: BOOK
    order: 1
  - name: advanced_enchants
    icon: ENCHANTED_BOOK
    order: 2
  - name: legendary_enchants
    icon: NETHER_STAR
    order: 3

recipes:
  # Basic enchanted books
  - name: SharpnessBook
    category: basic_enchants
    craftingTime: 30
    results:
      item:
        material: ENCHANTED_BOOK
        enchants:
          sharpness: 3
      professionExp: 25
    costs:
      money: 100.0
      items:
        - BOOK:1
        - LAPIS_LAZULI:3
    conditions:
      professionLevel: 5

  # Advanced enchanted books  
  - name: AdvancedSharpnessBook
    category: advanced_enchants
    craftingTime: 120
    results:
      item:
        material: ENCHANTED_BOOK
        enchants:
          sharpness: 5
      professionExp: 75
    costs:
      money: 500.0
      items:
        - material: ENCHANTED_BOOK
          enchants:
            sharpness: 3
        - DIAMOND:2
        - EXPERIENCE_BOTTLE:5
    conditions:
      professionLevel: 25

  # Legendary enchants
  - name: LegendarySharpnessBook
    category: legendary_enchants
    craftingTime: 300
    craftingLimit: 1
    hiding:
      recipeLimitReached: true
    results:
      item:
        material: ENCHANTED_BOOK
        name: '&6&lLegendary Sharpness'
        lore:
          - '&7Beyond normal enchanting limits'
          - '&7Sharpness X'
        enchants:
          sharpness: 10
      professionExp: 200
      commands:
        - delay: 0
          as: CONSOLE
          cmd: broadcast &6{player} created a legendary enchantment!
    costs:
      money: 50000.0
      items:
        - material: ENCHANTED_BOOK
          enchants:
            sharpness: 5
        - NETHER_STAR:3
        - DRAGON_BREATH:5
    conditions:
      professionLevel: 50
      mastery: true
      mcmmo:
        alchemy: 500
      fabled:
        mage: 30
```

### Integration Examples

#### Economy-Based Profession
```yaml
# High-end trading profession
- name: GoldTrading
  craftingTime: 5
  results:
    professionExp: 10
    commands:
      - delay: 0
        as: CONSOLE
        cmd: eco give {player} 150
  costs:
    items:
      - GOLD_INGOT:5
  conditions:
    professionLevel: 1
```

#### Experience Conversion
```yaml
# Convert profession XP to vanilla XP
- name: XPConversion
  craftingTime: 1
  craftingLimit: 10               # Daily limit
  results:
    vanillaExp: 100
    professionExp: -50             # Spend profession XP
  costs:
    money: 50.0
  conditions:
    professionLevel: 20
```

## Best Practices

### 1. Recipe Balancing

- **Progression Curve**: Make early recipes accessible, late recipes challenging
- **Time Investment**: Balance crafting time with reward value
- **Resource Economy**: Consider server economy when setting costs

### 2. Condition Design

- **Multi-Plugin Integration**: Use plugin conditions to create cross-system progression
- **Mastery Gates**: Reserve best recipes for mastered professions
- **Level Requirements**: Create natural progression through level requirements

### 3. Item Design

- **Clear Naming**: Use descriptive names and lore for custom items
- **Visual Appeal**: Use color codes and formatting for attractive items
- **Unique Identity**: Make special items visually distinct

### 4. Command Integration

- **Server Events**: Use commands to create server-wide events for special crafts
- **Reward Systems**: Integrate with other plugins through commands
- **Player Feedback**: Provide clear feedback through titles, sounds, and messages

## Troubleshooting

### Common Issues

1. **Recipe Not Appearing**
   - Check profession access permissions
   - Verify recipe conditions are met
   - Check hiding configuration

2. **Items Not Working**
   - Verify material names are correct
   - Check enchantment names and levels
   - Ensure proper YAML formatting

3. **Commands Not Executing**
   - Check command syntax
   - Verify placeholder usage
   - Test commands manually first

4. **Plugin Integration Issues**
   - Ensure target plugins are installed
   - Check plugin compatibility versions
   - Verify condition key names

### Debugging Tips

- **Test in Creative**: Use creative mode to test resource requirements
- **Check Console**: Monitor console for error messages
- **Validate YAML**: Use online YAML validators for syntax checking
- **Incremental Testing**: Test one change at a time

## Getting Help

For customization help and advanced configuration:

- **Getting Started**: [Getting Started Guide](../GettingStarted.md)
- **Commands Reference**: [Commands Documentation](../Commands.md)
- **API Integration**: [API Documentation](../FusionAPI.md)
- **Discord Support**: [Join our Discord](https://discord.gg/6UzkTe6RvW)
- **GitHub Issues**: [Report configuration issues](https://github.com/magemonkeystudio/fusion/issues)

---

**Next Recommended Reading**: [Editor Usage](../EditorUsage.md) - Learn to use the in-game editor for easier configuration