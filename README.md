# RPack
## Supported versions:
- `1.7.10` - `1.20.2`: OK (`bukkit`, `velocity`)
- `1.20.3+`: OK* (`bukkit`, `velocity`)
## Features:
- Clientside, serverside caching
- Position and count selectors (`bukkit`)
## Files and directories
- `./config.yml` - configuration file
- `./lang.yml` - localisation file
- `./Packs/` - packs directory
- `./Packs/<pack_id>.zip` - playlist specific parent resourcepack file
## Commands:
- `/loadpack @n <pack_id>` - update cache
- `/loadpack <playername> <pack_id>` - loads resourcepack to player
### Permissions
- `rpack.loadpack` - load resourcepack, allows `@s` usage
- `rpack.loadpack.other` - load resourcepack for other players
- `rpack.loadpack.update` - update cache, allows `@n` usage
### Selectors <playername>:
- `@n` - update cache
- `@s` - self
- `@p` - nearest
- `@r` - random
- `@a` - all

Argument format (`@p`, `@r`, `@a`): `[arg1,arg2,arg3,...]`
#### Filter by distance and postition
- Selector: `@p`, `@r`, `@a`
- Format: `<arg><operation><double_value>`
- Arg: `dist`, `x`, `y`, `z`
- Operation: `<=`, `<`, `>=`, `>`
#### Limit count
- Selector: `@a`
- Format: `<arg><operation><int_value>`
- Arg: `closer`, `further`, `random`
- Operation: `=`
