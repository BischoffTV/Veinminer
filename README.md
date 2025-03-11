### VeinMiner

A powerful Minecraft Spigot plugin for version 1.21 that allows players to mine connected blocks of the same type at once. Perfect for efficient mining of ores, logs, and other materials.

## Features

- **Vein Mining**: Mine entire veins of ore, logs, or other blocks with a single block break
- **Progression System**: Level up as you mine to increase the maximum number of blocks you can mine at once
- **Tool Compatibility**: Works with all mining tools (pickaxes, axes, shovels, hoes)
- **Tool Durability**: Configurable durability impact based on the number of blocks mined
- **Hunger System**: Configurable hunger cost for vein mining
- **Enchantment Support**: Full support for Fortune and Silk Touch enchantments
- **Permission System**: Control who can use the plugin
- **MySQL Database**: Store player data and progress in a MySQL database
- **Discord Integration**: Log mining activity to a Discord webhook


## Commands

- `/veinminer` or `/vm` - Toggle VeinMiner on/off
- `/veinminer on` - Enable VeinMiner
- `/veinminer off` - Disable VeinMiner
- `/veinminer toggle` - Toggle VeinMiner on/off
- `/veinminer tool <tooltype>` - Toggle VeinMiner for a specific tool type (pickaxe, axe, shovel, hoe)
- `/veinminer level` - Display your current level, XP, and maximum blocks
- `/veinminerreload` - Reload the plugin configuration
- `/veinminerhelp` - Display help information
- `/veinminerabout` - Display information about the plugin

- ## Permissions

- `veinminer.use` - Permission to use VeinMiner (configurable in config.yml)
- `veinminer.reload` - Permission to reload the plugin configuration


## Configuration

The plugin is highly configurable. Here are some key configuration options:
- settings:
  ` Maximum number ` of blocks that can be mined at once (if level system is disabled)
- max-blocks: 64
  
  ` Durability settings `
- use-durability-multiplier: true
- durability-multiplier: 1.0
  
  ` Hunger settings `
- use-hunger-multiplier: true
- hunger-multiplier: 0.1
  
  ` Debug mode - enables additional logging `
- debug: false
  
  ` List of blocks that can be mined with VeinMiner `
- allowed-blocks:
    - COAL_ORE
    - DIAMOND_ORE
    ` Add more blocks as needed `

` Permission settings `
- permissions:
  - ` Whether to require a permission to use VeinMiner `
  - require-permission: true
  - ` The permission node required to use VeinMiner `
  - use-permission: "veinminer.use"

` Level system settings `
- level-system:
  - ` Whether to enable the level system `
  - enabled: true
  - ` Number of blocks mined to gain 1 XP `
  - blocks-per-xp: 5
  - ` XP required for each level `
  - xp-per-level:
    - 1: 0
    - 2: 100
    - ` More levels... `
  - ` Maximum blocks that can be mined at each level`

- max-blocks-per-level:
    - 1: 8
    - 2: 16
    - ` More levels...`

## Database Setup

The plugin uses MySQL to store player data. Configure your database connection in the config.yml:

# database:
  - ` Whether to use MySQL for storing player data`
  - use-mysql: true
  - host: "localhost"
  - port: 3306
  - database: "veinminer"
  - username: "root"
  - password: "password"
  - table-prefix: "vm_"

## Usage

To use VeinMiner, simply hold shift (sneak) while breaking blocks. The plugin will automatically mine connected blocks of the same type, up to your configured or level-based maximum.

## Technical Details

- Built for Spigot 1.21
- Asynchronous database operations for optimal performance
- Efficient block detection algorithm to minimize server impact
- Comprehensive API for developers to integrate with


## Support

If you encounter any issues or have suggestions for improvements, please open an issue on GitHub.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
