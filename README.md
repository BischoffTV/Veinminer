# VeinMiner: Ultimate Mining Enhancement

VeinMiner is the ultimate mining enhancement for your Minecraft server! This plugin allows players to mine entire veins of ores, logs, or other blocks simply by holding shift while breaking a block. Perfect for survival servers, Skyblock, or any server looking to enhance the mining experience.

## ✨ Features

- Instant Vein Mining - Break entire ore veins, log clusters, or other block groups with a single block break
- Advanced Progression System - Players level up as they mine, unlocking the ability to mine larger veins
- New Skill System - Upgrade mining efficiency via the in-game GUI
- WorldGuard Integration - Fully compatible with protected regions
- Achievement System - Earn achievements for mining specific blocks, reaching certain levels, and more
- Update Checker - Get notified when a new plugin update is available
- User-Friendly GUI - Intuitive menu system for managing settings and viewing achievements
- Tool Support - Works with all mining tools - pickaxes, axes, shovels, and hoes
- Enchantment Compatible - Full support for Fortune and Silk Touch enchantments
- Balanced Gameplay - Configurable durability impact and hunger cost to maintain game balance
- Discord Integration - Track mining activity with detailed Discord webhook notifications
- Multi-Server Support - Seamless synchronization of player data across multiple servers
- Visual Indicators - Clear visual feedback when VeinMiner is active or inactive
- User-Friendly Commands - Simple commands with tab completion for easy management
- Flexible Permission System - Control who can use the plugin with detailed permission nodes and configurable requirements
  - Global permission toggle for easy setup
  - Tool-specific permission control
  - Command-based permission management
  - Configurable permission requirements per feature
- Economy Integration - Reward players with in-game currency for achievements

## Commands

- `/veinminer` or `/vm` - Open the main VeinMiner GUI
- `/veinminer on` - Enable VeinMiner
- `/veinminer off` - Disable VeinMiner
- `/veinminer toggle` - Toggle VeinMiner on/off
- `/veinminer tool <tooltype>` - Toggle for specific tools
- `/vmtoggle` - Toggle VeinMiner on/off
- `/vmlevel` - Check your current level and progress
- `/vmsetlevel <player> <level>` - Set a player's VeinMiner level (Admin)
- `/vmsync` - Synchronize VeinMiner data across servers (Admin)
- `/veinminerreload` - Reload the configuration (Admin)
- `/veinminerabout` - Show plugin information
- `/veinminerhelp` - Display help information
- `/vmskill` - Open the skill menu
- `/vmadmin [debug|reload|sync|stats|bstats]` - Admin commands for managing the plugin (Admin)

## Permissions

- `veinminer.use` - Allows the player to use VeinMiner
- `veinminer.tool.pickaxe` - Allows the player to use VeinMiner with pickaxes
- `veinminer.tool.axe` - Allows the player to use VeinMiner with axes
- `veinminer.tool.shovel` - Allows the player to use VeinMiner with shovels
- `veinminer.tool.hoe` - Allows the player to use VeinMiner with hoes
- `veinminer.admin` - Allows access to all VeinMiner admin commands (Default: op)

## Placeholders

- `%veinminer_level%` - Shows player's current level
- `%veinminer_experience%` - Shows player's current XP
- `%veinminer_blocks_mined%` - Shows total blocks mined
- `%veinminer_achievements_completed%` - Shows completed achievements
- `%veinminer_top_level_X%` - Shows top players by level (X = position 1-10)
- `%veinminer_top_achievements_X%` - Shows top players by achievements (X = position 1-10)
- `%veinminer_top_blocks_X%` - Shows top players by blocks mined (X = position 1-10)

## ⚙️ Configuration

VeinMiner is highly configurable:
- Set the maximum number of blocks that can be mined at once
- Configure durability impact on tools
- Adjust hunger cost for balance
- Define which blocks can be mined
- Customize the leveling system
- Set up achievements with custom rewards
- Enable/disable Discord logging
- Configure economy integration
- Toggle WorldGuard compatibility
- Enable the update checker for new versions
- Configure permission requirements:
  - Enable/disable permission requirements globally
  - Set up tool-specific permissions
  - Configure command permissions
  - Customize permission nodes for different features

## Automatic Config Updater

**Never worry about missing or outdated config options again!**

Whenever the plugin starts, VeinMiner automatically checks your `config.yml` for all important options. If any required option is missing (for example, after an update or if something was accidentally deleted), it will be added back with a safe default value—**without overwriting your existing settings**.

- Works with all supported and legacy config versions
- Only missing options are added; your customizations remain untouched
- All changes are clearly shown in the server log with prominent warnings and a summary
- Ensures maximum compatibility and prevents plugin errors due to incomplete configs

**Example log output:**
```
[WARNING] [CONFIG UPDATER] Added missing config option: settings.hybrid-mode = false
[WARNING] [CONFIG UPDATER] Added missing config option: settings.hybrid-blacklist with default tree/leaves blocks
[SEVERE] ------------------------------------------------------------
[SEVERE] !!! CONFIG UPDATER: Configuration has been updated !!!
[SEVERE] !!! Missing options have been added with default values. !!!
[SEVERE] !!! Please review your config.yml file for the new settings. !!!
[SEVERE] !!! Check the logs above for details on what was added. !!!
[SEVERE] ------------------------------------------------------------
```
If your config is already complete, you'll see:
```
[INFO] [CONFIG UPDATER] All configuration options are present and up to date.
```

This makes updating and maintaining your server configuration effortless and safe!

## 🛡️ WorldGuard Integration

VeinMiner offers seamless integration with WorldGuard:
- Define regions where VeinMiner is enabled or disabled
- Create individual rules for each region
- Offer players a custom mining experience in different areas of your world
- Configure region-specific mining settings
- Set up protected mining zones
- Create mining arenas with custom rules
- Protect important areas from vein mining
- Customize mining behavior per region

## Achievement & Skill System

- Earn rewards for mining specific blocks
- Gain recognition for reaching milestone levels
- Claim in-game currency and item rewards
- Track progress through an intuitive GUI
- Upgrade skills directly in the GUI
- Compete with other players for achievements

## Getting Started

1. Download and install the plugin
2. Configure the settings in config.yml
3. Set up MySQL for the progression and achievement systems
4. Enable WorldGuard support if needed
5. Hold shift while mining to activate VeinMiner!

## ❓ Support

If you encounter any issues or have suggestions, please visit our GitHub repository, contact us through SpigotMC or ask your question on the Discord.

## Future Plans

- More achievement types and rewards
- Additional customization options
- Enhanced GUI features
- Performance optimizations
- And more based on your feedback!
- Language packs for easy translation and community-contributed localizations
- Extended API for developers and addon support
- Automatic backups of config and database
- Custom mining events (competitions, challenges, seasonal events)
- Fast compatibility updates for new Minecraft versions
