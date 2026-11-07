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
| Source Jar | 2,500 Source per Jar payment | 10 ticks | 15 |
| Liquid, 1 funded face | 1,000 mB | 60 ticks | 240 |
| Liquid, 2 funded faces | 1,000 mB | 60 ticks | 390 |
| Liquid, 3 funded faces | 1,000 mB | 60 ticks | 541 |
| Liquid, 4 funded faces | 1,000 mB | 60 ticks | 691 |
| Gas, 1 funded face | 60 native chemical units | 60 ticks | 541 |
| Gas, 2 funded faces | 60 native chemical units | 60 ticks | 751 |
| Gas, 3 funded faces | 60 native chemical units | 60 ticks | 931 |
| Gas, 4 funded faces | 60 native chemical units | 60 ticks | 1,141 |

Jar payment attempts have an 80-tick cooldown. Payments provide a 100-tick window, with a three-block search radius including diagonals.

North, east, south and west each have a separate liquid buffer and gas buffer. Top, bottom and unsided Source inputs are rejected; bottom item extraction remains available. Default capacity per face is 1,000 mB liquid and 60 gas units. A face is funded only when it stores a full payment; an empty connected pipe or partial buffer does not raise the tier. Every funded face of the selected resource pays together. For example, three funded liquid faces spend 3,000 mB for one tier-3 boost. The listed growth is the whole boost.

Priority is always gas > liquid > Jar, even with custom intervals. Lower-priority buffers may refill but cannot pay or boost while suppressed. Paid Jar timers expire without renewal during suppression. One shared cooldown survives mode changes, and each pot buys at most one boost per real tick. Paid credit is spent first and retained exactly when the harvest bound or output storage stops further work. A fraction too small to advance growth remains paid credit and can combine with the next scheduled boost.

All values are configurable in `config/sourcepots-server.toml`. An existing world-specific `serverconfig/sourcepots-server.toml` overrides it. Restart the world or server after editing. At normal speed, 20 ticks equal one second.

- Set `liquid.costMbPerSide` and `liquid.bufferCapacityMbPerSide`, or `gas.costUnitsPerSide` and `gas.bufferCapacityUnitsPerSide`. Capacity is independent of cost and must cover one payment.
- Each resource has `oneSide`, `twoSides`, `threeSides` and `fourSides` sections. Each section has independent `boostIntervalTicks`, `growthMinTicks` and `growthMaxTicks` values. Jar has its own payment and growth settings, making nine growth tiers in total.
- `growth.maxHarvestsPerTick` defaults to 16, accepts 1–256, and counts native and Source-assisted harvests together. Additional harvests spend paid growth without repeating natural growth, tick callbacks, fertilizer or export timers. Setting 1 limits the pot to one total harvest per tick.

Each pot saves its per-face buffers, growth credit, cooldown and Jar timers. Missing optional resources remain stored but cannot fund growth. If you reduce buffer capacities, saved amounts above the new limits are clamped on load, with a warning.

### Growth and consumption

At 20 ticks per second, the defaults produce approximately the following wheat yields on dirt. Inputs must remain funded and output storage must have space. Other crops retain their own recipe growth requirements and soil modifiers.

| Input | Wheat per 10 minutes | Consumption per 10 minutes |
| --- | ---: | --- |
| Source Jar | 25 | 375,000 Source |
| Liquid, 1 funded face | 50 | 200 buckets |
| Liquid, 2 funded faces | 75 | 400 buckets |
| Liquid, 3 funded faces | 100 | 600 buckets |
| Liquid, 4 funded faces | 125 | 800 buckets |
| Gas, 1 funded face | 100 | 12,000 native units (12 buckets) |
| Gas, 2 funded faces | 135 | 24,000 native units (24 buckets) |
| Gas, 3 funded faces | 165 | 36,000 native units (36 buckets) |
| Gas, 4 funded faces | 200 | 48,000 native units (48 buckets) |

Liquid costs 1,000 mB per funded face every three seconds. Gas costs 60 native chemical units per funded face every three seconds. Jar power costs 2,500 Source every four seconds. Only the selected resource pays; suppressed resources are not consumed. Rates assume continuous eligible crop growth and full payment availability.

For a supply of 300 liquid buckets per ten minutes, one Source Condenser supports 1.5 liquid faces; three condensers cover all four. An unupgraded Rotary Condensentrator supplying one native gas unit per tick supports one gas face; four cover all four. Machine speed upgrades increase supply.

## Development

`./gradlew runClient` uses the required gameplay mods and development JEI. `./gradlew runFullClient` adds the pinned optional integrations. `./gradlew build` produces one distributable mod jar for either setup.

[Optional integration verification](docs/verification/optional-source-integrations.md) · [Gameplay verification](docs/verification/ars-botany-pots.md) · [Asset attribution](docs/asset-attribution.md)
