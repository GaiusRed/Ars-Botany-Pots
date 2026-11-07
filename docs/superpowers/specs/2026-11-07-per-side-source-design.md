# Per-side Source buffers and configurable growth tiers

Status: Approved by the user on 2026-11-07, including bounded multiple harvests and preservation of active pot recipe requirements. Implementation planning requested; no implementation or balance measurements yet.

## Intent and scope

Give each horizontal face of an Ars Botany Pot its own Source storage. Feeding more faces with the active resource buys larger growth boosts. Keep one active resource mode, with priority gas > liquid > Jar. Expose the nine reference tiers in server configuration so their balance can be adjusted from the user's ATM10 Aero measurements.

This is a revision of the unpublished mod. It supersedes the previous design's six-face inputs, shared liquid/gas buffers, interval-based mode selection, three growth tiers, and one-harvest-per-tick limit. Other pot behavior stays as described in the existing design unless changed below. Configuration and saved Source state do not need migration.

The user will perform in-game comparisons and report metrics. Automated benchmark tooling, collecting competitor measurements, and installing the mod into a CurseForge instance are outside this design task. Pack installation can follow when the user supplies the instance location. No claim of competitive performance or pack compatibility follows from this spec.

## Confirmed decisions

- Only north, east, south, and west accept liquid/gas and contribute to acceleration.
- Each horizontal face has independent buffers.
- Higher-priority available Source suppresses all lower-priority modes. Resources do not combine into mixed growth tiers.
- Buffer capacity and growth behavior must be configurable.
- The reference tiers are Jar, liquid with one through four funded sides, and gas with one through four funded sides.
- Balance will primarily change configuration defaults after the user supplies measurements.
- Replacement and simplification are allowed; backwards compatibility is unnecessary.
- Support multiple harvests per tick so later balance changes can exceed the old throughput ceiling.
- Preserve active Botany Pots recipes' crop-specific growth requirements and valid soils while accelerating them.

## Approaches considered

1. **Independent buffers, one selected mode, one aggregate boost. Selected direction.** Count funded sides of the highest-priority mode, select that count's configured tier, and buy one boost from those buffers. This supports nine explicit tiers without separate side timers or mixed resource arithmetic.
2. **Independent contributions from liquid and gas together. Rejected by the user.** Each face could add its own growth, but mixed combinations complicate balancing and conflict with the established priority rule.
3. **A shared buffer with a connected-face counter. Rejected in this proposal.** It would require rules for detecting active feeds and would not give each side independent storage. Empty pipe connections could accidentally increase strength.

For multiple harvests, use a bounded Source-credit loop around a small harvest adapter. Repeated full native ticks would repeat natural growth, crop/soil tick callbacks, and export timers. Multiplying drops would skip per-harvest recipe behavior and tool damage. Neither approach preserves the required behavior.

## Proposed operating rules

The details in this section are design calls for approval, rather than additional user-confirmed requirements.

### Storage and transfer

Maintain one liquid buffer and one gas buffer for each horizontal direction: four of each, with no transfer between faces. Keeping the media separate avoids adding a per-face mode setting or resource-locking interface. They can both be stored, but only one resource mode can be spent per boost.

A sided capability exposes only that face's buffer for its resource. Top, bottom, and unsided queries expose no Source input. Bottom item extraction retains the inherited hopper behavior. Source inputs remain insertion-only.

Pull only from the adjacent block on the matching horizontal face, querying its opposite face. Pushed and pulled resource enters the same buffer. Do not scan diagonals or load neighboring chunks. Preserve resource identity validation, simulated transfers, actual-amount accounting, partial fills, and full-buffer backpressure. Do not add cross-face redistribution.

Use one configured capacity per resource, applied separately to each face. Capacity is independent of payment cost and must be at least that cost. The initial capacity equals one payment. A larger configured capacity permits several payments to be stored on each face.

Input remains conditional on the pot being eligible to buy growth. Lower-priority buffers may fill during eligible operation even while their mode is suppressed. Consume retained credit first, including across additional harvests. When the harvest limit has been reached or further growth is ineligible, do not pull or accept more Source that tick. Capability queries never trigger harvests or buy growth themselves.

### What available means

A face is funded when its usable buffer contains at least one configured payment. A connected pipe, a partial payment, or an installed integration alone does not count.

Choose gas if at least one gas face is funded; otherwise choose liquid if at least one liquid face is funded; otherwise consider Jar power. This priority is fixed, regardless of configured boost intervals or growth amounts. Suppressed buffers retain their contents.

Consequently, three funded gas faces select gas tier 3 even if four liquid faces are funded. One funded gas face selects gas tier 1. No liquid or Jar payment supplements either boost. Buffered Source still counts after its pipe is disconnected, until consumed.

Availability is evaluated before the shared boost cooldown. Do not spend a lower-priority resource while waiting for the selected mode's next boost. If the higher-priority resource runs out, fallback uses the remaining shared cooldown rather than granting an immediate extra boost.

### Boost purchase

Retain one pot-wide boost cooldown. At most one boost purchase occurs per server tick.

For a liquid/gas purchase:

1. Validate crop/soil and growth eligibility using the existing growth adapter.
2. Spend existing retained growth through the bounded harvest loop described below. Recheck eligibility after each harvest. If that loop reaches the tick's harvest limit or cannot advance, stop without refilling or paying.
3. Refill eligible buffers and select the highest-priority funded mode.
4. Count its funded faces, from one to four, and select that tier.
5. When the shared cooldown is zero, consume one payment from every funded face in that mode, sample the tier's configured growth range once, and start that tier's cooldown.
6. Spend the purchased growth through the same harvest loop, using the tick's remaining harvest allowance. Retain exact unused credit for later cycles or ticks.

All payments in the batch use internal storage and succeed together. Plan before mutation; an ineligible or unfunded operation consumes nothing and does not reroll purchased growth. The tier is based on funded faces at purchase time, not pipe count or recent throughput. All funded faces participate, avoiding a direction preference.

Example: three liquid buffers each hold 500 mB and the fourth holds 200 mB. With a 500 mB cost, the pot spends 1,500 mB total and buys tier 3. The fourth buffer keeps its 200 mB. If gas can fund a purchase, no liquid is consumed.

Each tier defines the aggregate growth per purchase. Do not multiply that value by the face count again. Resource cost is per participating face, so a four-face batch costs four payments. Retained credit is already paid and keeps its sampled value when modes or tiers change. Spend retained credit before paying, and buy at most one batch per tick even if the harvest loop could use more growth.

### Jar fallback

Keep the existing Jar cost, search range, payment retry interval, powered duration, and growth rules. Jar power does not scale with faces.

While liquid or gas is available, do not buy or apply Jar boosts and do not renew Jar power. Existing Jar timers continue to expire. On fallback, any remaining paid Jar window can be used; otherwise follow the normal Jar payment rules. Preserve one shared boost cooldown across all mode and tier changes.

### Growth, repeated harvests, and limits

Reuse native Botany Pots crop, soil, loot, tools, harvesting, and item output. Apply the native pot tick once per server tick. Preserve the existing Source-assisted harvest pause bypass, exact retained credit, ordinary unpowered operation, and independent fertilizer behavior. Additional harvests spend Source credit; they grant no additional natural growth or ordinary fertilizer applications.

Add `growth.maxHarvestsPerTick`, proposed default 16, allowed range 1–256. This is a work bound, not free production: growth credit must fund every completed cycle. Count any harvest made by the native tick against the same allowance. Setting 1 restores a one-harvest-per-tick limit. Changing the limit requires restart.

After the native tick, use a bounded loop to apply retained or newly purchased credit, complete a mature harvest, and continue into the next cycle while credit and harvest allowance remain. Each cycle uses the current crop/soil's effective required growth ticks. Deduct only growth actually applied. Respect the native reset progress, including its -1 starting value; do not grant a free growth tick on every extra cycle. A partial cycle remains in normal growth progress and excess credit remains paid credit.

The harvest adapter must reproduce the pinned native harvest operation: recipe eligibility, yield/loot-roll calculation, crop `onHarvest` callbacks and functions, tool damage and breakage rules, drops into normal storage, progress reset, comparator updates, and harvest notification. Revalidate crop, soil, effective growth requirement, and harvest conditions after each harvest, since a callback or tool break can change them. Recheck Source growth eligibility before applying credit to the next cycle. No cached eligibility may authorize several cycles blindly.

Apply eligible partial growth even when available credit cannot complete another cycle, then leave that progress in the pot. Stop the loop when credit is exhausted, the allowance is used, the pot is removed, crop/soil becomes invalid, a harvest condition fails, or storage is full. Preserve unused credit when stopping. If retained credit was exhausted before the allowance was used, the scheduler may authorize the tick's one purchase, after which the loop resumes. A normal native harvest is not charged again as a Source purchase. Soil/crop per-tick hooks, natural growth, ordinary fertilizer cooldowns, resource timers, and item export timers advance only once per real tick. Per-harvest callbacks and damage occur once per actual harvest.

Localize the small amount of pinned upstream harvest behavior in the adapter rather than copying the whole ticker or adding global changes to ordinary pots. Review it against upstream when dependency versions change. Existing native output insertion semantics remain relevant; this does not promise transactional rollback of arbitrary recipe callbacks. Tests must cover output saturation and callback-driven changes.

At 20 server ticks per second, the proposed limit permits up to 320 harvests per second if paid growth and output handling suffice. It does not guarantee that rate. Growth budgets, crop requirements, resource supply, tick rate, and extraction can all impose lower limits. Tier defaults remain the main balance controls; raise the harvest limit only when it becomes the measured bottleneck.

### Crop and soil behavior: findings and preservation

The existing mod calls `pot.getRequiredGrowthTicks()`, which delegates to Botany Pots' `Helpers.getRequiredGrowthTicks`. That uses the active crop recipe's base time divided by the combined global, soil, tool, and pot growth modifiers, rounded down. The growth adapter checks `crop.isGrowthSustained` and `crop.canHarvest` before spending Source. A slower crop therefore still needs more growth credit, and an invalid soil blocks Source growth. Do not replace this with a fixed number of harvests per boost or a percentage of each crop's growth requirement.

Source grows the crop represented by its active Botany Pots recipe. It does not emulate a planted world block's random ticks or enforce world-only rules independently. The existing Source eligibility check also requires a supported positive Botany Pots bonemeal recipe matching the crop/soil; preserve that rule. A Mystical Agriculture tier's world `fertilizable` flag is not independently checked by this mod. Do not claim that every world fertilizer restriction is inherited merely because a seed uses that flag.

Research on 2026-11-07 found:

- The public ATM10 Aero Allthemodium definition uses the custom `mysticalcustomization:magical` tier, whose numeric value is 7, and declares `kubejs:magical_soil` as its crux. The tier names Insanium Farmland and sets `fertilizable` false.
- The Botany Pots Mystical Agriculture addon constructs a pot recipe from the Mystical Agriculture crop. Its default growth time is 1,200 ticks for tier 1 plus 300 ticks for each subsequent tier: tier 7 gives 3,000 ticks before soil/global/tool/pot modifiers. Per-tier config can override this.
- The addon derives valid pot soils from the crop tier and adds the crop's crux as an alternative. On NeoForge it combines these using `CompoundIngredient`. Thus its default pot recipe can accept the tier farmland or crux; world planting rules are not identical to pot rules. Respect the actual loaded pot recipe, including pack overrides, rather than hard-coding Magical Soil as the only valid choice.

These are source findings, not a runtime check of the user's installation. Confirm the installed addon version, generated tier configs, tags, and active recipes when the instance is supplied. Do not copy pack scripts or add replacement Allthemodium recipes to this mod.

Sources inspected:

- Local `upstream/BotanyPots/.../impl/Helpers.java`, `.../impl/data/recipe/crop/BasicCrop.java`, and `src/main/java/io/github/gaiusred/sourcepots/growth/BonemealGrowth.java`.
- [ATM10 Aero Allthemodium crop](https://github.com/AllTheMods/ATM10-Aeronautics/blob/15a32d1fdb9a68bf4060e62136e2c695a6089da6/config/mysticalcustomization/crops/allthemodium.json) and [Magical tier](https://github.com/AllTheMods/ATM10-Aeronautics/blob/15a32d1fdb9a68bf4060e62136e2c695a6089da6/config/mysticalcustomization/tiers/magical.json).
- [Addon crop construction](https://github.com/Darkhax-Minecraft/Botany-Pots-Mystical-Agriculture/blob/e2eac892de7871cd9a32a3e19ece1e4eaecea777/common/src/main/java/net/darkhax/botanypotsmystical/common/impl/data/recipe/crop/MysticalCrop.java), [tier time defaults](https://github.com/Darkhax-Minecraft/Botany-Pots-Mystical-Agriculture/blob/e2eac892de7871cd9a32a3e19ece1e4eaecea777/common/src/main/java/net/darkhax/botanypotsmystical/common/impl/config/TierConfig.java), and [NeoForge ingredient composition](https://github.com/Darkhax-Minecraft/Botany-Pots-Mystical-Agriculture/blob/e2eac892de7871cd9a32a3e19ece1e4eaecea777/neoforge/src/main/java/net/darkhax/botanypotsmystical/impl/NeoForgeMod.java).

## Configuration

Use the existing server-controlled configuration and require a world/server restart. Keep existing Jar settings. Replace the liquid/gas mode settings with:

- `liquid.costMbPerSide` and `liquid.bufferCapacityMbPerSide`.
- `gas.costUnitsPerSide` and `gas.bufferCapacityUnitsPerSide`.
- Four tier sections under each resource, named `oneSide`, `twoSides`, `threeSides`, and `fourSides`.
- Each tier contains `boostIntervalTicks`, `growthMinTicks`, and `growthMaxTicks`. Growth values describe the whole purchase.
- `growth.maxHarvestsPerTick` bounds total harvests per pot per real tick, proposed default 16, range 1–256.

Initial defaults extend the current growth budgets linearly. They are starting values, not measured balance targets:

| Tier | Payment per participating face | Interval | Aggregate growth ticks |
| --- | --- | --- | --- |
| Jar | Existing Jar payment: 2,500 Source | 10 ticks | 600–800 |
| Liquid 1 | 500 mB | 5 ticks | 500–700 |
| Liquid 2 | 500 mB | 5 ticks | 1,000–1,400 |
| Liquid 3 | 500 mB | 5 ticks | 1,500–2,100 |
| Liquid 4 | 500 mB | 5 ticks | 2,000–2,800 |
| Gas 1 | 250 units | 1 tick | 400–600 |
| Gas 2 | 250 units | 1 tick | 800–1,200 |
| Gas 3 | 250 units | 1 tick | 1,200–1,800 |
| Gas 4 | 250 units | 1 tick | 1,600–2,400 |

Initial per-face capacities are 500 mB and 250 gas units. Jar payment cadence and powered duration remain 80 and 100 ticks respectively. Each of the eight liquid/gas tier ranges and intervals can be tuned separately, including nonlinear scaling. Costs and capacities are uniform across faces of the same resource. Direction-specific settings and live configuration reload are outside scope.

Validate positive costs, capacities, intervals, and ordered positive growth ranges. Liquid quantities and growth bounds must fit their underlying APIs; gas quantities use positive longs. Capacity must cover at least one payment. Use overflow-safe validation and arithmetic. Do not require custom configurations to preserve a speed ordering: priority remains fixed even if an administrator makes liquid faster than gas.

## Architecture and persistence

Keep the existing division of responsibilities:

- **Settings:** immutable validated configuration, including four explicit tiers per resource. Keep defaults in one place.
- **Resource access:** owns directional storage, capability views, refill operations, and exact batch payments. Keep Mekanism types inside its optional integration package.
- **Scheduler:** selects a mode and funded-side tier and authorizes one purchase. It operates on testable resource availability/payment abstractions, without world or capability lookup code.
- **Growth adapter:** validates growth eligibility, samples an authorized tier, and applies credit. It does not select resources.
- **Harvest adapter:** performs one recipe-preserving harvest and reports its result. The bounded growth loop uses it without advancing world-tick work. Its native-harvest accounting must be verified against the real ticker.
- **Block entity:** coordinates native ticking, Source work, dirty state, and persistence.

Replace shared-buffer methods and scheduler interfaces where needed. Use small value types for a selected tier/purchase rather than passing loosely related mode/count/range values. Do not introduce a generic resource framework, duplicate the upstream pot implementation, or add a hierarchy per face or tier.

Persist each face's liquid/gas quantities, shared cooldown, Jar timers, retained credit, and Source-assisted harvest state. Use stable direction names. Restore into existing storage objects so cached capability views remain attached. Clamp invalid/excess quantities to configured limits and report capacity truncation once per restoration. Preserve dormant quantities for unavailable optional integrations without allowing them to fund boosts.

Ignore the old Source-specific save format; inherited inventory and crop data remain native. Crop/soil resets and breaking still discard paid credit as before. Do not drop or duplicate buffered Source on breaking. Update tests, names, and documentation to describe the replacement model instead of retaining obsolete paths.

## Verification and acceptance

Use focused unit tests for configuration and scheduling, and GameTests for capabilities, storage, persistence, and real pot growth. Required cases:

- All four horizontal faces accept the correct resources into separate storage; top, bottom, and unsided access cannot insert Source. Bottom item export still works.
- Push and pull reach only their corresponding buffer. Partial fills, excess input, wrong resources, unavailable integrations, unloaded neighbors, and simulation are handled correctly.
- One through four funded faces select the correct configured tier and consume exactly one cost per participating face. Partial faces do not contribute or lose their contents. Capacity greater than cost supports repeated purchases.
- Gas suppresses liquid and Jar even when its configured interval is slower. Liquid suppresses Jar. Empty or partial higher-priority buffers allow fallback. Mixed stored resources never produce mixed growth or multiple purchases.
- Shared cooldown survives mode/tier changes and reloads. Jar renewals cease while suppressed, with paid timers expiring normally.
- Retained growth survives harvests and saves without rerolls, duplicate payments, or truncation. Invalid crop/soil and sufficient credit prevent new spending.
- A large paid boost can complete several cycles in one tick without exceeding the configured total harvest limit. Count native and Source harvests together; limits 1 and greater than 1 both work. No repeated payment or discarded credit occurs at the limit.
- Native per-tick hooks/timers/export run once, while yield rolls, harvest functions, notifications, and tool damage run once per actual harvest. Stop correctly on broken tools, callback changes/removal, failed eligibility, and full output storage.
- Two otherwise identical test recipes with different growth requirements retain their relative credit costs across multiple harvests. Wrong soil prevents growth and spending; valid soil modifiers change effective growth cost. Native reset progress and fractional growth do not create free cycles.
- When the pack is supplied, compare the loaded Allthemodium recipe and allowed soils in ordinary and Ars pots. Preserve its effective growth requirement, harvest/drop rules, and fertilizer eligibility. Record the distinction between world planting rules and pot recipes rather than assuming they match.
- Required-only and full-integration test setups still load. Optional API isolation and existing smoke coverage remain valid.

Run the relevant unit tests, GameTests, and distributable build after implementation. Verify real horizontal pipe input and the rejected vertical connections in a development client. Record unperformed manual checks explicitly. This design-only change requires a document review and diff check, not a game build.

## User measurements and balance follow-up

When reporting results, useful context is the pack version, crop/soil, pot count, resource supply, elapsed time, output count, and comparator setup. Record tick rate or lag where known, and whether supply or output extraction limited production. These are measurement notes for the user, not a benchmark system to implement.

Use reported results to revise the tier defaults. Final acceptance of competitive performance belongs to that follow-up, with comparison targets grounded in those measurements. If growth settings reach the harvest or transport ceiling before the desired performance, identify that limit and revisit the relevant design rather than claiming a larger growth value fixes it.

## Approval and next step

The user approved the design and requested an implementation plan for subagent execution, applying clean-code-principles and minimizing production changes through replacement and simplification. Subagents are authorized for execution. Write and present the plan before modifying product code. Commits and worktrees remain unauthorized.
