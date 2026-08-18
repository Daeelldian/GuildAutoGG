# GuildAutoGG

A mod that sends gg to your guild chat every time it detects a showoff message.

The mod requires Fabric API to work correctly.

## Remote trigger rules

GuildAutoGG downloads its trigger rules from:

`https://raw.githubusercontent.com/Daeelldian/GuildAutoGG/main/triggers.json`

The file is checked when the client starts and then every 30 minutes. You can therefore add, remove, or change trigger rules on GitHub without rebuilding the mod.

### Editing the triggers

Edit `triggers.json` in the root of the GitHub repository (the same level as `build.gradle` and `README.md`).

Each inner array is one rule. **All** strings in a rule must occur in the Guild chat message for that rule to trigger.

For example:

```json
[
  ["➜"],
  ["WOW!", "Dye"],
  ["TROPHY", "You caught"]
]
```

A rule with one string matches that string. A rule with multiple strings requires all of them to be present.

The mod keeps the previous rules if GitHub is temporarily unavailable. A built-in copy of the current rules is also included as the initial fallback.
