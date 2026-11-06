# Ars Botany Pots

Source-powered upgrades for all 61 Hopper Botany Pot variants. Preserves native crops, soil, tools, harvesting and hopper automation. Unused paid growth carries into later cycles, and Source-assisted harvests bypass the normal harvest pause.

## Dependencies

Minecraft **1.21.1**, NeoForge **21.1.250** and Java **21**.

- Botany Pots
- Ars Nouveau

Install their required libraries: Bookshelf, Prickle, Curios API and GeckoLib. Source Jars work with these mods alone.

Sauce Library enables Liquid Source when `sauce:source_fluid` is registered. Mekanism and StarbuncleMania enable Gaseous Source when `starbunclemania:source_gas` is registered. These integrations are optional. See [compatibility](docs/compatibility.md) for the tested versions.

## Recipe

```text
A A
BCB
BBB
```

- `A`: gold nugget
- `B`: any Archwood Planks
- `C`: matching Hopper Botany Pot

Produces one Ars Hopper Botany Pot of the same material.

## Defaults

| Input | Cost | Boost interval | Growth ticks per boost |
| --- | --- | --- | --- |
| Source Jar | 2,500 Source per payment | 10 ticks | 600–800 |
| Liquid Source | 500 mB per boost | 5 ticks | 500–700 |
| Gaseous Source | 250 chemical units per boost | 1 tick | 400–600 |

Jar payment attempts have an 80-tick cooldown. Payments provide a 100-tick window, with a three-block search radius including diagonals. Liquid and gas accept input on all six faces. All modes share one boost cooldown; harvesting is limited to once per pot per tick.

All values are configurable in `config/sourcepots-server.toml`. An existing world-specific `serverconfig/sourcepots-server.toml` overrides it. Restart the world or server after editing. At normal speed, 20 ticks equal one second.

## Development

`./gradlew runClient` uses the required gameplay mods and development JEI. `./gradlew runFullClient` adds the pinned optional integrations. `./gradlew build` produces one distributable mod jar for either setup.

[Optional integration verification](docs/verification/optional-source-integrations.md) · [Gameplay verification](docs/verification/ars-botany-pots.md) · [Asset attribution](docs/asset-attribution.md)
