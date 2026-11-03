# Ars Botany Pots Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. The user requested subagents; do not create worktrees. Do not silently fall back to inline execution.

## Live progress

- Task 14: cleanup and verification complete. Implementation checkpoint `0bad75f` is committed. All 81 GameTests and 29 unit tests are retained in a development-only test mod; production jars exclude test code. The clean build and data/package/isolation checks pass. README and display name use **Ars Botany Pots**, while `sourcepots` save/registry identity and gameplay remain unchanged. Cleanup commit and GitHub PR delivery are prepared; GitHub is the delivery record.

- Task 13 amendment: complete and independently reviewed. Growth ranges are Jar 600–800, liquid 500–700 and gas 400–600; gas costs 250 units. Defaults and the backed-up/migrated dev config match all 15 settings. Final gate passes 81 GameTests, 29 unit tests and build in 49 seconds. Updated client PID 26924 reached Singleplayer, responding; JEI initialized at 04:36:36. Ready for user testing.

- Design and implementation plan: approved.
- Documentation: approved; this file and the spec form the initial docs-only checkpoint.
- Tasks 1-6, 8, 10 and 11: complete and independently reviewed. Task 7: launches verified, detailed user gameplay checks pending. Task 9: corrected client loaded JEI 19.64.0.455 and MezzConfig 0.6.6; JEI runtime initialization is verified. User confirmation of Creative/JEI item visibility and filtering remains pending.
- Task 11: complete. Shared growth defaults are 600–1,200 and all 61 Ars pots have a separate Creative category. Its checkpoint passed 68 GameTests, 28 unit tests and build, with independent review approval. Saved liquid/gas intervals were preserved at that checkpoint; Task 12 subsequently updates the dev config and Source finishing behavior.
- Task 12: complete and independently reviewed. Source maturity, persisted overflow and powered pause bypass passed 79 GameTests, 28 unit tests and build. Its recorded rate/config checkpoint is superseded by Task 13. The Task 12 client reached Singleplayer and initialized JEI; detailed gameplay/full-pack checks remain open.
- Execution: subagent tools are available and the user requested their use on 2026-11-03. Working in the existing feature branch, without worktrees or commits.
- Task 1 evidence: four settings tests pass after three intended behavioral failures against the stub; `gradlew.bat build` succeeds. Independent review approved spec compliance and code quality with no findings.
- Task 2 evidence: all 24 unit tests pass (20 scheduler, four settings); independent review approved spec compliance and code quality.
- Task 3 evidence: all nine growth GameTests (seven planned plus two cleanup regressions) and all 24 unit tests pass. Review approved the growth logic and the subsequent fixture cleanup fixes. Known dependency warnings/errors are recorded in the test report.
- Task 4 evidence: 18 resource GameTests failed against stubs before implementation. All 27 GameTests and 24 unit tests pass; independent review approved spec compliance and code quality with no findings. Deprecated chemical-stack calls were replaced before the final run.
- Task 5 evidence: the final combined gate passes 45 GameTests, 26 unit tests and build. Independent review approved spec compliance and code quality with two nonblocking pinned FML subscriber deprecation warnings. The complete 61-variant inventory, saved state, registered capabilities and live-load handler identity are covered; unload evidence uses a removed/detached entity rather than a forced chunk unload.
- Task 6 evidence: the final clean gate passes 52 GameTests, 26 unit tests and build. All 310 generated JSON resources are deterministic and packaged without the data cache. Independent review approved the assets, tag parity, self-drop test and guarded datagen compatibility hook. One minor test-formatting finding is deferred to the final review.
- Minecraft testing: ready for initial in-game testing; announced to the user. Use `build/libs/sourcepots-0.1.0-dev.jar` in a separate backed-up ATM10Aero 0.8.0 instance. The user confirmed appearance and basic Source operation; detailed visual checks and full-pack compatibility remain pending. Development launch: `.\gradlew.bat runClient` (Java 21 required).
- User testing feedback: pots were absent from Creative because no tab entries were registered. Task 9 adds the entries and requested pack-matched JEI. The prior client saved and exited normally. The updated launch opened and reached Singleplayer, but its mod list still omits JEI/MezzConfig; the standalone discovery probe did not establish actual launch success.
- Later user confirmation: appearance and operation with Jar, liquid and gaseous Source are approved. Task 10 passes 59 GameTests, 26 unit tests and build. One-face/six-face growth, spending and cooldowns match. Mature harvests match upstream hopper and normal manual pots across 16 controlled cases (16 wheat, 44 seeds each). The user's existing saved world configuration is preserved.

**Goal:** Add gold-rimmed Hopper Botany Pot upgrades that use configurable Ars, liquid, or gaseous Source for bonemeal-equivalent growth.

**Architecture:** Extend the pinned Botany Pots block and block entity classes, preserving upstream hopper behavior. Keep scheduling in a Minecraft-independent state machine, resource access in small adapters, and registration/assets driven by one variant list. Reuse the existing menu and crop/soil renderer rather than copy them.

**Tech Stack:** Java 21, Minecraft 1.21.1, NeoForge 21.1.250, ModDevGradle 2.0.148, Gradle 9.2.1, JUnit Jupiter 5.11.4, NeoForge GameTests and data generation.

**Spec:** `docs/superpowers/specs/2026-11-03-ars-botany-pots-design.md` (approved).

## Global Constraints

- Work on `feature/source-powered-pots` in `C:\Users\admin\personal\BotanyPots-Starbunklemania-Integration`.
- Target ATM10Aero 0.8.0: Botany Pots 21.1.44, Ars Nouveau 5.13.3, StarbuncleMania 1.5.9, Mekanism 10.7.19.85. Retain all published dependency pins and reference-only submodules.
- Use `sauce:source_fluid` and `starbunclemania:source_gas`. Do not introduce replacement resources.
- Current target defaults (Task 13 amendment): Jar 2,500 Source every 80 ticks, 100-tick reset window, boosts every 10 ticks, range 3, growth 600–800; liquid 500 mB/5 ticks, growth 500–700; gas 250 native chemical units/1 tick, growth 400–600. All growth bounds are inclusive and independently configurable. Earlier completed task steps retain historical values superseded by later requests.
- Separate, coexisting input-only buffers each hold exactly one configured operation. Liquid/gas connections use six faces only.
- Source growth uses the selected input mode's independently configurable range. Recipe matching still determines eligibility; ordinary manual fertilizer keeps its recipe amount.
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

- [x] Add JUnit Jupiter 5.11.4, the matching JUnit platform launcher, `useJUnitPlatform()`, and the first settings test. Run ` .\gradlew.bat test --tests '*SourceSettingsTest'`; confirm discovery works and the new default assertion fails before settings implementation.
- [x] Add tests named `defaultsMatchSpec`, `rejectsNonPositiveValues`, and `rejectsUnsafeRangeAndFluidCapacity`: assert 2000/80/100/20/3, 250/10 and 125/5; reject zero, negatives, range 17, and liquid amounts above `Integer.MAX_VALUE`.
- [x] Implement immutable settings and described server config keys exactly as in the spec. Buffer capacity is derived from cost, never a second setting. Require restart for these settings.
- [x] Run ` .\gradlew.bat test --tests '*SourceSettingsTest'` and ` .\gradlew.bat build`; require passing tests and successful compilation. Refactor validation duplication before the checkpoint.

## Task 2: Deterministic Source scheduling

**Files:** create `source/SourceMode.java`, `source/SourceState.java`, `source/SourceAccess.java`, `source/SourceScheduler.java`, and matching `SourceSchedulerTest.java`.

**Interfaces:** consume Task 1 settings and expose the interfaces listed above. Inject an in-memory counting `SourceAccess` in tests; production uses Task 4.

- [x] Write tests before implementation: `gasBoostsAt0And5`, `liquidBoostsAt0And10`, `jarPaysAt0And80`, `jarRenewsTo100Not200`, `failedRenewalKeepsPaidRemainder`, and `invalidGrowthNeverPays`.
- [x] Pin tick semantics: decrement existing timers at the start of each tick; a successful purchase then sets Jar remaining ticks to 100 and payment cooldown to 80. A boost sets the shared cooldown to its interval. With the initial tick numbered zero, assert Jar boosts at 0/20/40/60/80, payment counts 1 through tick 79 and 2 at tick 80, and remaining duration 100 immediately after renewal.
- [x] Run ` .\gradlew.bat test --tests '*SourceSchedulerTest'`; observe failure for the missing scheduler behavior.
- [x] Implement the scheduler: order funded modes by configured interval, break ties GAS/LIQUID/JAR, never renew Jar while a faster funded mode is selected, and allow paid Jar fallback. Jar payment/renewal can occur when due independently of the shared boost timer, but only while growth is eligible and Jar is the selected mode. Failed Jar attempts set the retry cooldown to 80. Do not require a new Jar payment to use remaining paid time.
- [x] Add and run tests `fasterInputWaitsForExistingCooldown`, `oneBoostPerTick`, `configuredIntervalsChangePriority`, `tiesPreferGasThenLiquid`, `jarExpiresDuringGasAndInvalidGrowth`, `emptyInputsProduceNoBoost`, and `failedJarPaymentRetriesAfter80Ticks`. Assert exact payment/boost counts and elapsed ticks.
- [x] Refactor duplicated countdown logic; run all unit tests. Deliver a tested state machine with no Minecraft imports.

Verified: 20 scheduler tests plus four settings tests pass. An unpaid Jar in retry cooldown is not a usable mode, so a slower funded buffer can run. A due Jar check that finds insufficient Source starts the retry timer; faster funded modes prevent that check entirely.

## Task 3: Bonemeal growth without player or fertilizer-cooldown changes

**Files:** create `growth/BonemealGrowth.java`, `gametest/BonemealGrowthGameTests.java`; modify `build.gradle` for a `gameTestServer` run with the `sourcepots` test namespace.

**Interfaces:** implement `growthAmount(...)` and `apply(...)` from the file map. Use a synthetic bonemeal interaction stack only for recipe lookup; never invoke `BasicFertilizer.apply()` or construct a fake player. No recipe cache initially, so reload cannot retain a stale recipe.

- [x] Add discoverable GameTests using the pinned NeoForge GameTest API and a programmatic empty structure/template. First establish test discovery with a known assertion; then add the failing tests below before growth code.
- [x] Add `bonemealAdds400To600`, `growthCapsTwentyTicksBeforeHarvest`, `shortCropCannotBeBoosted`, `invalidSoilOrSeedReturnsZero`, and `sourceDoesNotSetManualBonemealCooldown`. Assert the resulting delta, cap, and `canBonemeal()` state.
- [x] Run ` .\gradlew.bat runGameTestServer`; require named behavioral failures, not startup failures.
- [x] Implement recipe lookup with `Fertilizer.getFertilizer`, a bonemeal lookup context, and `BasicFertilizer.getProperties().growthAmount()`. Check crop/soil compatibility and sustained growth. Unsupported, missing, or nonpositive-growth recipes return zero without spending Source; bound diagnostics per recipe identity/reload.
- [x] Add `recipeReloadUsesNewBonemealAmount` and `missingOrUnsupportedRecipeDoesNotSpend`, using a development test datapack/reload fixture with fixed 500 then 100 growth amounts. Verify manual fertilizer still uses its own recipe/cooldown and Source does not consume an item.
- [x] Run GameTests and unit tests; require all discovered tests to pass. Preserve the pinned upstream finishing cap and natural harvest behavior.

Verified: nine GameTests pass, including two added cleanup regressions. Test-only mock players exercise upstream manual fertilizer use; production growth creates no player and never invokes `BasicFertilizer.apply()`. The real recipe reload test restores the original packs, recipes, and world configuration, including the tested failure path.

## Task 4: One-operation buffers and real resource access

**Files:** create `source/PotSourceAccess.java`, `source/JarSourceAccess.java`, `gametest/SourceAccessGameTests.java`; create small package-local helpers only if fluid versus chemical API differences require them.

**Interfaces:** implement `SourceAccess`; expose `IFluidHandler fluidHandler()` and Mekanism `IChemicalHandler chemicalHandler()`. Constructor receives the server level/position, settings, and a current-growth-eligibility supplier. `void pullAdjacent()` fills available space from touching sides in stable `Direction.values()` order. Neither handler permits external extraction.

- [x] Write failing GameTests `buffersCoexistAt250And125`, `rejectsWrongResources`, `partialFillCannotPay`, `fullBufferRejectsMore`, `invalidGrowthRejectsIntake`, `capRejectsIntake`, and `automationCannotExtract`. Assert accepted/remaining/consumed amounts, including simulated operations that must not mutate storage.
- [x] Run the test server to observe the new behavioral failures.
- [x] Implement buffers with native fluid/chemical stacks, exact capacity derived from settings, changed-state notifications, and acceptance gated by current growth eligibility. `pay` drains only a full operation and succeeds only for that full amount. Read only loaded neighbors and use the face opposite the pot's scanned direction for neighbor handlers.
- [x] Add tests `sixFacesAcceptInput`, `diagonalFluidAndGasIgnored`, `deniedSideIsNotPulled`, `multipleNeighborsFillOnlyOneOperation`, and `shortExecuteTransferDoesNotCreateFreeBoost`. Simulation can guide a pull, but buffer only the actual executed result; retain partial amounts safely.
- [x] Implement Jar access with `SourceUtil.hasSourceNearby` and `takeSourceMultipleWithParticles`, using the configured cost/range. Preserve upstream multi-provider and creative semantics; do not write an alternative jar scanner.
- [x] Add tests `jarAtThreeBlockDiagonalWorks`, `jarAtFourBlocksDoesNot`, `jarsCombineTo2000`, `insufficientJarsDoNotFundBoost`, `creativeJarWorks`, and `pullableGasPreventsJarPurchase`.
- [x] Run unit tests and GameTests. Confirm independent fluid and chemical handler exposure and that inactive growth does not drain suppliers. Production block-entity capability registration is verified in Task 5, when the custom type exists.

## Task 5: Playable Ars hopper blocks and persisted state

**Files:** create `block/ArsBotanyPotBlock.java`, `block/ArsBotanyPotBlockEntity.java`, `registry/PotVariants.java`, `registry/ModContent.java`, `client/SourcePotsClient.java`, `gametest/ArsPotGameTests.java`; modify `SourcePots.java`.

**Interfaces:** `ModContent.register(IEventBus bus)` registers blocks/items/type; `SourcePotsClient` handles client-only renderer events. The block uses `PotType.HOPPER` and overrides construction/ticker for the custom type. The block entity invokes the upstream ticker once, computes one growth increment/eligibility, pulls input, runs the scheduler, then applies at most one authorized increment.

- [x] Create the shared variant list from every pinned upstream `*_hopper_botany_pot` blockstate/model, preserving the material and top texture references. Test list uniqueness and a one-to-one match to the pinned source inventory. Do not add addon variants by guessing.
- [x] Add failing GameTests `unpoweredMatchesUpstreamHopper`, `poweredPotHarvestsAndExports`, `cropChangeDoesNotGrantPower`, `saveLoadKeepsBothBuffersAndTimers`, and `smallerConfiguredCapacityClampsSave`. Include missing/malformed persisted values and saturation, avoiding negative quantities or timer overflow.
- [x] Run the tests, then implement registrations, capabilities, state persistence and server tick orchestration. Use constructor types compatible with the upstream `Supplier<BlockEntityType<BotanyPotBlockEntity>>`; avoid unchecked generic casts spread across files.
- [x] Register the upstream `BotanyPotRenderer` for the custom type on the client. Reuse `BotanyPotMenu.potMenuServer` through inheritance. Register item automation capability equivalents for the custom block entity so hopper inventory remains accessible.
- [x] Add tests `normalPotUnchanged`, `hopperMenuUsesAll15Slots`, `comparatorTracksGrowth`, `toolAndOutputBehaviorMatchesUpstream`, `breakDropsInventoryButNotStoredPower`, and `reloadDoesNotGrantImmediateExtraBoost`. Check unloaded pots do not receive catch-up growth or spend Source.
- [x] Run ` .\gradlew.bat test runGameTestServer build`. Require all tests passing and no dedicated-server client-class loading errors. Review the block entity for policy leakage: decisions belong in the scheduler, not repeated in ticker branches.

## Task 6: Appearance-preserving recipes, gold-rim assets, and generated data

**Files:** create `data/PotDataProvider.java`, `gametest/PotDataGameTests.java`, generated resources under `src/generated/resources/`; modify `build.gradle` for a data run and generated resource directory. Add texture/source attribution documentation if derived upstream assets require it.

**Interfaces:** the data provider consumes only `PotVariants` for variant identity and registry mapping. Register a ModDevGradle data run using the pinned NeoForge data-generation arguments; invoke it with ` .\gradlew.bat runData`.

- [x] Add failing data tests asserting that every mapped variant has one recipe, model, item model, blockstate and self-drop loot table; recipes must contain `A A` / `BCB` / `BBB`, gold nuggets, `c:planks/archwood`, its exact original hopper item, and the matching Ars output.
- [x] Implement deterministic data generation and derive rim models with the original silhouette/texture orientation. Inspect `botanypots:block/template/hopper_pot` before choosing rim faces; avoid coplanar overlay geometry that flickers. Prefer a shared rim model/texture and variant texture references over duplicated meshes. Check upstream asset licensing before copying or modifying assets; preserve notices where required.
- [x] Generate assets and test representative plain terracotta, black concrete, and glazed terracotta variants for their original material/top references, rotation, and item-model inheritance. Include required mining/drop tags so breaking behaves like the original pots.
- [x] Run ` .\gradlew.bat runData` twice; the second run must produce no content changes. Run GameTests and build; require every known hopper variant mapped and no normal pot accepted by the upgrade recipe.

## Task 7: Launch actual Minecraft and perform gameplay verification

**Files:** create `docs/verification/ars-botany-pots.md`; update `build.gradle` to forward console input to `runServer`; adjust production code only through a reproducing failing test if verification reveals a bug.

**Interfaces:** use the existing `runClient`/`runServer` runs and the assembled mod jar; no separate launch wrapper or authentication automation.

- [x] Run ` .\gradlew.bat --no-daemon clean test runGameTestServer build`; record commands, tool versions, test counts, and result. This is the final automated gate, not proof of client rendering. Completed with the final Task 6 code: 52 GameTests, 26 unit tests and build pass (`task-6-final-verification.log`).
- [x] Enable `standardInput = System.in` for the `runServer` JavaExec task (the pinned plugin defaults to empty input). Bind the development server to `127.0.0.1`, then launch ` .\gradlew.bat runServer`; confirm all expected dependency versions, world startup, and successful `stop` shutdown. Respect manual EULA acceptance for packaged servers; do not expose development offline-authentication servers. Verified loopback port 57561, a new verification world, clean stop/save/exit 0, and exact restoration of original properties.
- [x] Launch **` .\gradlew.bat runClient`** with Java 21 and confirm the Minecraft window opens with NeoForge, `sourcepots`, and all nine configured dependency mods, including selected bundled Sauce. Verify startup/resource logs for errors. Native window existence and startup/resource logs verified; the first client exited cleanly, and the second reached Singleplayer. No direct visual inspection was available; gameplay checks below remain pending. Dependency asset/animation diagnostics are documented.
- [ ] In a local creative world, craft representative variants using the exact recipe. Inspect the gold rim in inventory and placed blocks, all facing directions, soil and tree/crop rendering, menu, harvest tool, output inventory, comparator, and waterlogging. Ask the user to judge the rim appearance; no browser mockups are required.
- [ ] Use valid crops and soil to test Jar-only, liquid-only, gas-only, mixed, and empty inputs with real Ars/Mekanism containers and pipes. Check costs/cadence with automated tick-counter tests as evidence, then visually confirm effects, priority, six-face connections, and Jar diagonals. Remove/change crops and reach the growth cap to verify no supplier drain.
- [ ] Save/quit/reopen the client world with both buffers populated and paid Jar time remaining. Confirm contents, no duplication/free boost, and continued ordinary hopper behavior without Source. Exit cleanly and retain relevant logs/results.
- [ ] With the user's separate backed-up ATM10Aero 0.8.0 instance, install only the built integration jar; test client and dedicated server with the same scenarios and a non-operator player. Do not download/login automatically or modify an existing world without permission. Record blocked or unperformed full-pack checks explicitly; development success alone is not full-pack compatibility.

## Task 8: Documentation and whole-change review

**Files:** modify `README.md`, `docs/compatibility.md`, and `docs/verification/ars-botany-pots.md`; update the obsolete bootstrap description in `src/main/resources/META-INF/neoforge.mods.toml` and implementation status in the approved spec. Keep the approved design, dependency pins, display name and mod ID unchanged.

- [x] Replace bootstrap-only status with implemented behavior only after Tasks 1–7 supply evidence. Document crafting, config defaults/file location/restart requirement, buffer sizes, priority, growth cap, source range, and development versus full-pack verification limits.
- [x] Review the complete change against each approved spec section and this plan. Check focused responsibilities, no duplicated per-variant logic, descriptive identifiers, no speculative abstractions, no upstream changes, and no unnecessary dependency/version changes.
- [x] Run ` .\gradlew.bat test runGameTestServer build`, `git diff --check`, and `git status --short`. Record any residual warning or untested scenario; fix defects with a failing regression test first. Final correction gate: 60 GameTests, 26 unit tests and build pass; scoped review approved the corrections. Two supported pinned-subscriber deprecation warnings remain documented.
- [x] Present changed files and verification evidence to the user. Leave changes uncommitted. Ask for explicit consent only if committing is requested as the next action.

## Task 9: Creative visibility and pack-matched JEI (user-requested follow-up)

Execute before Task 8. The user requested this during live Minecraft testing on 2026-11-03.

**Files:** modify `registry/ModContent.java`, `SourcePots.java`, `build.gradle`, `gradle.properties`, `compatibility/atm10aero-0.8.0.json`; add `gametest/CreativeContentsGameTests.java` and register it with the existing development test environment.

**Requirements:** retain the display name `Botany Pots - StarbuncleMania Integration` and ID `sourcepots`. Add all 61 mapped items to the existing `botanypots:tab` Creative tab with parent-and-search visibility; do not create another tab or populate unrelated tabs. Add JEI only to the development client's JVM classpath, excluding the legacy bootstrap classpath, without a mandatory distribution dependency. The hash-verified ATM10Aero 0.8.0 manifest pins project 238222, file 9205121: `jei-1.21.1-neoforge-19.64.0.455.jar`, SHA-256 `f7a9de3d7ee07eed8d293a1a1a0f6afaac556eb43c103523457bd07ebb551e7b`. It bundles `mezz_config` 0.6.6. Preserve all earlier pins. Keep automated GameTests in a separate `run/gametest` directory so they do not overwrite logs/config/world data of the user's active client.

- [x] Verify the pack archive and manifest hashes against the recorded baseline; read the JEI entry and published jar metadata rather than choose the latest version.
- [x] Add and run behavioral regressions for all 61 parent/search Creative entries and no additions to unrelated tabs. Observe the missing-entry failure before registering the handler.
- [x] Register the tab-content listener, add `jei_file=9205121` and the client-only dependency, and record provenance. Isolate the GameTest working directory. Final launcher correction uses a separate resolvable configuration added to `runClient`'s JVM classpath. Keep JEI off the legacy bootstrap list, which causes FML to skip its path during mod discovery.
- [x] Run unit tests, isolated GameTests and build; verify the shipped integration jar does not bundle JEI. Review the change independently. The original gate passed 55 GameTests and 26 unit tests; the final gameplay correction gate passes 60/26. The metadata probe alone proved insufficient in an actual launch. The final direct-JVM correction passes generated launch-input assertions and build, with JEI absent from bootstrap, nonclient runs, test/product classpaths and shipped jars. Independent review approved the correction. Actual JEI initialization remains the next step.
- [x] After the old client saved and closed normally, launch the updated client once and verify JEI 19.64.0.455 plus bundled MezzConfig 0.6.6 load. Verified in `task-11-client.log`; JEI runtime startup completed at 03:52:51. No active world was interrupted.
- [ ] Let the user confirm item visibility, the separate Ars Botany Pots category, JEI `@integration` filtering and recipe lookup.

## Task 10: Faster default inputs, shared-face cadence, and mature wheat parity

Requested during user testing on 2026-11-03. The user confirmed appearance and operation with all three Source modes, asked about mature wheat producing more seeds than wheat, then requested faster default intervals.

**Files:** update `source/SourceSettings.java`, affected unit/GameTests, and factual default tables in the spec/README/verification docs. Add focused GameTests for mature harvest parity and one-face versus six-face input. Keep scheduling/resource policy in the existing classes; change production logic only if a reproduced defect requires it.

**Requirements:** liquid interval defaults to 5 ticks and gas to 1 tick. Costs remain 250 mB and 125 chemical units. Jar defaults are unchanged. Six faces expose one shared liquid buffer and one shared gas buffer per pot, each one operation; adding suppliers cannot add boost authorizations or shorten the configured shared cooldown. Preserve existing world/server configuration overrides; do not edit the user's live world to force new defaults.

- [x] Observe failing assertions for the new 5/1 default intervals before changing production defaults. Retain meaningful coverage of custom intervals, ties and switching cooldowns.
- [x] Add integration checks comparing one adequately supplied face with all six faces for liquid and gas. Assert shared handler/storage identity, exact operation/cooldown behavior and equal growth progress under controlled randomness. Account for an operation retained in the buffer rather than treating intake as immediate boost spending.
- [x] Verify mature Ars harvests against upstream hopper harvests with matching crop, soil, tool and controlled loot randomness. Check ordinary manual pot harvesting separately where its upstream semantics differ. Inspect the actual loaded/pinned mature-wheat loot and report wheat/seed quantities without changing their balance. Include a Source-capped crop finishing naturally and harvesting once.
- [x] Run unit tests, isolated GameTests and build. Update all current-default documentation, record counts/results and review independently. Note that existing saved world configs retain their explicit intervals until changed and the world/server restarted.

## Task 11: Configurable growth per Source boost and a separate Creative tab

User-requested follow-up: make the minimum and maximum growth ticks configurable with a final user-selected default of 600–1,200 for every Source mode (superseding the initial request to double both bounds). One shared range applies to Jar, liquid and gas: `growth.minTicks=600`, `growth.maxTicks=1200`, inclusive. This replaces the Source boost amount previously taken from the supported bonemeal recipe. Recipe matching, positive recipe growth, crop/soil eligibility, the 20-tick finishing cap, manual bonemeal behavior, resource costs, intervals and shared buffers remain intact. Existing worlds acquire these new keys without rewriting existing settings; configuration changes require a world/server restart.

**Files:** `source/SourceSettings.java`, `config/SourcePotsConfig.java`, `growth/BonemealGrowth.java`, affected growth/settings/registered-mode/reload/cadence GameTests, README, spec and verification notes. Keep the range in validated immutable settings; use one inclusive, overflow-safe sampling implementation shared by all modes. Reject nonpositive or reversed ranges with a clear error. Equal bounds must give a constant amount. Do not change the user's live config/world or restart an active client.

The user also requested a separate Creative category within this task. Register `sourcepots:ars_botany_pots`, titled **Ars Botany Pots**, with an Ars terracotta-pot icon. Populate it with all 61 mapped upgrades, including Creative search visibility. Remove this mod's additions to `botanypots:tab`; preserve original and unrelated tab contents. This supersedes Task 9's shared-tab requirement. Extend `registry/ModContent.java`, event wiring, `CreativeContentsGameTests.java`, the data provider and generated translation with the native tab registration.

- [x] Add behavioral RED tests for 600/1200 defaults, equal/custom bounds, invalid/reversed ranges and large valid bounds. Add registered-mode tests proving all three payment paths use configured growth and preserve the natural finishing cap.
- [x] Implement and wire the shared server settings and growth sampling. Retain supported-recipe checks and manual fertilizer independence; adapt reload checks to prove newly unsupported/nonpositive recipes disable Source after reload rather than expecting recipe amounts to override configured bounds.
- [x] Keep one-face/six-face exact cadence coverage deterministic with a scoped test-only fixed growth range. Restore any temporary config/recipe state even on failure; do not overwrite the user's world settings.
- [x] Observe a behavioral failure for the missing dedicated Creative tab and the remaining shared-tab additions. Register the separate tab, remove the old listener, generate its title translation, and verify all 61 parent/search entries with no normal pot or unrelated tab changes.
- [x] Run unit tests, isolated GameTests and build. Record actual results and artifact hash, update current behavior/default docs, and obtain one independent scoped review. Keep JEI/manual/full-pack acceptance open unless separately verified. Verified: 68 GameTests, 28 unit tests, build successful in 49 seconds; independent scoped review approved. Runtime jar SHA-256: `74ba8a4954c20976120467b86266b04481732d3c3f411f526c1cf10cc30f60db`.

## Task 12: Source overflow and uninterrupted powered harvests

The user explicitly selected all three changes: remove the 20-tick finishing cap for Source boosts, carry excess growth into later cycles, and bypass the upstream five-tick harvest pause for Source-assisted cycles. Also update the actual development config to liquid interval 5, gas interval 1 and shared growth 600–1,200. This supersedes earlier cap/pause requirements for Source acceleration; ordinary manual bonemeal and unpowered pots retain upstream behavior.

**Design:** keep the upstream pot ticker and harvest hooks, loot/yield/tool damage/export behavior, and at most one harvest per pot per server tick. A paid boost adds its configured growth once; use what the current crop needs and retain remaining paid growth for the following cycle. Apply retained growth before buying another boost; a backlog cannot consume repeated payments or create a same-tick harvest loop. Track Source-assisted cycles so only powered harvests bypass the upstream pause. Retain credit safely across save/load, without catch-up while unloaded, and discard it on crop/soil reset or block break. Ineligible crops and unsupported/missing/nonpositive fertilizer recipes cannot accept or spend Source or apply retained growth. Keep six-face shared buffers, payment priority and cooldowns intact. No changes to upstream source or copied harvest implementation.

**Files:** growth adapter, Ars block entity and a small focused growth-state helper if needed; affected growth/persistence/registered-mode/mature-harvest GameTests, README/spec/verification. Root owns the dev-config edit and this plan. Keep integer arithmetic safe at configured maximums and clamp malformed saves. No new dependency, Creative-tab or recipe-asset changes are needed.

The user's later Task 12 rate selection supersedes earlier defaults: Jar payment is 2,500 Source (25% of a standard 10,000-capacity Jar) every 80 eligible ticks, paid duration 100 ticks and boost interval 10 ticks. Liquid remains 250 mB every 5 ticks. Gas becomes 100 native chemical units per boost every tick, equivalent to two 1,000-unit buckets over 20 uninterrupted funded ticks. All modes share growth 600–1,200, and every cost/interval/window/bound remains configurable through the existing keys. The dev config must match these values. Existing resource fixtures and budget assertions must test the new defaults; custom settings remain supported.

- [x] Add behavioral RED tests for reaching maturity without the finishing delay, exact overflow accounting, consecutive Source-assisted harvests without the five-tick pause, and one upstream harvest per tick even with a very large boost.
- [x] Implement growth credit and pause bypass through the existing adapter/ticker, preserving ordinary growth/manual fertilizer, loot and scheduler semantics. Test no extra payment when retained credit completes the current crop. If retained credit is exhausted while the crop remains immature, one normally due paid boost may run that tick.
- [x] Cover persistence, malformed/large credit, reset/break clearing, invalid/missing recipe and blocked-harvest handling, no unloaded catch-up, and shared one-face/six-face cadence. Replace obsolete cap assertions with the approved behavior while retaining loot-parity checks.
- [x] Back up and update only the requested values in `run/config/sourcepots-server.toml`; verify Jar 2500/80/100/10, liquid 250/5, gas 100/1 and growth 600/1200. Backups before both edits are retained; the later request explicitly changes Jar cost/boost interval and gas cost as well as the saved liquid/gas intervals.
- [x] Run unit tests, isolated GameTests and build; record results and artifact hash, update current docs, and obtain an independent scoped code/prose review. Tell the user when the new build can be tested; leave unperformed UI/full-pack checks open.

## Task 13: Higher liquid/gas costs and revised growth range

Historical first Task 13 checkpoint: the user confirmed Task 12 works and requested defaults of: liquid 500 mB per boost, gas 200 chemical units per boost, and shared growth 500–1,100 ticks. Jar settings and all boost intervals remain unchanged. These values stay configurable. Update the actual dev config and run the updated client.

- [x] Observe behavioral RED for the new settings defaults, then update `SourceSettings.defaults()` and affected default-bound/buffer/resource-budget tests. Preserve explicit custom settings and the overflow, pause bypass and one-face/six-face regressions.
- [x] Back up and update the dev config to liquid 500/5, gas 200/1, growth 500/1100; retain Jar 2500/80/100/10 and range 3.
- [x] Run unit tests, isolated GameTests and build; update current README/spec/verification values and artifact evidence, preserving historical checkpoints. Obtain an independent scoped review.
- [x] Launch the updated development client once after confirming no prior client remains. Record startup evidence and tell the user it is ready to test.

### Task 13 amendment: growth ranges per input mode

The user supersedes the shared growth range and gas price: Jar/Source growth 600–800, liquid growth 500–700 and gas growth 400–600, all inclusive; gas costs 250 native chemical units per boost. Liquid remains 500 mB. Jar cost/window/timing and liquid/gas boost intervals stay as previously selected.

Use independent configurable `growthMinTicks` and `growthMaxTicks` under each existing `[jar]`, `[liquid]` and `[gas]` table. Replace the old shared `[growth]` keys. Mode selection must determine the growth range actually used: retain one validated recipe-eligibility decision before payment, report the scheduler's authorized mode, and sample that mode's validated bounds without rerolling recipe eligibility after payment. Keep overflow, pause bypass, priority, shared cooldown and single-harvest limit intact. Retained growth is already paid work and does not change value when the selected mode changes.

- [x] Add behavioral RED for gas cost 250, three independent default/custom ranges and authorized-mode growth, including simultaneous inputs and switching. Preserve the failed-payment and post-payment-recipe-reroll regressions.
- [x] Implement per-mode settings/configuration, scheduler mode reporting and growth sampling. Validate each positive ordered pair, equal bounds and maximum values; retain one operation and shared storage across six faces.
- [x] Back up and migrate the dev config to per-mode keys and gas cost 250. Adapt isolated test configuration/fixtures and current docs; clearly label earlier shared-range evidence as historical. Verified all 15 target values and absence of legacy shared bounds.
- [x] Run tests/build, verify artifact evidence and obtain independent scoped review; launch the updated dev client once safely and record readiness for user testing.

## Task 14: Repository cleanup, documentation and pull request

The user explicitly authorizes two commits on the existing feature branch, pushing that branch and opening a GitHub PR for approval. First checkpoint all implemented work, then clean and verify the repository before the second commit. Do not merge the PR or change the mod ID, registered content, recipes, resource pins or gameplay behavior.

- [x] Run the current tests/build, verify staged contents exclude ignored local data, and commit the implemented mod as a checkpoint. Verified 81 GameTests, 29 unit tests and build; checkpoint `0bad75f` preserves 352 changed files. Ignored/local artifacts were excluded. Four existing whitespace findings are deferred to the cleanup commit.
- [x] Reread and apply `clean-code-principles`. Audit production code, test fixtures, diagnostics, build/runtime packaging and ignored/tracked files. Remove only demonstrably unused or redundant code/tests; retain independent coverage of scheduling, resource economics, native harvesting, overflow, persistence, mode-specific growth and six-face behavior. Keep required diagnostics and development compatibility hooks unless evidence establishes they are unnecessary.
- [x] Apply focused cleanup and ignore rules without deleting user worlds, local configs or useful development artifacts. Preserve reference submodules and generated runtime resources. Keep test-only machinery out of production packaging where the pinned loader supports a verified separation.
- [x] Rewrite README as **Ars Botany Pots**, with a minimal feature description, required dependencies, generic recipe and current defaults. Align the mod's display name; retain `sourcepots` registry/save identity.
- [x] Run appropriate tests, data/package checks and independent code/prose review. Record retained coverage and any test removals with reasons.
- [x] Update this plan with verified cleanup results, prepare the second commit and PR description, and leave the branch ready for delivery.

Delivery after the cleanup commit: push `feature/source-powered-pots`, use `gh` to open a PR against `main`, and return its link for user approval. GitHub is the delivery record; do not merge. Preserve the checkout for review changes.

## Plan review

Coverage: configuration and defaults (Task 1), timing/priority (Task 2), fertilizer parity (Task 3), Source access and buffers (Task 4), hopper behavior/persistence (Task 5), recipe/appearance variants (Task 6), real client/server and pack verification (Task 7), documentation/review (Task 8), Creative visibility and JEI (Task 9), faster inputs and wheat parity (Task 10), configurable growth and separate Creative tab (Task 11), and growth overflow, powered pause bypass, current rates and dev configuration (Task 12). Detailed manual gameplay, actual chunk unload and full-pack acceptance remain open.

Clean-code checks: one scheduling policy shared by every variant; one variant map for registration and data; dedicated growth/resource adapters; upstream hopper behavior reused; minimal entry-point wiring; no fake players, copied hopper implementation, or general-purpose fuel framework.

The user approved this plan and requested subagent execution. Task 14 explicitly authorizes the implementation checkpoint, a cleanup commit, pushing the feature branch and opening a PR for approval. It does not authorize merging the PR. Keep the checkout available for review changes.
