# Ars Botany Pots

Source-powered upgrades for all 61 Hopper Botany Pot variants. Preserves native crops, soil, tools, harvesting and hopper automation. Unused paid growth carries into later cycles, and Source-assisted harvests bypass the normal harvest pause. Growth uses the active pot recipe's required ticks and soil modifiers. Source also requires a matching supported positive Botany Pots bonemeal recipe; world-only fertilizer restrictions are not added.

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

| Input | Cost per participating face | Boost interval | Aggregate growth ticks per boost |
| --- | --- | --- | --- |
| Source Jar | 2,500 Source per Jar payment | 10 ticks | 600–800 |
| Liquid, 1 funded face | 500 mB | 5 ticks | 500–700 |
| Liquid, 2 funded faces | 500 mB | 5 ticks | 1,000–1,400 |
| Liquid, 3 funded faces | 500 mB | 5 ticks | 1,500–2,100 |
| Liquid, 4 funded faces | 500 mB | 5 ticks | 2,000–2,800 |
| Gas, 1 funded face | 250 native chemical units | 1 tick | 400–600 |
| Gas, 2 funded faces | 250 native chemical units | 1 tick | 800–1,200 |
| Gas, 3 funded faces | 250 native chemical units | 1 tick | 1,200–1,800 |
| Gas, 4 funded faces | 250 native chemical units | 1 tick | 1,600–2,400 |

Jar payment attempts have an 80-tick cooldown. Payments provide a 100-tick window, with a three-block search radius including diagonals.

North, east, south and west each have a separate liquid buffer and gas buffer. Top, bottom and unsided Source inputs are rejected; bottom item extraction remains available. Default capacity per face is 500 mB liquid and 250 gas units. A face is funded only when it stores a full payment; an empty connected pipe or partial buffer does not raise the tier. Every funded face of the selected resource pays together. For example, three funded liquid faces spend 1,500 mB for one tier-3 boost. The listed growth is the whole boost.

Priority is always gas > liquid > Jar, even with custom intervals. Lower-priority buffers may refill but cannot pay or boost while suppressed. Paid Jar timers expire without renewal during suppression. One shared cooldown survives mode changes, and each pot buys at most one boost per real tick. Paid credit is spent first and retained exactly when the harvest bound or output storage stops further work. A fraction too small to advance growth remains paid credit and can combine with the next scheduled boost.

All values are configurable in `config/sourcepots-server.toml`. An existing world-specific `serverconfig/sourcepots-server.toml` overrides it. Restart the world or server after editing. At normal speed, 20 ticks equal one second.

- Set `liquid.costMbPerSide` and `liquid.bufferCapacityMbPerSide`, or `gas.costUnitsPerSide` and `gas.bufferCapacityUnitsPerSide`. Capacity is independent of cost and must cover one payment.
- Each resource has `oneSide`, `twoSides`, `threeSides` and `fourSides` sections. Each section has independent `boostIntervalTicks`, `growthMinTicks` and `growthMaxTicks` values. Jar keeps its existing settings, making nine growth tiers in total.
- `growth.maxHarvestsPerTick` defaults to 16, accepts 1–256, and counts native and Source-assisted harvests together. Additional harvests spend paid growth without repeating natural growth, tick callbacks, fertilizer or export timers. Setting 1 limits the pot to one total harvest per tick.

This unpublished revision replaces the old shared-buffer configuration and Source save state. Old liquid/gas cost, interval and growth keys do not control the new tiers. Previous Source buffers, credit and timers reset on load; inherited crop and inventory data remain. New saves retain each face, shared cooldown, Jar timers and paid credit. Missing optional resources remain dormant and cannot fund growth. Loading clamps quantities to current capacities, with one warning per restoration if capacity is exceeded.

The defaults await the user's ATM10 Aero measurements. The user's installed Allthemodium recipe, soils and pack behavior remain unverified; source research is recorded in the [approved design](docs/superpowers/specs/2026-11-07-per-side-source-design.md).

## Development

`./gradlew runClient` uses the required gameplay mods and development JEI. `./gradlew runFullClient` adds the pinned optional integrations. `./gradlew build` produces one distributable mod jar for either setup.

[Optional integration verification](docs/verification/optional-source-integrations.md) · [Gameplay verification](docs/verification/ars-botany-pots.md) · [Asset attribution](docs/asset-attribution.md)
