# Ars Botany Pots Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. The user requested subagents; do not create worktrees. Do not silently fall back to inline execution.

## Live progress

- Design and implementation plan: approved.
- Documentation: approved; this file and the spec form the initial docs-only checkpoint.
- Tasks 1–8: not started. Update their checkboxes as verified steps finish.
- Execution blocker: the current session exposes no subagent tool. Subagent execution requires a session with that tool available, or explicit approval of a different execution method.
- Minecraft testing: not ready for feature testing. Announce readiness after a successful build and the required automated integration checks. Windows launch command from the repository: `.\gradlew.bat runClient` (Java 21 required).

**Goal:** Add gold-rimmed Hopper Botany Pot upgrades that use configurable Ars, liquid, or gaseous Source for bonemeal-equivalent growth.

**Architecture:** Extend the pinned Botany Pots block and block entity classes, preserving upstream hopper behavior. Keep scheduling in a Minecraft-independent state machine, resource access in small adapters, and registration/assets driven by one variant list. Reuse the existing menu and crop/soil renderer rather than copy them.

**Tech Stack:** Java 21, Minecraft 1.21.1, NeoForge 21.1.250, ModDevGradle 2.0.148, Gradle 9.2.1, JUnit Jupiter 5.11.4, NeoForge GameTests and data generation.

**Spec:** `docs/superpowers/specs/2026-11-03-ars-botany-pots-design.md` (approved).

## Global Constraints

- Work on `feature/source-powered-pots` in `C:\Users\admin\personal\BotanyPots-Starbunklemania-Integration`.
- Target ATM10Aero 0.8.0: Botany Pots 21.1.44, Ars Nouveau 5.13.3, StarbuncleMania 1.5.9, Mekanism 10.7.19.85. Retain all published dependency pins and reference-only submodules.
- Use `sauce:source_fluid` and `starbunclemania:source_gas`. Do not introduce replacement resources.
- Defaults: Jar 2,000 Source every 80 ticks, 100-tick reset window, boosts every 20 ticks, range 3 including diagonals; liquid 250 mB/10 ticks; gas 125 units/5 ticks.
- Separate, coexisting input-only buffers each hold exactly one configured operation. Liquid/gas connections use six faces only.
- Preserve ordinary unpowered growth and all Hopper Botany Pot features. Leave upstream fertilizer behavior unchanged.
- No product code until plan review. No commits, subagents, or worktrees without explicit user consent/request. Task checkpoints below are reviews, not commits.
- Each implementation task follows red/green/refactor: observe the intended failing test before implementation, then verify passing tests. Do not count test-discovery or classpath errors as behavioral failures.
- Windows commands below run from the workspace in PowerShell. From this harness's Linux shell, invoke Windows tools explicitly and translate paths as needed.

## Review Focus

1. A faster resource arrives during a slower cooldown: preserve that cooldown and prevent duplicate boosts (Task 2).
2. A container allows simulation but returns less during extraction: account only for actual transfer without overfilling or free growth (Task 4).
3. A recipe reload removes or replaces bonemeal: stop spending Source until a supported matching recipe exists (Task 3).
4. A save contains more resource than a reduced configured capacity: clamp safely and never duplicate resources (Task 5).
5. A glazed or unusually textured pot is upgraded: preserve material/orientation and show the rim in both placed and inventory rendering (Tasks 6 and 7).

## File responsibilities and interfaces

All Java paths below are under `src/main/java/io/github/gaiusred/sourcepots/`, unless prefixed otherwise. Unit tests mirror these packages under `src/test/java/`. GameTests live under `src/main/java/io/github/gaiusred/sourcepots/gametest/` and run only in the configured development test namespace.

- `config/SourcePotsConfig.java`: NeoForge server configuration and validated conversion to immutable settings.
- `source/SourceSettings.java`: plain Java settings record. Nested `ModeSettings(long cost, int intervalTicks)` for liquid/gas; Jar cost/range/payment/window/boost values as explicit fields.
- `source/SourceMode.java`: `GAS`, `LIQUID`, `JAR`; default tie-breaking order only.
- `source/SourceState.java`: mutable per-pot Jar remaining ticks, Jar retry cooldown, shared boost cooldown; no world, capability, or serialization dependencies.
- `source/SourceScheduler.java`: `boolean tick(SourceState state, SourceSettings settings, boolean growthEligible, SourceAccess access)`; returns whether exactly one boost was purchased/authorized this tick. Defines tick boundary semantics in Task 2.
- `source/SourceAccess.java`: only two methods: `boolean canPay(SourceMode mode)` and `boolean pay(SourceMode mode)`. Existing paid Jar power is tracked by scheduler state, not paid again by this interface.
- `growth/BonemealGrowth.java`: `int growthAmount(BotanyPotBlockEntity pot, ServerLevel level)` returns the supported active recipe's growth increment, or zero if ineligible/unsupported; `boolean apply(BotanyPotBlockEntity pot, int amount)` adds growth with the upstream cap and marks updates.
- `source/PotSourceAccess.java`: implements `SourceAccess`, owns the fluid/chemical buffers and adapters, exposes capability handlers, and pulls from six neighboring handlers. No growth or timer decisions.
- `source/JarSourceAccess.java`: `boolean canPay(ServerLevel level, BlockPos pos, SourceSettings settings)` and `boolean pay(...)`; delegates to Ars helpers and particles.
- `block/ArsBotanyPotBlock.java`, `block/ArsBotanyPotBlockEntity.java`: upstream extension, ticker, orchestration, persistence.
- `registry/PotVariants.java`: immutable list of `PotVariant(String basePath, ResourceLocation originalItem, ResourceLocation material, ResourceLocation materialTop)` used for registration and assets. Ars IDs use the original hopper pot path under `sourcepots`.
- `registry/ModContent.java`: registrations and original-to-Ars mapping. One custom block entity type for all mapped blocks, using upstream-compatible generic types where required by the constructor.
- `client/SourcePotsClient.java`: client-only renderer registration using `BotanyPotRenderer`.
- `data/PotDataProvider.java`: recipes, loot, models, blockstates, translations and block/item tags derived from `PotVariants` into `src/generated/resources/`.
- `SourcePots.java`: event wiring only. Do not turn the entry point into gameplay logic.
- `docs/verification/ars-botany-pots.md`: repeatable manual checks and evidence, including actual Minecraft launches and limitations.

Keep Minecraft-dependent classes out of pure scheduler tests. Avoid factories, general resource frameworks, fake players, or abstractions for hypothetical future fuels. Put nontrivial policy in the scheduler and reuse it from every variant.

## Task 1: Validated settings with a working unit-test harness

**Files:** modify `build.gradle`; create `config/SourcePotsConfig.java`, `source/SourceSettings.java`, and `src/test/java/io/github/gaiusred/sourcepots/source/SourceSettingsTest.java`.

**Interfaces:** produce `SourceSettings.defaults()` and `SourcePotsConfig.snapshot()` returning `SourceSettings`; record construction validates all costs/intervals/windows as positive and range as positive with a documented safe maximum of 16. Liquid cost must fit a positive fluid-handler integer; gas cost uses long.

- [ ] Add JUnit Jupiter 5.11.4, the matching JUnit platform launcher, `useJUnitPlatform()`, and the first settings test. Run ` .\gradlew.bat test --tests '*SourceSettingsTest'`; confirm discovery works and the new default assertion fails before settings implementation.
- [ ] Add tests named `defaultsMatchSpec`, `rejectsNonPositiveValues`, and `rejectsUnsafeRangeAndFluidCapacity`: assert 2000/80/100/20/3, 250/10 and 125/5; reject zero, negatives, range 17, and liquid amounts above `Integer.MAX_VALUE`.
- [ ] Implement immutable settings and described server config keys exactly as in the spec. Buffer capacity is derived from cost, never a second setting. Require restart for these settings.
- [ ] Run ` .\gradlew.bat test --tests '*SourceSettingsTest'` and ` .\gradlew.bat build`; require passing tests and successful compilation. Refactor validation duplication before the checkpoint.

## Task 2: Deterministic Source scheduling

**Files:** create `source/SourceMode.java`, `source/SourceState.java`, `source/SourceAccess.java`, `source/SourceScheduler.java`, and matching `SourceSchedulerTest.java`.

**Interfaces:** consume Task 1 settings and expose the interfaces listed above. Inject an in-memory counting `SourceAccess` in tests; production uses Task 4.

- [ ] Write tests before implementation: `gasBoostsAt0And5`, `liquidBoostsAt0And10`, `jarPaysAt0And80`, `jarRenewsTo100Not200`, `failedRenewalKeepsPaidRemainder`, and `invalidGrowthNeverPays`.
- [ ] Pin tick semantics: decrement existing timers at the start of each tick; a successful purchase then sets Jar remaining ticks to 100 and payment cooldown to 80. A boost sets the shared cooldown to its interval. With the initial tick numbered zero, assert Jar boosts at 0/20/40/60/80, payment counts 1 through tick 79 and 2 at tick 80, and remaining duration 100 immediately after renewal.
- [ ] Run ` .\gradlew.bat test --tests '*SourceSchedulerTest'`; observe failure for the missing scheduler behavior.
- [ ] Implement the scheduler: order funded modes by configured interval, break ties GAS/LIQUID/JAR, never renew Jar while a faster funded mode is selected, and allow paid Jar fallback. Jar payment/renewal can occur when due independently of the shared boost timer, but only while growth is eligible and Jar is the selected mode. Failed Jar attempts set the retry cooldown to 80. Do not require a new Jar payment to use remaining paid time.
- [ ] Add and run tests `fasterInputWaitsForExistingCooldown`, `oneBoostPerTick`, `configuredIntervalsChangePriority`, `tiesPreferGasThenLiquid`, `jarExpiresDuringGasAndInvalidGrowth`, `emptyInputsProduceNoBoost`, and `failedJarPaymentRetriesAfter80Ticks`. Assert exact payment/boost counts and elapsed ticks.
- [ ] Refactor duplicated countdown logic; run all unit tests. Deliver a tested state machine with no Minecraft imports.

## Task 3: Bonemeal growth without player or fertilizer-cooldown changes

**Files:** create `growth/BonemealGrowth.java`, `gametest/BonemealGrowthGameTests.java`; modify `build.gradle` for a `gameTestServer` run with the `sourcepots` test namespace.

**Interfaces:** implement `growthAmount(...)` and `apply(...)` from the file map. Use a synthetic bonemeal interaction stack only for recipe lookup; never invoke `BasicFertilizer.apply()` or construct a fake player. No recipe cache initially, so reload cannot retain a stale recipe.

- [ ] Add discoverable GameTests using the pinned NeoForge GameTest API and a programmatic empty structure/template. First establish test discovery with a known assertion; then add the failing tests below before growth code.
- [ ] Add `bonemealAdds400To600`, `growthCapsTwentyTicksBeforeHarvest`, `shortCropCannotBeBoosted`, `invalidSoilOrSeedReturnsZero`, and `sourceDoesNotSetManualBonemealCooldown`. Assert the resulting delta, cap, and `canBonemeal()` state.
- [ ] Run ` .\gradlew.bat runGameTestServer`; require named behavioral failures, not startup failures.
- [ ] Implement recipe lookup with `Fertilizer.getFertilizer`, a bonemeal lookup context, and `BasicFertilizer.getProperties().growthAmount()`. Check crop/soil compatibility and sustained growth. Unsupported, missing, or nonpositive-growth recipes return zero without spending Source; bound diagnostics per recipe identity/reload.
- [ ] Add `recipeReloadUsesNewBonemealAmount` and `missingOrUnsupportedRecipeDoesNotSpend`, using a development test datapack/reload fixture with fixed 500 then 100 growth amounts. Verify manual fertilizer still uses its own recipe/cooldown and Source does not consume an item.
- [ ] Run GameTests and unit tests; require all discovered tests to pass. Preserve the pinned upstream finishing cap and natural harvest behavior.

## Task 4: One-operation buffers and real resource access

**Files:** create `source/PotSourceAccess.java`, `source/JarSourceAccess.java`, `gametest/SourceAccessGameTests.java`; create small package-local helpers only if fluid versus chemical API differences require them.

**Interfaces:** implement `SourceAccess`; expose `IFluidHandler fluidHandler()` and Mekanism `IChemicalHandler chemicalHandler()`. Constructor receives the server level/position, settings, and a current-growth-eligibility supplier. `void pullAdjacent()` fills available space from touching sides in stable `Direction.values()` order. Neither handler permits external extraction.

- [ ] Write failing GameTests `buffersCoexistAt250And125`, `rejectsWrongResources`, `partialFillCannotPay`, `fullBufferRejectsMore`, `invalidGrowthRejectsIntake`, `capRejectsIntake`, and `automationCannotExtract`. Assert accepted/remaining/consumed amounts, including simulated operations that must not mutate storage.
- [ ] Run the test server to observe the new behavioral failures.
- [ ] Implement buffers with native fluid/chemical stacks, exact capacity derived from settings, changed-state notifications, and acceptance gated by current growth eligibility. `pay` drains only a full operation and succeeds only for that full amount. Read only loaded neighbors and use the face opposite the pot's scanned direction for neighbor handlers.
- [ ] Add tests `sixFacesAcceptInput`, `diagonalFluidAndGasIgnored`, `deniedSideIsNotPulled`, `multipleNeighborsFillOnlyOneOperation`, and `shortExecuteTransferDoesNotCreateFreeBoost`. Simulation can guide a pull, but buffer only the actual executed result; retain partial amounts safely.
- [ ] Implement Jar access with `SourceUtil.hasSourceNearby` and `takeSourceMultipleWithParticles`, using the configured cost/range. Preserve upstream multi-provider and creative semantics; do not write an alternative jar scanner.
- [ ] Add tests `jarAtThreeBlockDiagonalWorks`, `jarAtFourBlocksDoesNot`, `jarsCombineTo2000`, `insufficientJarsDoNotFundBoost`, `creativeJarWorks`, and `pullableGasPreventsJarPurchase`.
- [ ] Run unit tests and GameTests. Confirm both capabilities remain registered independently and inactive growth does not drain suppliers.

## Task 5: Playable Ars hopper blocks and persisted state

**Files:** create `block/ArsBotanyPotBlock.java`, `block/ArsBotanyPotBlockEntity.java`, `registry/PotVariants.java`, `registry/ModContent.java`, `client/SourcePotsClient.java`, `gametest/ArsPotGameTests.java`; modify `SourcePots.java`.

**Interfaces:** `ModContent.register(IEventBus bus)` registers blocks/items/type; `SourcePotsClient` handles client-only renderer events. The block uses `PotType.HOPPER` and overrides construction/ticker for the custom type. The block entity invokes the upstream ticker once, computes one growth increment/eligibility, pulls input, runs the scheduler, then applies at most one authorized increment.

- [ ] Create the shared variant list from every pinned upstream `*_hopper_botany_pot` blockstate/model, preserving the material and top texture references. Test list uniqueness and a one-to-one match to the pinned source inventory. Do not add addon variants by guessing.
- [ ] Add failing GameTests `unpoweredMatchesUpstreamHopper`, `poweredPotHarvestsAndExports`, `cropChangeDoesNotGrantPower`, `saveLoadKeepsBothBuffersAndTimers`, and `smallerConfiguredCapacityClampsSave`. Include missing/malformed persisted values and saturation, avoiding negative quantities or timer overflow.
- [ ] Run the tests, then implement registrations, capabilities, state persistence and server tick orchestration. Use constructor types compatible with the upstream `Supplier<BlockEntityType<BotanyPotBlockEntity>>`; avoid unchecked generic casts spread across files.
- [ ] Register the upstream `BotanyPotRenderer` for the custom type on the client. Reuse `BotanyPotMenu.potMenuServer` through inheritance. Register item automation capability equivalents for the custom block entity so hopper inventory remains accessible.
- [ ] Add tests `normalPotUnchanged`, `hopperMenuUsesAll15Slots`, `comparatorTracksGrowth`, `toolAndOutputBehaviorMatchesUpstream`, `breakDropsInventoryButNotStoredPower`, and `reloadDoesNotGrantImmediateExtraBoost`. Check unloaded pots do not receive catch-up growth or spend Source.
- [ ] Run ` .\gradlew.bat test runGameTestServer build`. Require all tests passing and no dedicated-server client-class loading errors. Review the block entity for policy leakage: decisions belong in the scheduler, not repeated in ticker branches.

## Task 6: Appearance-preserving recipes, gold-rim assets, and generated data

**Files:** create `data/PotDataProvider.java`, `gametest/PotDataGameTests.java`, generated resources under `src/generated/resources/`; modify `build.gradle` for a data run and generated resource directory. Add texture/source attribution documentation if derived upstream assets require it.

**Interfaces:** the data provider consumes only `PotVariants` for variant identity and registry mapping. Register a ModDevGradle data run using the pinned NeoForge data-generation arguments; invoke it with ` .\gradlew.bat runData`.

- [ ] Add failing data tests asserting that every mapped variant has one recipe, model, item model, blockstate and self-drop loot table; recipes must contain `A A` / `BCB` / `BBB`, gold nuggets, `c:planks/archwood`, its exact original hopper item, and the matching Ars output.
- [ ] Implement deterministic data generation and derive rim models with the original silhouette/texture orientation. Inspect `botanypots:block/template/hopper_pot` before choosing rim faces; avoid coplanar overlay geometry that flickers. Prefer a shared rim model/texture and variant texture references over duplicated meshes. Check upstream asset licensing before copying or modifying assets; preserve notices where required.
- [ ] Generate assets and test representative plain terracotta, black concrete, and glazed terracotta variants for their original material/top references, rotation, and item-model inheritance. Include required mining/drop tags so breaking behaves like the original pots.
- [ ] Run ` .\gradlew.bat runData` twice; the second run must produce no content changes. Run GameTests and build; require every known hopper variant mapped and no normal pot accepted by the upgrade recipe.

## Task 7: Launch actual Minecraft and perform gameplay verification

**Files:** create `docs/verification/ars-botany-pots.md`; adjust production code only through a reproducing failing test if verification reveals a bug.

**Interfaces:** use the existing `runClient`/`runServer` runs and the assembled mod jar; no separate launch wrapper or authentication automation.

- [ ] Run ` .\gradlew.bat --no-daemon clean test runGameTestServer build`; record commands, tool versions, test counts, and result. This is the final automated gate, not proof of client rendering.
- [ ] Launch ` .\gradlew.bat runServer`; confirm all expected dependency versions, world startup, and successful `stop` shutdown. Keep the server local. Respect manual EULA acceptance for packaged servers; do not expose development offline-authentication servers.
- [ ] Launch **` .\gradlew.bat runClient`** with Java 21 and confirm the Minecraft window opens with NeoForge, `sourcepots`, and all nine configured dependency mods, including selected bundled Sauce. Verify startup/resource logs for errors. If GUI access is unavailable, report the blocker and have the user perform these steps; never mark them passed based on a build.
- [ ] In a local creative world, craft representative variants using the exact recipe. Inspect the gold rim in inventory and placed blocks, all facing directions, soil and tree/crop rendering, menu, harvest tool, output inventory, comparator, and waterlogging. Ask the user to judge the rim appearance; no browser mockups are required.
- [ ] Use valid crops and soil to test Jar-only, liquid-only, gas-only, mixed, and empty inputs with real Ars/Mekanism containers and pipes. Check costs/cadence with automated tick-counter tests as evidence, then visually confirm effects, priority, six-face connections, and Jar diagonals. Remove/change crops and reach the growth cap to verify no supplier drain.
- [ ] Save/quit/reopen the client world with both buffers populated and paid Jar time remaining. Confirm contents, no duplication/free boost, and continued ordinary hopper behavior without Source. Exit cleanly and retain relevant logs/results.
- [ ] With the user's separate backed-up ATM10Aero 0.8.0 instance, install only the built integration jar; test client and dedicated server with the same scenarios and a non-operator player. Do not download/login automatically or modify an existing world without permission. Record blocked or unperformed full-pack checks explicitly; development success alone is not full-pack compatibility.

## Task 8: Documentation and whole-change review

**Files:** modify `README.md`, `docs/compatibility.md`, and `docs/verification/ars-botany-pots.md`.

- [ ] Replace bootstrap-only status with implemented behavior only after Tasks 1–7 supply evidence. Document crafting, config defaults/file location/restart requirement, buffer sizes, priority, growth cap, source range, and development versus full-pack verification limits.
- [ ] Review the complete change against each approved spec section and this plan. Check focused responsibilities, no duplicated per-variant logic, descriptive identifiers, no speculative abstractions, no upstream changes, and no unnecessary dependency/version changes.
- [ ] Run ` .\gradlew.bat test runGameTestServer build`, `git diff --check`, and `git status --short`. Record any residual warning or untested scenario; fix defects with a failing regression test first.
- [ ] Present changed files and verification evidence to the user. Leave changes uncommitted. Ask for explicit consent only if committing is requested as the next action.

## Plan review

Coverage: configuration and defaults (Task 1), timing/priority (Task 2), fertilizer parity (Task 3), Source access and buffers (Task 4), hopper behavior/persistence (Task 5), recipe/appearance variants (Task 6), real client/server and pack verification (Task 7), documentation and review (Task 8).

Clean-code checks: one scheduling policy shared by every variant; one variant map for registration and data; dedicated growth/resource adapters; upstream hopper behavior reused; minimal entry-point wiring; no fake players, copied hopper implementation, or general-purpose fuel framework.

The user approved this plan and requested subagent execution. Execution has not started because the session has no subagent tool. Keep this file current as implementation and verification progress. Consent covers committing and pushing the docs first; later implementation commits require separate explicit consent.
