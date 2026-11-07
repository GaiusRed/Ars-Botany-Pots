# Per-side Source and Multiple Harvests Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Apply clean-code-principles to every implementation and review task.

**Goal:** Replace shared Source storage with four horizontal inputs and nine configurable tiers, supporting bounded repeated harvests while retaining crop-specific pot rules.

**Architecture:** Keep existing storage adapters, pure scheduler, growth adapter, and block entity. Replace their shared-buffer contracts directly. Implement the small harvest adapter as focused private methods on the existing block entity, which already has protected access to inventory and cooldown state; no new production file is planned.

**Tech Stack:** Java 21, Minecraft 1.21.1, NeoForge 21.1.250, existing pinned Botany Pots/Ars dependencies, JUnit 5, NeoForge GameTests.

**Spec:** `docs/superpowers/specs/2026-11-07-per-side-source-design.md` (approved 2026-11-07).

## Global Constraints

- Use the existing personal GitHub checkout on `feature/per-side-source-buffers`; do not create worktrees, commits, or change upstream submodules. User authorization permits subagents for execution, not commits.
- Only north/east/south/west accept Source. Top/bottom/unsided Source capability queries return no handler. Keep bottom item automation.
- Fixed priority gas > liquid > Jar, based on payable stored quantities, never configured speed or connected pipes. Suppressed buffers may retain/refill quantities but cannot pay or boost.
- One liquid and one gas buffer per horizontal face. Per-mode capacity is independent of per-face cost and must cover one payment. Spend all funded faces of the selected mode together, once per boost; never multiply aggregate tier growth again.
- One shared cooldown and at most one boost purchase per real tick. Jar settings remain cost 2500, payment interval 80, duration 100, boost interval 10, range 3, growth 600–800. Jar timers expire while suppressed; do not renew then.
- Liquid defaults: per-face cost/capacity 500 mB, interval 5 for all four tiers, aggregate growth ranges 500–700, 1000–1400, 1500–2100, 2000–2800.
- Gas defaults: per-face cost/capacity 250 native chemical units, interval 1 for all four tiers, aggregate growth ranges 400–600, 800–1200, 1200–1800, 1600–2400.
- Each liquid/gas tier has independent interval and growth bounds. `growth.maxHarvestsPerTick` defaults to 16, range 1–256; count native and additional harvests together. Configuration changes require restart.
- Run the native ticker once per real tick; extra cycles spend Source credit and reproduce per-harvest behavior without repeating natural growth or per-tick work.
- Use active pot recipe growth requirements, soils, modifiers, callbacks, yield and tool rules. Retain the supported positive pot-bonemeal eligibility check; do not add world-only Mystical Agriculture restrictions.
- Preserve exact unused growth, native partial progress, cached capability identity, and dormant optional-resource quantities. No legacy Source migration, replacement resources, benchmark harness, dependency upgrades, or pack-script changes.
- The user supplies ATM10 Aero measurements and the instance location later. Do not claim runtime pack verification from source research or synthetic tests.

## Review Focus

- A neighbor caches a sided capability before a live NBT load: it must still write to the same face afterward (Task 1).
- An extraction handler returns less on execution than simulation: only the real amount can become payable (Task 1).
- A deliberately slow gas configuration coexists with full liquid buffers: gas still wins, with no lower-mode payment during cooldown (Task 1).
- A harvest callback changes soil/crop, breaks a tool, removes the pot, or fills output: the next cycle must revalidate and stop when required (Task 2).
- Native progress resets to -1 and credits can be fractional: additional cycles must not gain free growth, duplicate a native harvest, or round into maturity (Task 2).

## Execution and review protocol

Three dependent tasks, executed sequentially. Task 1 is a complete vertical change because settings, storage, scheduling, and their callers share one contract; splitting them would introduce temporary APIs or leave the project uncompilable. Task 2 adds repeated harvesting to that working foundation. Task 3 verifies the complete feature and updates user-facing documentation.

For each task, dispatch one fresh implementer with its task brief, the approved spec, Global Constraints, ownership list, and report path. Require test-driven-development and clean-code-principles. The implementer must not delegate or commit. After its report, dispatch a separate task reviewer for both spec compliance and code quality; fixes go back to that implementer. Finish with a fresh whole-change reviewer. Select supported models at execution time following the subagent skill, with explicit model and reasoning settings; use a capable model for harvest correctness and final review.

Use one implementer at a time in the shared checkout. Keep a plan-specific progress ledger and reports under `.superpowers/sdd/2026-11-07-per-side-source/`. Reviewers are read-only. Record commands, exit codes, test counts, changed paths, unresolved checks, and any necessary rulings. Do not use commit-range-only review helpers: HEAD will remain unchanged. Save before/after file snapshots per task, including new files, so each reviewer receives the actual task diff; final review uses the complete working-tree diff and untracked deliverables. Never stage files merely to generate a review package.

Clean-code review must verify SRP, KISS, DRY, and YAGNI in concrete terms: settings own defaults/validation; storage owns resource accounting; scheduler owns priority/timers; growth owns eligibility/credit math; block entity coordinates and hosts the small harvest operation. Remove replaced contracts and tests, rather than retaining compatibility shims. Keep helper methods focused; minimal changes do not justify a large inseparable method.

## Production change map

All paths below are relative to `src/main/java/io/github/gaiusred/sourcepots/`.

| Existing file | Change |
| --- | --- |
| `source/SourceSettings.java` | Replace single liquid/gas growth settings with four immutable tiers; separate capacity/cost; add harvest bound. |
| `config/SourcePotsConfig.java` | Register the approved keys/defaults and build a validated snapshot. |
| `source/SourceAccess.java` | Replace shared-mode funding with exact face-mask funding/payment contracts. |
| `source/SourceScheduler.java` | Fixed priority, funded-side tier selection, one aggregate boost result. |
| `source/PotSourceAccess.java` | Directional liquid buffers and gas delegation, transfer, batch payment, buffer persistence. |
| `source/GasStorage.java` | Directional numeric storage/payment API without optional chemical types. |
| `source/UnavailableGasStorage.java` | Retain/clamp dormant quantities independently by direction. |
| `compat/mekanism/MekanismSourceStorage.java` | Four native gas tanks with stable sided input views and horizontal pulling. |
| `compat/mekanism/MekanismCompatibility.java` | Pass the queried horizontal face into its handler. |
| `registry/ModContent.java` | Pass the queried horizontal face into liquid input; preserve item registration. |
| `block/ArsBotanyPotBlockEntity.java` | New storage/purchase integration, save schema, bounded credit loop and private harvest adapter. |
| `growth/BonemealGrowth.java` | Change only if needed to expose existing eligibility/credit behavior to the loop; preserve recipe semantics. |

Keep `SourceState`, `SourceMode`, `JarSourceAccess`, pot variants/assets/recipes, client registration, build configuration, and CI unchanged unless a specific failing requirement proves an edit necessary. Do not introduce managers, generic resource frameworks, per-tier subclasses, new dependencies, or speculative support for additional resources.

### Task 1: Replace shared inputs with directional storage and nine-tier purchases

**Owner:** First implementation subagent; scope is the production change map except the repeated-harvest work reserved for Task 2.

**Files:** Modify the mapped settings/config/storage/scheduler/capability files and the block entity's Source setup, purchase sampling, and persistence calls. Modify existing tests and fixtures affected by their contracts:

- `src/test/java/io/github/gaiusred/sourcepots/source/{SourceSettingsTest,SourceSchedulerTest,UnavailableGasStorageTest,OptionalApiLoadingTest}.java`.
- `src/gametest/java/io/github/gaiusred/sourcepots/gametest/{SourceAccessGameTests,SourceCadenceGameTests,ArsPotGameTests,GrowthConfigFixture,MekanismTestAccess}.java`.
- `src/smoke/java/io/github/gaiusred/sourcepots/smoke/OptionalRuntimeSmokeTests.java`.
- Update other existing test callers only where compilation or obsolete six-face assertions require it. Keep unrelated coverage.

**Interfaces (final replacements, not temporary bridges):**

- `SourceSettings.TierSettings(int intervalTicks, GrowthSettings growth)`; immutable `ModeSettings(long cost, long capacity, List<TierSettings> tiers)` with exactly four entries and `TierSettings tier(int fundedSides)` for counts 1–4.
- Retain existing Jar settings accessors and `jarGrowth()`. Replace standalone liquid/gas growth fields with the tier list. Add `int maxHarvestsPerTick()` and `TierSettings tier(SourceMode mode, int fundedSides)`; Jar uses count 0.
- `SourceAccess`: `int fundedSideMask(SourceMode mode)`, `boolean pay(SourceMode mode, int sideMask)`, `boolean canPayJar()`, `boolean payJar()`. Masks apply only to liquid/gas; bit `1 << direction.get2DDataValue()` identifies a horizontal face. Reject zero/invalid masks on batch payment and prevalidate all requested faces before draining any.
- `SourceScheduler.tick(SourceState state, SourceSettings settings, boolean eligible, SourceAccess access)` returns nullable nested `Boost(SourceMode mode, int sideMask, SourceSettings.TierSettings tier)`. A Jar boost has mask 0. Scheduler pays before returning a result and never samples growth.
- `PotSourceAccess`: `IFluidHandler fluidHandler(Direction side)`, `long storedLiquid(Direction side)`, `long storedGas(Direction side)`, `void saveBuffers(CompoundTag power)`, `boolean restoreBuffers(CompoundTag power)`. Keep availability/access-to-gas methods only where still needed.
- `GasStorage`: replace unsided quantities with `long stored(Direction side)`, `void restore(Direction side, long amount)`, `boolean canPay(Direction side)`, `boolean pay(Direction side)`; keep `available()`, `capacity()`, and `pullAdjacent()`. `MekanismSourceStorage.handler(Direction side)` returns its stable `IChemicalHandler` view. Replace its factory with `static GasStorage create(ServerLevel level, BlockPos pos, long cost, long capacity, BooleanSupplier eligible, Runnable changed)` so payment uses cost, not capacity. Unavailable storage can keep its capacity-only constructor because it never pays.
- `PotSourceAccess.restoreBuffers` returns whether anything exceeded capacity. Save under `buffers.<direction>.liquid` and `buffers.<direction>.gas`, using stable direction names. Restore into existing tanks/views. Use block-entity root `sourcepots_v2` to discard the obsolete Source format without a migration path; inherited pot NBT is untouched.

- [ ] **Step 1: Add failing settings/scheduler/storage regressions.** Assert all eight configured tiers and Jar defaults, independent capacity/cost, tier count/range validation, intervals/bounds >0, capacity >=cost, harvest bounds 1 and 256 accepted and 0/257 rejected, and no overflow for large gas quantities. Pin these representative assertions:

```java
assertEquals(2000, settings.tier(SourceMode.LIQUID, 4).growth().minTicks());
assertEquals(2400, settings.tier(SourceMode.GAS, 4).growth().maxTicks());
assertEquals(16, settings.maxHarvestsPerTick());
```

Use fake SourceAccess masks in scheduler tests: gas mask 1 plus liquid mask 15 must pay only gas mask 1; gas mask 7 selects tier 3; an unpaid partial face is excluded. With gas interval 9 and liquid interval 1, gas still wins. Assert cooldown advancement once per tick, suppressed Jar expiry without renewal, fallback respecting remaining cooldown, and no payment when ineligible or a batch fails prevalidation.
- [ ] **Step 2: Run the focused unit tests and record the red result.** Run `./gradlew.bat test --tests '*SourceSettingsTest' --tests '*SourceSchedulerTest' --tests '*UnavailableGasStorageTest' --console=plain`. Expected: failures from missing/replaced behavior, not unrelated environment errors.
- [ ] **Step 3: Replace settings/config and scheduler contracts.** Register `liquid.costMbPerSide`, `liquid.bufferCapacityMbPerSide`, corresponding `gas.costUnitsPerSide`/`gas.bufferCapacityUnitsPerSide`, and `{liquid,gas}.{oneSide,twoSides,threeSides,fourSides}.{boostIntervalTicks,growthMinTicks,growthMaxTicks}`. Use SourceSettings defaults once; validate the complete snapshot. Keep Jar behavior but replace interval comparison with fixed priority. Remove superseded mode-growth accessors/config keys.
- [ ] **Step 4: Implement sided storage and wire callers.** Use `Direction.Plane.HORIZONTAL` and stable per-face native tanks/views. Keep liquid types and chemical types in their existing adapters. Pull only into the matching face; simulate then account for actual extraction. Consume exactly cost per funded face. Update capability registration and block-entity purchase sampling to use the returned tier. Replace buffer NBT and dormant restore paths. Keep the existing single-harvest integration until Task 2 replaces it; do not add another parallel execution path.
- [ ] **Step 5: Add and run real-capability regressions.** Extend `SourceAccessGameTests` and smoke fixtures: all horizontal faces independent; top/down/null absent; push/pull parity; wrong resources; partial execute of 40 units; cost 500/capacity 1500 allowing three payments; every n=1..4 tier with exact n*cost consumption; no drain on failed batch; no chunk loading; cached handler identity survives live NBT restore. Save different amounts in every face and reload with optional resources unavailable, then available. Preserve bottom item automation. Rewrite old six-face assertions instead of leaving contradictory tests.
- [ ] **Step 6: Verify the complete vertical change.** Run `./gradlew.bat test compileGametestJava compileSmokeJava runCoreGameTestServer runFluidGameTestServer runMekanismGameTestServer runGameTestServer --console=plain`. Expected: no compilation errors, all unit tests pass, and each native profile reports zero required test failures. Fix only regressions attributable to this task; report environmental blockers accurately.
- [ ] **Step 7: Self-review and hand off to the task reviewer.** Report replaced APIs removed, exact tests/results, remaining limits, and changed files. Reviewer checks both spec compliance and clean-code quality from the before/after diff. No commit.

### Task 2: Spend paid growth across bounded, recipe-preserving harvests

**Owner:** Fresh implementation subagent, after Task 1 review passes.

**Files:**

- Modify `src/main/java/io/github/gaiusred/sourcepots/block/ArsBotanyPotBlockEntity.java`.
- Modify `src/main/java/io/github/gaiusred/sourcepots/growth/BonemealGrowth.java` only if existing methods cannot serve the loop without duplication.
- Create `src/gametest/java/io/github/gaiusred/sourcepots/gametest/MultipleHarvestGameTests.java` and register with the existing test holder setup if required.
- Extend existing `GrowthTestEnvironment`, `GrowthTestSupport`, `GrowthReloadFixture`, `GrowthConfigFixture`, `BonemealGrowthGameTests`, `SourceCadenceGameTests`, and `MatureWheatGameTests` test files where they own recipes, controls, or superseded one-harvest expectations.

**Interfaces:** Consume Task 1's Boost/tier/settings/buffers. Preserve public `BonemealGrowth.isEligible(BotanyPotBlockEntity, ServerLevel)` and `applyEligibleCredit(BotanyPotBlockEntity, double)` if possible. Add focused private block-entity methods `int spendGrowthCredit(ServerLevel server, int remainingHarvests)` and `boolean harvestSourceCycle(ServerLevel server)`. The first returns actual additional harvests and updates existing credit/progress; the second performs exactly one eligible mature harvest. Use a focused private storage-room check if required. No new public harvest API or new production file is needed.

- [ ] **Step 1: Add failing multi-harvest GameTests with controlled recipes.** A crop needing 100 ticks and credit 1000 must harvest more than once when limit=16, never exceed the limit, and conserve credit plus applied progress against each cycle's actual reset state. Limit=1 counts the native harvest and prevents another; limit=2 with a native harvest permits at most one extra. A huge boost at limit=2 preserves the remainder through save/reload and does not cause a second purchase that tick.

Add a pair of otherwise identical recipes requiring 100 and 400 growth ticks: equal paid growth must retain their different cycle costs. With no modifiers, compare effective requirements directly; validate that the active soil modifier changes required growth via the native helper. Wrong soil consumes neither resources nor credit. Fractional credit and native -1 reset progress must not grant a free cycle.
- [ ] **Step 2: Run `./gradlew.bat runGameTestServer --console=plain` and record red evidence.** Existing single-harvest implementation must fail the new repeated-harvest assertions. Ensure recipe fixtures load correctly before treating a failed assertion as the intended red result.
- [ ] **Step 3: Add the bounded credit loop.** Count native harvests using the pinned ticker's actual reset behavior, verified by tests rather than inferred solely from resource availability. Run the native ticker once, then spend retained credit before the scheduler's one purchase, then spend newly purchased credit with the remaining allowance. Apply eligible partial growth even if it cannot mature; return to the scheduler only after retained credit is exhausted and allowance remains. No purchases or refills after the allowance is spent. Advance SourceState timers exactly once even for inactive/mature/blocked pots; use the existing scheduler's eligible flag rather than ticking it twice.
- [ ] **Step 4: Add the private single-harvest adapter.** Mirror only the pinned native harvest branch: `Helpers.getLootRolls`, `crop.onHarvest`, insertion through `Services.GAMEPLAY.addItem` into inherited `getItems()`/`STORAGE_SLOTS`, native tool damage rules, progress reset, comparator and game-event updates. Keep normal native-tick cooldown behavior and remove the pause only for Source-assisted cycles. Revalidate current crop/soil/required ticks, supported pot fertilizer, removal, harvest conditions and tool-dependent eligibility between cycles. Check storage before extra harvests and stop when full; preserve the native insertion semantics without inventing rollback of arbitrary crop functions. Do not multiply drops or call the full ticker inside the loop.
- [ ] **Step 5: Add adverse-callback and per-tick accounting tests.** Count soil/crop onTick and onGrowthTick, Source timers, export work and native fertilizer cooldown advancement: no additional advancement beyond the single native tick. Count loot rolls, onHarvest functions, tool damage, resets and notifications per actual harvest. Test a final-durability tool, callbacks that change crop/soil or remove the pot, failed harvest conditions, full output, a partial output fill that becomes full during the loop, crop reset clearing credit, and repeated live NBT restore without duplication. Preserve ordinary unpowered ticking and player fertilizer behavior.
- [ ] **Step 6: Run `./gradlew.bat test runGameTestServer --console=plain`.** Expected: all unit and full GameTests pass, including exact limit/credit assertions. Inspect existing default-timing tests and update only expectations intentionally changed by additional harvests. Real Allthemodium validation remains deferred to the user's instance; synthetic long-growth/soil fixtures prove mechanism, not pack compatibility.
- [ ] **Step 7: Self-review and obtain task review.** Review the small harvest branch side-by-side with the pinned upstream ticker and report every intentional difference. Verify protected state access stays within the existing subclass and no broad copy of upstream logic was introduced. Reviewer must approve both behavior and code quality. No commit.

### Task 3: Verify optional runtimes, streamline the final diff, and document operation

**Owner:** Fresh integration subagent, after Task 2 review passes; production changes limited to demonstrated defects in Tasks 1–2.

**Files:** Existing affected unit/GameTest/smoke fixtures; `README.md`; `docs/verification/ars-botany-pots.md`; `docs/verification/optional-source-integrations.md`. Correct outdated six-face/shared-buffer descriptions and document the new config/save behavior without rewriting historical evidence as a new result. No planned build/CI/assets changes.

**Interfaces:** Verify the completed APIs from Tasks 1–2; introduce none. Keep user prose technical and neutral; apply the repository's writing rules without inventing a personal voice profile.

- [ ] **Step 1: Run focused integration cases.** Exercise full gas+liquid priority, partial higher-priority buffers, Jar expiry/fallback, all face counts, capacity reductions, directional dormant state, and cached views after reload. Confirm required-only loading still rejects accidental optional API linkage using `OptionalApiLoadingTest`. Update existing smoke calls to real face handlers without imports of Mekanism types in the core smoke source set.
- [ ] **Step 2: Audit the final production diff using clean-code-principles.** Search all main/test callers for obsolete shared-buffer methods, old growth config keys, interval-based priority, and six-face assumptions. Remove replaced methods/branches and temporary helpers; do not preserve dead test support. List any changed production path outside the map with its concrete failing requirement. Inspect the distributed runtime for optional API isolation. Do not refactor unrelated code to improve a diff statistic.
- [ ] **Step 3: Run final automated verification on the settled tree.** Run `./gradlew.bat test runCoreGameTestServer runFluidGameTestServer runMekanismGameTestServer runGameTestServer build --console=plain`. Expected: all unit tests pass, each native profile has zero required failures, and one distributable jar builds. Inspect jar entries to confirm GameTest/smoke classes and optional mods were not bundled. This reuses CI's existing tasks; datagen is unchanged and needs rerunning only if generated assets or their inputs actually changed.
- [ ] **Step 4: Perform development-client checks.** Run `./gradlew.bat runFullClient --console=plain`; verify actual horizontal fluid/chemical pipes, rejected top/bottom inputs, two/four-face growth, mode suppression/fallback, rendering, harvest tools and bottom item export, and save/reopen. Use an isolated development world. If interactive tooling is unavailable, record these checks as unperformed rather than treating server tests as proof. No CurseForge-instance edits or automatic pack benchmarks.
- [ ] **Step 5: Update user documentation and verification evidence.** Describe the nine tier settings, per-face payment/capacity, funded versus connected sides, fixed priority, `maxHarvestsPerTick`, retained credit, old Source-state reset, and active pot recipe requirements. Record actual commands/test counts/manual checks plus limits; retain source-versus-installed-pack distinction for Allthemodium. Do not claim competitive balance before user measurements.
- [ ] **Step 6: Review the integration task, then obtain a fresh whole-change review.** Provide the approved spec, plan, full diff (including untracked files), test reports, and unresolved manual checks. Ask for spec compliance, payment/credit conservation, harvest semantics, optional loading, and unnecessary production changes. Resolve findings; rerun only affected checks after fixes. Run whitespace/status checks and present the final uncommitted result. No commit, push, PR, or merge is part of this plan.

## Plan self-review

- Storage/transfer/config/priority/persistence requirements map to Task 1; repeated harvests and crop/soil preservation map to Task 2; profile/client/documentation acceptance maps to Task 3.
- Cross-task contracts use the same tier, mask, Boost and storage methods. Scheduler tests stay world-free; native behavior is verified by real pot GameTests.
- Every Review Focus condition has a named assertion or scenario in its owning task.
- Twelve existing production paths are the expected upper scope, with `BonemealGrowth` conditional and no new production classes. Tests may grow substantially because the new payment and harvest behaviors need independent verification.
- No task includes an automatic commit or worktree. Subagents execute sequentially with independent reviews; this document is the requested plan, not authorization to skip its review and begin implementation.
