# Botany Pots - StarbuncleMania Integration

A Minecraft mod for All the Mods 10: Aeronautics (ATM10Aero), with mod ID `sourcepots`.

The mod adds **61 Ars Hopper Botany Pots**, one upgrade for each Hopper Botany Pot in the pinned Botany Pots release. Each keeps its original color and material and adds a gold rim in block and item models. Ordinary non-hopper Ars pots are outside this release.

The pots retain the upstream menu, soil and crop support, harvest tool, 15 inventory slots (soil, seed, tool, and 12 output/storage slots), hopper export, comparator behavior, waterlogging, and ordinary unpowered growth. Normal player-applied fertilizer keeps its existing effects, item cost, and cooldown, independently of Source boosts.

## Crafting and finding pots

Use this shaped recipe for each variant:

```text
A A
BCB
BBB
```

- `A`: two gold nuggets (`minecraft:gold_nugget`).
- `B`: five Archwood Planks (`c:planks/archwood`).
- `C`: one matching original Hopper Botany Pot.

The result is one matching Ars Hopper Botany Pot. Crafting preserves the variant's color and material; stored crop, inventory, and arbitrary item data are not copied.

All 61 items appear in their own **Ars Botany Pots** Creative category and Creative search. The original Botany Pots category keeps its upstream items. When JEI is available, use its mod-name filter `@integration` to search for the display name `Botany Pots - StarbuncleMania Integration`. Actual Creative and JEI appearance checks remain on the [manual verification checklist](docs/verification/ars-botany-pots.md).

## Source growth

Each pot accepts all three inputs. These are the default costs and intervals; 20 ticks are about one second at normal server speed.

| Input | Cost | Growth boost interval | Supply |
| --- | --- | --- | --- |
| Ars Source | 2,500 Source per payment, attempted every 80 ticks | 10 ticks during a paid window | Ars providers within 3 blocks on each axis, including diagonals |
| Liquid Source | 500 mB per boost | 5 ticks | `sauce:source_fluid` through face-adjacent fluid handlers |
| Gaseous Source | 250 chemical units per boost | 1 tick | `starbunclemania:source_gas` through face-adjacent Mekanism chemical handlers |

A successful Jar payment resets the paid window to 100 ticks. Payments never stack duration. The default payment is 25% of a standard 10,000-capacity Source Jar, regardless of its current contents. Ars Nouveau's standard provider access supports multiple providers and creative jars. Its range is a 7 by 7 by 7 region centered on the pot.

Liquid and gas have separate input-only buffers that coexist. Each holds exactly one configured payment: 500 mB liquid and 250 chemical units gas by default. Partial fills can accumulate; a boost needs a full payment. The pot accepts pushed input on all six faces and pulls from adjacent handlers that allow extraction toward the pot. Diagonal pipe connections do not qualify. Stored liquid remains available while gas is used. When every scheduled boost needs a payment, 20 ticks consume 2,000 mB liquid (two buckets) or 5,000 chemical units gas (five 1,000-unit buckets).

Mekanism measures the gas buffer in chemical units. Retained growth can reduce actual spending.

All six faces share the same liquid buffer, gas buffer, and boost cooldown. Extra connections can supply resources but cannot increase buffer capacity or boost speed beyond the configured interval.

The pot chooses the fully funded input with the shortest configured boost interval. Equal intervals use gas, then liquid, then Jar power. The defaults therefore prefer gas, liquid, and Jar in that order. A shared cooldown permits at most one Source boost per tick, and changing input does not reset that cooldown. While another input is selected, paid Jar time keeps counting down and Jar payments pause; remaining paid time can be used when Jar power is selected again.

Each paid Source boost samples its input mode’s configurable range, inclusive: **Jar 600–800**, **liquid 500–700**, and **gas 400–600 growth ticks**. Retained paid growth keeps its value when the input mode changes. This amount replaces the bonemeal recipe's growth amount for Source boosts. A matching supported bonemeal recipe with positive growth is still required for the current crop and soil. Manual bonemeal keeps its recipe amount, 400 to 600 ticks with the pinned recipe.

Source growth can reach maturity immediately. The upstream ticker harvests on the next tick, with at most one harvest per pot per server tick. Excess paid growth carries into later cycles and is used before another boost is bought. If the retained growth fully matures the crop, the pot takes no new Source and buys no boost. A partially used bank can be followed by one due boost when more growth is needed. Source-assisted harvests skip the upstream five-tick pause; unpowered and manual-bonemeal-only cycles keep it. Source boosts consume no bonemeal items.

Mature wheat on dirt with no harvest tool keeps upstream loot: one wheat and one to four seeds per harvest roll. The planted seed stays in its input slot. Soil and tool modifiers can change hopper yield rolls.

With missing or incompatible crop/soil, a missing, unsupported or nonpositive bonemeal recipe, a blocked harvest, or a mature crop, the pot takes no new Source from suppliers and buys no boost. Short crops can receive Source growth. Existing buffers remain, and already paid Jar time still expires during loaded ticks. Retained growth waits while the same crop is ineligible. Crop or soil changes and breaking the pot discard retained growth; saving and loading preserve it without unloaded catch-up.

Both buffers, paid Jar time, and cooldowns survive save/reload. Unloaded pots do not tick or gain catch-up boosts. Replacing a crop grants no free power. Breaking a pot drops its matching item and follows upstream inventory drops; buffered liquid/gas and paid time are discarded.

## Server configuration

Edit the instance's `config/sourcepots-server.toml`, then restart the world or dedicated server. In the development client, this is `run/config/sourcepots-server.toml`. An existing file at `saves/<world>/serverconfig/sourcepots-server.toml` (local world) or `<level-name>/serverconfig/sourcepots-server.toml` (dedicated server) overrides that instance file. Values must be positive; `jar.searchRange` has a maximum of 16. Growth bounds are inclusive positive integers with `growthMinTicks <= growthMaxTicks` in each mode table; equal bounds give a constant boost.

| Key | Default |
| --- | --- |
| `jar.sourceCost` | 2500 |
| `jar.paymentIntervalTicks` | 80 |
| `jar.poweredDurationTicks` | 100 |
| `jar.boostIntervalTicks` | 10 |
| `jar.searchRange` | 3 |
| `liquid.costMb` | 500 |
| `liquid.boostIntervalTicks` | 5 |
| `gas.costUnits` | 250 |
| `gas.boostIntervalTicks` | 1 |
| `jar.growthMinTicks` | 600 |
| `jar.growthMaxTicks` | 800 |
| `liquid.growthMinTicks` | 500 |
| `liquid.growthMaxTicks` | 700 |
| `gas.growthMinTicks` | 400 |
| `gas.growthMaxTicks` | 600 |

Liquid and gas buffer capacities follow their configured costs. If saved contents exceed a reduced capacity after restart, the contents are clamped to that capacity with a bounded diagnostic.

Existing saved configurations keep explicit resource costs and intervals. The old shared `growth.minTicks` and `growth.maxTicks` keys are replaced by `growthMinTicks` and `growthMaxTicks` in each `[jar]`, `[liquid]` and `[gas]` table. The new keys receive the defaults above; old shared values no longer control growth. Set each mode’s bounds and any resource values you want, then restart the world or server.

## Target and verification status

The target is ATM10Aero 0.8.0, using the released pack's `manifest.json`:

| Component | Target |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.250 |
| Java | 21 |
| Botany Pots | 21.1.44 |
| StarbuncleMania | 1.5.9 |
| Ars Nouveau | 5.13.3 |
| Mekanism | 10.7.19.85 |

Automated verification covers 81 GameTests and 29 unit tests. Development dedicated-server startup and clean shutdown were verified; an actual development client opened and a completed launch exited cleanly. The user confirmed that the Ars pots look okay in game and work with Source Jars, Liquid Source, and Gaseous Source. The updated client loaded JEI and MezzConfig and initialized JEI's runtime. Detailed rendering/gameplay checks, save/reopen, user confirmation of filtering, and full-pack client/server testing remain pending. Development source runs do not prove that the assembled jar works in the pack. See [verification evidence and manual checks](docs/verification/ars-botany-pots.md) and [the compatibility baseline](docs/compatibility.md).

## Get the sources

```sh
git clone --recurse-submodules https://github.com/GaiusRed/BotanyPots-Starbunklemania-Integration.git
cd BotanyPots-Starbunklemania-Integration
```

For an existing clone:

```sh
git submodule update --init --recursive
```

`upstream/` contains reference-only Git submodules pinned to commits. Gradle uses published jars and does not build or package these source trees. Use `git submodule update --remote` only when deliberately changing the baseline.

## Build and run

Install a Java 21 JDK and set `JAVA_HOME` to it. No global Gradle installation is needed.

```sh
./gradlew test runGameTestServer build
./gradlew runClient
./gradlew runServer
```

On Windows, use `gradlew.bat`. Build output is in `build/libs/`; install `sourcepots-0.1.0-dev.jar`. The sources jar is for source inspection. GameTests use the separate `run/gametest` directory.

Development runs load the integration dependency set. The development client is configured with optional JEI 19.64.0.455 (CurseForge file 9205121), with bundled MezzConfig 0.6.6. JEI is absent from the shipped mod jar and is not a required mod dependency. The corrected client launch loaded both mods and initialized JEI's runtime. User confirmation of item filtering remains pending.

NeoForge development servers can bypass the normal EULA prompt and use offline authentication. Keep them local; do not expose a development server to the internet. Read and accept the Minecraft EULA yourself when setting up a normal packaged server.

For pack testing, use a separate ATM10Aero 0.8.0 instance and back up any worlds. Add only the built integration jar to its `mods/` folder on both client and server. The pack already supplies the upstream mods and JEI; avoid duplicate dependency jars.

## Repository

- `src/main/`: registration, Source growth, capabilities, rendering registration, configuration, and development GameTests.
- `src/test/`: deterministic scheduler, settings, and variant tests.
- `src/generated/resources/`: 310 generated recipes, models, loot tables, language, and tag JSON files.
- `upstream/`: pinned upstream source submodules.
- `gradle.properties`: pack-matched dependency file IDs.
- `compatibility/atm10aero-0.8.0.json`: release provenance, artifact hashes, source pins, and development-tool provenance.
- `.github/workflows/build.yml`: Java 21 build on pushes and pull requests.

## Licensing

No project licence has been selected. The metadata uses All Rights Reserved until that decision is made. Upstream projects retain their own licences. See [asset attribution](docs/asset-attribution.md) for the referenced Botany Pots models and this addon's gold rim.
