# Ars Botany Pots design

Status: Approved by the user on 2026-11-03. Implementation planning requested; product code has not started.

## Intent

Add Source-powered upgrades of Hopper Botany Pots for ATM10Aero 0.8.0. Ordinary pots grow slowly; Ars pots spend Source to add repeated bonemeal-equivalent growth while retaining normal hopper behavior. Existing fertilizer use remains available alongside Source boosts.

## Goals and constraints

- Preserve each input Hopper Botany Pot's color and material. Add a gold rim to both its placed model and inventory appearance.
- Retain hopper inventory, crop/soil support, harvesting, tool handling, output, comparator behavior, waterlogging, and normal unpowered growth.
- Support Ars Source, Liquid Source, and Gaseous Source in the same pot.
- Prefer the fastest usable input: gas, then liquid, then Jar power.
- Make resource costs and boost intervals easy to configure on the server.
- Target the pinned dependencies in `docs/compatibility.md`: Minecraft 1.21.1, NeoForge 21.1.250, Java 21, Botany Pots 21.1.44, Ars Nouveau 5.13.3, StarbuncleMania 1.5.9, and Mekanism 10.7.19.85.
- Use existing resources: `sauce:source_fluid` and `starbunclemania:source_gas`. Do not register competing Source types.

Non-goals: ordinary non-hopper Ars variants, new crops, new Source generation/conversion, changes to upstream pots, a custom growth multiplier for existing fertilizers, or compatibility claims for untested pack versions.

Failure conditions include loss of hopper features, consuming resources without valid crop and soil, stockpiling multiple fluid/chemical operations, duplicate boosts from concurrent modes, and changing upstream fertilizer behavior.

## Approaches considered

1. **Extend the existing Botany Pots classes. Selected by the user.** Reuse hopper growth, inventory, harvesting, interaction, and rendering support. Add dedicated Ars blocks/block entities and Source handling. This minimizes duplicated behavior, but requires compatibility checks when upgrading Botany Pots.
2. **Independent pot implementation. Not selected.** Offers more control but duplicates recipe, inventory, harvest, and rendering behavior. It makes keeping parity with Hopper Botany Pots harder.

No upstream source edits or global mixins are planned. If inspection during implementation reveals an extension blocker, return to the design rather than silently replacing this approach.

## Confirmed gameplay

### Default operating modes

| Mode | Cost | Payment interval | Growth boost interval | Powered duration |
| --- | --- | --- | --- | --- |
| Ars Source | 2,000 Source, equal to 20% of a standard 10,000-capacity Jar | 80 ticks | 20 ticks | Each successful payment resets remaining duration to 100 ticks |
| Liquid Source | 250 mB | 10 ticks | 10 ticks, with payment | One operation |
| Gaseous Source | 125 chemical units, described as 125 mB in the request | 5 ticks | 5 ticks, with payment | One operation |

Use Mekanism's native chemical quantity API for gas; do not model it as a fluid.

Jar power never accumulates beyond its configured duration. A failed renewal leaves any previously paid time available until it expires. Without power, the pot still grows, harvests, and exports at ordinary Hopper Botany Pot speed.

### Bonemeal-equivalent growth

The pinned recipe `upstream/BotanyPots/common/src/main/resources/data/botanypots/recipe/minecraft/fertilizer/bone_meal.json` specifies a random 400–600 growth-tick increase.

`BasicFertilizer.apply()` caps growth at `requiredGrowthTicks - 20` and only applies when required growth exceeds 20 ticks. At that cap it consumes no fertilizer. Natural growth completes the final ticks; `BotanyPotBlockEntity.tickPot()` harvests eligible hopper crops and resets growth for the next cycle.

Source boosts must follow that growth amount and cap. They do not spawn or bonemeal world-placed crop blocks and do not consume bonemeal items. No Source payment occurs if a boost cannot add growth, or if crop and soil are absent or incompatible.

Source scheduling must remain independent of the upstream fertilizer cooldown, whose default is 20 ticks. Normal player-applied fertilizer keeps its existing recipe, effects, item consumption, and cooldown. Source and ordinary fertilizer can both contribute growth; do not add an unrequested multiplier to existing fertilizer recipes.

### Source Jar access

Search within three blocks on each axis, including diagonals: a 7×7×7 region centered on the pot. Use Ars Nouveau's standard `SourceUtil` behavior rather than a custom jar-only scanner.

The pinned helper combines available Source from multiple providers, supports creative jars and registered special providers, avoids unloaded positions, and provides Source-transfer particles. Its multi-provider extraction attempts to return extracted amounts if the total payment cannot be met. Use the standard multi-provider helper and preserve its semantics rather than promising stronger transactions than upstream supplies.

Reference users include the Imbuement Chamber (range 2), Whirlisprig (5), Drygmy (7), and Enchanting Apparatus (10). Three is this mod's chosen range, not a universal Ars default.

### Liquid and chemical access

Only the six face-adjacent blocks qualify. Diagonal blocks do not.

- Accept pushed input on all six faces through standard NeoForge fluid and Mekanism chemical capabilities.
- Actively pull from adjacent handlers that permit extraction on the face touching the pot.
- Accept only the matching existing Source resource. Respect sided access and extraction restrictions.
- Keep separate liquid and gas buffers. They coexist.
- Liquid capacity equals one configured liquid payment; gas capacity equals one configured gas payment. Defaults are 250 mB and 125 chemical units.
- Partial fills can accumulate up to one payment. A boost requires a full payment; never charge for a partial operation.
- Stored liquid remains when gas takes priority.

## Proposed defaults requiring design approval

These settle cases not explicitly decided in conversation. They are implementation proposals, not claims of prior user agreement.

### Scheduling and switching

Run the upstream ordinary pot tick once per server tick, then process Source growth. A Source boost reaching the cap leaves upstream ticking to finish maturation and harvest.

Use one shared boost cooldown to prevent gas, liquid, and Jar boosts stacking on the same tick. Choose the fastest fully funded mode by its configured boost interval; defaults give gas, liquid, then Jar priority. Break equal-interval ties in that same order. After a boost, set the next eligible boost interval to that mode's configured interval. Switching modes does not erase an existing cooldown; once it expires, select the fastest currently funded input. Before buying Jar power, try eligible adjacent input handlers so a pullable faster input is not overlooked.

On initial activation, the first eligible payment can apply its first boost immediately. Under uninterrupted operation the intervals in the table apply. For Jar power, the purchase tick counts as the first of five possible boosts in a 100-tick window, at offsets 0, 20, 40, 60, and 80 if growth remains eligible. A renewal at tick 80 resets the window without creating a second boost on that tick.

While liquid or gas funds the selected mode, do not renew Jar power or apply Jar boosts. Previously paid Jar time still counts down. When faster inputs become unavailable, remaining paid Jar power can resume; otherwise attempt a Jar payment when its payment cooldown permits.

Keep payment/boost cooldowns as per-pot counters, not global world-time phases. Jar payment cooldown is at least 80 ticks between successful payments. When a payment fails, retry at the configured payment interval rather than scanning the full neighborhood every tick.

Paid Jar duration elapses during loaded ticks even when growth is invalid, capped, or using another mode. No refunds. Keep at most one mode's boost per tick.

### Idle intake and persistence

Treat accepting or pulling Source into a pot as consumption from the supplier: disable external intake and pulling when there is no valid crop/soil or when a boost cannot add growth. Existing buffered contents remain. Revalidate when input changes; do not drain an external handler before validation.

Store both buffers, Jar time, and cooldowns with the block entity. Unloaded pots do not tick, spend Source, grow, or accrue catch-up operations. Save/reload cannot grant a free boost or renew a window. Replacing the crop must not grant free power; existing paid duration continues counting down under the normal rules.

Expose buffers as input-only to automation. Source used by this upgrade is not a new fluid/chemical routing device. Breaking the pot follows upstream inventory drops; buffered resources and paid time are discarded, preventing break/place resource duplication.

### Fertilizer recipe behavior

Use the active bonemeal recipe's growth amount for the current crop/soil so pack data changes are respected, rather than permanently hardcoding 400–600. Retain the upstream 20-tick finishing cap. If no applicable bonemeal recipe exists, disable Source boosts without spending resources.

Do not call `BasicFertilizer.apply()` with a null or fake player: the pinned implementation dereferences the player for item consumption. Reuse the recipe's growth calculation in a server-side adapter without modifying the player fertilizer cooldown or consuming items. The initial implementation supports the pinned basic bonemeal recipe; an unsupported custom fertilizer implementation disables boosting with a bounded diagnostic rather than guessed behavior.

### Variants and crafting

Register a corresponding Ars variant for each Hopper Botany Pot supplied by the pinned Botany Pots release. Generate mappings, recipes, assets, loot, and language entries from the same variant list. Pots from other addons are outside the initial scope unless explicitly mapped later.

Use one shaped recipe per original variant so output appearance is deterministic:

```text
A A
BCB
BBB
```

- A: `minecraft:gold_nugget`.
- B: Archwood Planks using `c:planks/archwood`, verified in the pinned Ars Nouveau generated resources.
- C: the matching original Hopper Botany Pot.
- Result: one matching Ars Botany Pot.

This consumes two gold nuggets, five planks, and one pot. Upgrade crafting preserves color/material identity; it does not promise to copy arbitrary item data or stored crop/inventory state. Do not silently accept an unmapped pot and replace it with a different appearance.

Keep the original pot silhouette and material textures. Add a gold rim through derived models/textures visible in block and item rendering. Preserve crop and soil rendering; the user declined mockups and the browser companion.

## Implementation structure

- `ArsBotanyPotBlock`: extends `BotanyPotBlock` with `PotType.HOPPER`, constructs the custom block entity, and supplies the custom ticker.
- `ArsBotanyPotBlockEntity`: extends `BotanyPotBlockEntity`, runs the upstream tick once, owns Source state, and persists it alongside inherited inventory/growth data.
- A small Source scheduler separates mode selection and cooldown decisions from Minecraft capability calls, allowing deterministic tests.
- Resource adapters handle standard Ars provider access and sided fluid/chemical handlers. Simulate extraction before execution; account only for amounts actually transferred and never overfill.
- A growth adapter computes a bonemeal-equivalent increment without faking a player interaction.
- Client registration reuses upstream pot crop/soil rendering where compatible and registers all Ars block entity variants. Verify renderer and menu type assumptions during implementation.
- Registration and asset generation use a shared variant mapping. Published jars remain Gradle dependencies; submodules stay reference-only and unchanged.

The exact class/file split is left to implementation. Keep abstractions limited to state-machine testing and the three actual resource APIs.

## Configuration

Use server-controlled NeoForge configuration with descriptions and positive-value validation. Proposed keys/defaults:

| Key | Default |
| --- | --- |
| `jar.sourceCost` | 2000 |
| `jar.paymentIntervalTicks` | 80 |
| `jar.poweredDurationTicks` | 100 |
| `jar.boostIntervalTicks` | 20 |
| `jar.searchRange` | 3 |
| `liquid.costMb` | 250 |
| `liquid.boostIntervalTicks` | 10 |
| `gas.costUnits` | 125 |
| `gas.boostIntervalTicks` | 5 |

The percentage describes the default cost relative to a standard jar, not a percentage of its current contents or of whichever provider is found. Buffer capacities derive from the configured costs. Initially require a world/server restart for configuration changes, avoiding live buffer truncation. On loading stored amounts above a reduced capacity, clamp to capacity and issue a bounded diagnostic.

## Verification and acceptance

Automated scheduler/growth tests must cover:

- Every mode's payment and boost cadence, first operation, and one boost per tick.
- Jar renewal resets to 100 rather than adding time; failed payment does not erase paid time.
- Faster-mode selection, switching with a shared cooldown, and fallback to existing Jar power.
- No resources spent for invalid crop/soil, growth capped within 20 ticks of maturity, or crops whose required growth is at most 20 ticks.
- Independent ordinary fertilizer use and Source scheduling; valid crop-dependent bonemeal growth and recipe reload handling.
- Separate one-operation buffers, partial fills, wrong resource rejection, full-buffer backpressure, and exact payment amounts.
- Configured costs/intervals, persistence, unload/reload, and no break/place duplication.

Development integration tests must cover all six input faces, diagonal rejection for liquid/gas, three-block diagonal Jar access, out-of-range Jars, multiple Jar providers, creative providers, real pipe push/pull behavior, and extraction-denied sides. Test race/partial-transfer behavior without promising arbitrary external handlers can be rolled back.

Check every variant's crafting identity, models, item appearance, loot, inherited menu/inventory access, crop/soil display, harvest tool use, waterlogging, comparator behavior, and item export. Confirm ordinary pots remain unchanged.

Run a Java 21 build and bounded development-server smoke test. Launch `gradlew.bat runClient` on Windows with NeoForge, this mod, and all configured dependencies; interactively verify crafting, rendering, hopper behavior, all three Source modes, priority, consumption, and save/reopen. This client run is a required verification step, not an optional substitute for testing. Then test in a backed-up ATM10Aero 0.8.0 client and dedicated server. Compilation and development runs alone do not establish full-pack compatibility. Document unperformed checks as limits.

## Approval and next step

The user approved the spec, including its proposed defaults, and requested the implementation plan. Write that plan before product code. Do not create commits without explicit consent for that action.
