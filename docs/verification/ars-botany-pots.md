# Ars Botany Pots verification

Recorded on 2026-11-03 (Asia/Singapore) for ATM10Aero 0.8.0.

The automated checks cover 81 required GameTests and 29 unit tests. Development server startup and clean shutdown passed. An actual Minecraft client window opened and a completed launch exited cleanly. The user confirmed in-game appearance and operation with all three Source inputs. The corrected client loaded JEI and MezzConfig and initialized JEI's runtime. Detailed manual scenarios, user confirmation of item filtering, and full-pack checks remain pending. Development runs load the source set; they do not prove that the assembled jar works in the full pack.

## Environment and artifact

- Windows 11 amd64; OpenJDK 21.0.2+13-58.
- Gradle 9.2.1; ModDevGradle 2.0.148; Minecraft 1.21.1; NeoForge 21.1.250.
- Integration version: `sourcepots` 0.1.0-dev.
- Jar: `build/libs/sourcepots-0.1.0-dev.jar`, 257,683 bytes.
- Jar SHA-256: `273d8271e8d32a3312c7a1db1ffe4fa0d35f257a10e98e0209e3044ceedfbd79`.

The completed Task 7 server and first client launches listed these nine configured dependencies:

| Mod | Loaded version | CurseForge file ID |
| --- | --- | --- |
| BotanyPots | 21.1.44 | 8243851 |
| Starbunclemania | 1.5.9 | 8964554 |
| Ars Nouveau | 5.13.3 | 9052154 |
| Ars Elemental | 0.7.10.4 | 9057244 |
| Mekanism | 10.7.19 | 7904058 |
| Bookshelf | 21.1.81 | 7606240 |
| PrickleMC | 21.1.11 | 6961457 |
| Curios API | 9.5.1+1.21.1 | 6529130 |
| GeckoLib 4 | 4.9.3 | 8893490 |

NeoForge selected bundled Sauce Library 0.0.61.100. The list also included bundled Nuggets 1.1.0.48, Infiniverse 2.0.1.0, and LambDynamicLights API 4.5.1+1.21.1. No separate Sauce jar was installed.

Mekanism displays `10.7.19` in its mod metadata. The loaded file ID is the pinned `Mekanism-1.21.1-10.7.19.85.jar`: its SHA-256 is `004dbc9f3106f4d192aeaa1ee1190dd16ec9ca8059ed3d093b80034f4c574f43`, matching [the pack baseline](../../compatibility/atm10aero-0.8.0.json).

## Commands and automated evidence

Run commands from the project directory with Java 21:

```powershell
$env:JAVA_HOME = 'C:/Users/admin/scoop/apps/openjdk21/current'
$env:GRADLE_USER_HOME = 'C:/Users/admin/.gradle'
$env:Path = "$env:JAVA_HOME/bin;$env:Path"
& "$env:JAVA_HOME/bin/java.exe" -version
.\gradlew.bat test runGameTestServer build --console=plain
```

The historical Task 6 gate ran with `--no-daemon clean` and passed 26 JUnit tests and all 52 required GameTests before the launch checks (`BUILD SUCCESSFUL in 40s`, `task-6-final-verification.log`). Task 9 added three Creative-tab regressions; its gate passed 55 GameTests and 26 unit tests (`task-9-green.log`). The later client launcher correction passed a focused loader-discovery check and build (`task-9-discovery-green.log` and `task-9-discovery-proof.log`). Those focused checks did not launch Minecraft or repeat GameTests.

The final Task 8 command above passed after the mod-description change, without `clean`, using the isolated `run/gametest` directory: exit 0, `BUILD SUCCESSFUL in 38s`, all 55 required GameTests passed. The `test` task executed, and XML results show 26 unit tests, zero failures, errors, or skips. Evidence: `task-8-final.log`. Only documentation result fields were filled after this gate. Logs named in this document are under `.superpowers/sdd/2026-11-03-ars-botany-pots/`; JUnit results are under `build/test-results/test/`.

Two final Task 6 data runs produced the same SHA-256 manifest for all 310 distributable JSON files; the second run wrote zero files. Both jars were checked for those 310 resources and the absence of generated `.cache` entries. Task 9 also verified no JEI, MezzConfig, or nested jars were packaged. Data-generation and archive checks do not establish baked-model rendering.

Final Task 8 archive inspection confirmed 310 JSON files in each jar and no `.cache`, JEI, MezzConfig, or nested-jar entries. The sources jar is 158,971 bytes, SHA-256 `cefe1308c0661425d9ecf59a0fbce59dc6e4728fca7587322853aa73b124a0c8`. These hashes identify the built artifacts from this gate; the Task 6 and Task 9 hashes describe earlier artifacts.

## Task 10: input defaults and mature wheat

The final Task 10 `test runGameTestServer build --console=plain` gate exited 0 (`BUILD SUCCESSFUL in 40s`): 26 unit tests, zero failures/errors/skips, and all 59 required GameTests passed. Evidence: `task-10-final.log`. Archive inspection found 310 JSON resources in each jar and no `.cache`, JEI, MezzConfig, or nested jars. The Task 10 sources jar was 164,148 bytes, SHA-256 `72e11b70a5bf95fcf910d6a9c0c27069dfd3f21074b728ffefeef7e6d2711f3b`. Earlier Task 6/8/9 results and hashes above describe those earlier artifacts.

At the historical Task 10 checkpoint, defaults were 250 mB liquid every 5 ticks and 125 chemical units gas every tick. That task left Jar settings and payment amounts unchanged. Task 12 supersedes the Jar and gas values. An existing world's saved interval settings remain in effect until edited and the world/server restarted. Change `liquid.boostIntervalTicks` to `5` and `gas.boostIntervalTicks` to `1` to adopt the new rates.

The isolated automated server's generated `run/gametest/config/sourcepots-server.toml` contained the old 10/5 settings. Only that test configuration was updated to 5/1; its original bytes are retained as `task-10-gametest-config.original`. It remains at 5/1 so repeated verification uses the current rates. The user's client and saved-world configuration were not edited.

Two registered-block-entity tests compare one native supplier tank against six, using a temporary constant-ten-tick bonemeal recipe restored after each test. Both sets receive 15 natural ticks. Liquid gives three boosts at ticks 0, 5, and 10: growth 45, spending 750 mB with 250 mB retained, total intake 1,000 mB. Gas gives 15 boosts: growth 165, spending 1,875 units, zero retained. Every face returns the same liquid handler and the same gas handler. Growth, cooldown, storage, and payments match between one and six suppliers on every tick.

A separate synchronous batch compares 16 mature wheat harvests through the registered Ars and upstream hopper tickers, ordinary basic-pot manual interaction, and the actual loaded vanilla wheat loot table. Each uses dirt, no harvest tool, one yield roll, empty output slots, and a retained planted seed excluded from the counts. Both level randomness and the named `minecraft:blocks/wheat` loot sequence are reset before each matching harvest.

All paths give exactly one wheat per case. For seeds 0 through 15, the seed outputs are `3, 2, 4, 2, 3, 3, 3, 2, 2, 3, 3, 3, 4, 3, 2, 2`: 44 seeds and 16 wheat in each path. This deterministic sample is a parity check. The actual loaded age-seven table has one initial seed plus three binomial trials at probability 0.5714286, so its full range without Fortune is one to four seeds, with mean about 2.714. More seeds than wheat is expected. The test logs the loaded table; no loot balance was changed.

The matching basic manual result applies to this default context. Upstream basic harvesting calls `onHarvest` once; hopper harvesting uses yield rolls, which soil and tools can modify. No equality is claimed for all modifier combinations.

The historical Task 10 cap test spent one gas operation to reach exactly 20 ticks before maturity, produces no premature loot over the next 19 natural ticks, harvests once on tick 20, and produces no second harvest during the following five ticks. Upstream resets growth to -1 after harvest. Reload and removed-entity checks begin with the five-tick liquid cooldown so they still verify that switching to faster gas cannot skip a saved cooldown; one-tick gas may legitimately boost again on the next tick.

The automated no-catch-up check removes the placed block entity, waits 100 server ticks, and restores its saved state into a detached entity. It checks that buffers and local timers stay unchanged during that interval. No actual forced chunk unload/reload was performed; that Minecraft lifecycle check remains pending below.

## Final review corrections

The new `everyVariantPreservesOriginalMapColor` GameTest first failed on the black concrete variant before the constructor/registration correction. The RED run completed 60 GameTests with that single required failure (`final-fixes-red.log`, exit 1). The corrected registration takes each original mapped hopper's `defaultMapColor()` and preserves it for all 61 upgrades, without a fallback color or another variant list. Models and their orientation remain unchanged.

The final `test runGameTestServer build --console=plain` gate then passed: exit 0, `BUILD SUCCESSFUL in 39s`, all 60 required GameTests, and 26 unit tests with zero failures/errors/skips. Evidence: `final-fixes-green.log`. Source, configuration, and generated data were frozen after that gate; subsequent edits only record prose and results.

Both jars from that correction gate contain 310 valid JSON resources, each byte-identical to the generated files, with no `.cache`, JEI, MezzConfig, or nested jars. That historical mod jar was 237,438 bytes, SHA-256 `2da141b59f48d8add5a3df5d6ba3ec6232b0f604f2d123002c04fdde1f15dca6`. Its sources jar was 164,326 bytes, SHA-256 `06a053ae004a7fe0b28186b971b79bc58d34243881ee1ccd649ed2e18972451a`. Archive inspection and JUnit counts are recorded in `final-fixes-artifacts.json`; the change report is `final-fixes-report.md`. Earlier counts and hashes identify their named historical gates.

## Task 11: Configured growth and separate Creative category

The Task 11 instruction set `growth.minTicks = 600` and `growth.maxTicks = 1200`, inclusive, for Jar, liquid and gas. Equal bounds give constant growth; nonpositive or reversed bounds are rejected. Integer.MAX_VALUE is supported without overflow. Configured Source growth replaces the recipe amount. Supported BasicFertilizer matching and positive recipe growth still determine eligibility; manual bonemeal keeps its pinned 400..600 recipe amount. At that checkpoint the twenty natural finishing ticks and upstream harvest timing remained, alongside costs, intervals and shared buffers. Task 12 below supersedes the Source cap and pause.

The instance `config/sourcepots-server.toml` is used unless an existing world `serverconfig/sourcepots-server.toml` overrides it. Pinned NeoForge 21.1.250 ServerLifecycleHooks and loader 4.0.44 ConfigTracker confirm this path selection (`task-11-config-tracker.txt`). Missing growth keys receive 600/1200 defaults on the next load; existing resource settings remain. Changes require a world/server restart. Only the isolated GameTest configuration was adjusted from the initially generated 800 minimum to the revised 600 minimum; prior bytes are saved as `task-11-gametest-config-before600.toml`. No live configuration or user world was edited.

The registered category `sourcepots:ars_botany_pots` has the translated title **Ars Botany Pots** and an Ars terracotta pot icon. Native category content generation verifies all 61 mapped items in parent and search contents, without original Botany Pots items. The integration no longer appends items to the original category. This supersedes historical Task 9 behavior. Datagen wrote one language JSON and retained 310 distributable JSON files.

RED evidence: `task-11-red-growth.log` (initial amount mismatch), `task-11-red-600.log` (revised default mismatch), and `task-11-red-creative.log` (missing category and additions to the old category). Registered block tickers test exact custom growth, current defaults and maximum growth with the finishing cap for all three payment paths. Other new tests cover inclusive endpoints, the full integer width and config restoration after assertion failure. Real reloads change recipe growth 500→100→0→600 with incompatible soil→compatible 600: configured Source growth stays 17 while eligible, stops for nonpositive/nonmatching recipes and resumes at 17. Existing failed-restoration and partial-setup cleanup regressions remain.

Exact one-face/six-face cadence checks use scoped configured growth 10. Existing save/load, unload, cooldown and break/place fixtures that need buffers after an initial boost use scoped growth 100 to avoid randomly reaching the cap during setup. Their resource, timing and preservation assertions remain; repayment now verifies an exact 105 growth delta over five ticks. Temporary settings are restored in finally, including asynchronous reload cleanup.

The final Java 21 `test runGameTestServer build --console=plain` gate exited 0: `BUILD SUCCESSFUL in 49s`, all **68 required GameTests**, and **28 unit tests** with zero failures/errors/skips (`task-11-final.log`). Both jars contain 310 valid JSON resources, byte-identical to generated files, and no `.cache`, JEI, MezzConfig or nested jars. The Task 11 mod jar was 244,873 bytes, SHA-256 `74ba8a4954c20976120467b86266b04481732d3c3f411f526c1cf10cc30f60db`; the sources jar is 167,101 bytes, SHA-256 `ce654d5331e0436b3c935c09416a93f1f50cfc6cb52d4036a26f94f8bf3554fd`. Details: `task-11-artifacts.json` and `task-11-report.md`. Source, data and test configuration were frozen after this gate. Existing dependency diagnostics remain. JEI initialization is recorded below; visual category inspection and full-pack acceptance remain pending.

## Task 12: Paid growth overflow and historical input defaults

Source growth reaches maturity without the twenty-tick finishing cap. The native ticker still runs once, before Source growth, so a newly matured crop harvests on the next tick. Excess paid growth carries into subsequent cycles. A sufficient bank blocks new intake and payments; a partial bank is consumed first and can be followed by one due boost. Only Source-assisted harvests clear the native five-tick growth pause. Native loot/yield, tool wear, inventory export and comparator hooks remain in use. Unpowered and manual-only cycles retain the original pause.

The exact-credit fixture pays 1000 ticks with 300 needed and saves 700. On the following harvest, native growth resets to -1, so spending those 700 ticks places the next crop at 699. This preserves the upstream restart sentinel without inventing a free tick. Integer.MAX_VALUE boosts preserve exact remaining credit and produce no more than one harvest per tick; one-face/six-face tests with fixed 600 growth mature wheat at 1200, carry two ticks and harvest every second tick after initial maturity.

Saved credit is finite, nonnegative and bounded by one maximum integer boost. Recipe invalidity holds it without applying it or accepting Source; crop/soil resets and break/place discard it. Tests cover fractional and malformed saves, retained capability identity and push rejection before the first loaded tick, blocked crop harvest gates, and a delayed detached-entity reload with paid credit. The unload evidence uses removal/detachment rather than a forced chunk unload. No elapsed unloaded time is awarded. Eligibility and the configured boost are prepared before payment; a varying recipe regression prevents post-payment rerolls from consuming Source without growth.

The historical Task 12 defaults were Jar cost 2500 (25% of a standard 10000-capacity Jar), Jar boost interval 10, liquid cost 250 mB/interval 5, gas cost 100 chemical units/interval 1, and shared growth 600–1200. Jar payment interval 80 and paid window 100 are unchanged. Mekanism stores gas as chemical units. With a new boost needed every interval, liquid uses 1000 mB and gas uses 2000 chemical units per 20 ticks. Exact budgets are checked for both one and six suppliers; the registered Jar fixture checks 2500 Source payments at ticks 0 and 80 and boosts every ten ticks.

The development instance configuration was backed up and updated separately by the root agent under the user's authorization. The isolated GameTest configuration was also backed up before adopting these values. No user world was edited. Existing explicit settings in other worlds remain in effect until edited and restarted.

RED evidence is in `task-12-red.log` (seven missing overflow behaviors), `task-12-red-extended.log` (ten missing behaviors), `task-12-red-recipe.log` (post-payment recipe reroll), and `task-12-red-rates.log` (seven default/cadence failures). The overflow checkpoint passed 78 GameTests and 28 unit tests before the latest rate changes (`task-12-overflow-final.log`). Older cap, cost and cadence figures elsewhere identify their historical gates.


Final Java 21 `test runGameTestServer build --console=plain` exited 0: `BUILD SUCCESSFUL in 48s`, all **79 required GameTests** and **28 unit tests**, zero failures/errors/skips (`task-12-final.log`). Both jars contain 310 valid JSON resources, byte-identical to generated files, with no `.cache`, JEI, MezzConfig or nested jars. The sources jar is 170,991 bytes, SHA-256 `34ca1407bc2ac47821895b4e76530406a23c42e64a76388b85f7ed970927d517`. Artifact details and implementation evidence are in `task-12-artifacts.json` and `task-12-report.md`. Source/data/test configuration were frozen after this gate; remaining edits record factual documentation and review results. Existing dependency diagnostics remain. No new client/UI/full-pack result is claimed by this gate.

## Historical Task 13: Shared growth defaults

At the initial Task 13 checkpoint, defaults were liquid 500 mB every 5 ticks, gas 200 native chemical units every tick, and shared inclusive growth 500–1100 ticks. The per-mode amendment below supersedes those growth bounds and the gas cost. Jar cost 2500, payment interval 80, paid duration 100, boost interval 10 and radius 3 remain unchanged. The configuration still derives defaults from `SourceSettings.defaults()` and preserves existing explicit settings.

At that historical checkpoint, the 20-tick one-face/six-face checks required exactly four liquid payments (2000 mB, two buckets) plus one buffered payment (500 mB), or twenty gas payments (4000 units, four 1000-unit buckets) with no gas retained. Every tick still checks equal growth, one shared cooldown and bounded independent storage. The fixed-600 growth overflow fixture remains an explicit custom setting; all registered payment modes separately check the new default 500–1100 range. Overflow carry, finishing-cap removal, powered pause bypass, native harvest behavior and the separate Creative category are unchanged.

`task-13-red.log` and `task-13-red-settings.xml` record the failing defaults test before production changed. The first full gate caught two accidentally edited custom Jar provider amounts; both were restored to their original 900-unit fixtures. Only the four requested values changed in production. The isolated GameTest configuration was backed up as `task-13-gametest-config-before.toml` before applying the four defaults. The root agent handles the separately authorized development config and client launch.

The Java 21 `test runGameTestServer build --console=plain` gate exited 0 in 46 seconds: all 79 required GameTests and 28 unit tests passed, with zero unit failures, errors or skips (`task-13-final.log`). Both jars contain 310 valid JSON resources, byte-identical to generated resources, with no `.cache`, JEI, MezzConfig or nested jars. Runtime jar: 254295 bytes, SHA-256 `c07d726706232b8961568b3fe4584cc4ace492ecd4fb57efaf5191cb519e9ec5`. Sources jar: 170985 bytes, SHA-256 `0dcec3572616aa710ff76e3951a45b1a513f86c62ec0f94ff877a67ca463cc15`. See `task-13-artifacts.json`, `task-13-source-freeze.json` and `task-13-report.md`. Source, generated data and isolated test configuration were frozen after the gate. Existing dependency startup diagnostics remain; this gate adds no client UI or full-pack acceptance claim.

## Task 13 amendment: Growth per input mode

Current inclusive growth bounds are Jar 600–800, liquid 500–700 and gas 400–600. Gas costs 250 native chemical units every tick; liquid remains 500 mB every five ticks. Jar remains 2500 Source/80-tick payment/100-tick window/10-tick boost/radius 3. Each mode’s existing table has `growthMinTicks` and `growthMaxTicks`; the old shared `[growth]` keys are replaced and no longer control boosts. Existing resource settings remain explicit overrides until changed.

The scheduler reports the actual authorized mode. The ticker validates all ranges and recipe eligibility before payment, applies retained credit first, then samples the paid mode’s range without reevaluating the recipe. Jar renewal during a shared cooldown cannot authorize growth early. Already paid credit keeps its original amount when modes switch.

The fixed-growth one-face/six-face checks now require twenty gas payments totaling 5000 chemical units (five 1000-unit buckets), with no retained gas, or four liquid payments totaling 2000 mB plus 500 mB buffered. The fixed-600 overflow fixture remains an explicit custom range. Registered paths test distinct fixed ranges (37 Jar, 47 liquid, 57 gas), current defaults and maximum credit. Mixed-input tests cover gas → liquid → paid Jar switching without shortening cooldowns; an invalid range in any mode prevents charging funded gas. Recipe-reroll, failed-payment, overflow and native harvest regressions remain.

RED evidence is in `task-13a-red-defaults.log` and `task-13a-red-growth.log`: the defaults test failed and the registered gas path failed to use its independent range. The isolated test config was backed up to `task-13a-gametest-config-before.toml` before migration. Development config and client-launch evidence are recorded by the root agent.

The Java 21 `test runGameTestServer build --console=plain` gate exited 0 in 49 seconds with all 81 required GameTests and 29 unit tests passing, zero unit failures/errors/skips (`task-13a-second-gate.log`). Runtime jar: 257683 bytes, SHA-256 `273d8271e8d32a3312c7a1db1ffe4fa0d35f257a10e98e0209e3044ceedfbd79`. Sources jar: 172095 bytes, SHA-256 `81a9b425d174105720ed6927faa6bfe6df854110fd2991dbefabc331f5252538`. Each jar contains 310 valid JSON resources, byte-identical to generated data, with no `.cache`, JEI, MezzConfig or nested jars. `task-13a-artifacts.json` and `task-13a-source-freeze.json` record the checks and frozen hashes. Existing dependency diagnostics remain; this gate adds no client UI or full-pack acceptance claim.

## Dedicated server launch

The existing `runServer` task now forwards `System.in`, so its console can receive `stop`.

Before launch, the original `run/server.properties` bytes were backed up. Temporary values were `server-ip=127.0.0.1`, `server-port=0`, and `level-name=sourcepots-verification-20261103`. Query and RCON stayed disabled. This was a new verification world; the GameTest world `run/world` was not reused.

```powershell
.\gradlew.bat --no-daemon --console=plain runServer
```

- At 02:37:00, the server reported `Done (5.207s)!`.
- PID 26316 listened on `127.0.0.1:57561`; the requested ephemeral bind was logged as `127.0.0.1:0`.
- At 02:37:10, console `stop` was accepted. The server saved players, worlds, all chunks, and all dimensions.
- Gradle reported `BUILD SUCCESSFUL in 41s`, exit code 0. The game process ended.
- The original properties were restored after shutdown. SHA-256 before and after: `5bbf2a286002dd2f8c93c5eec75806ebf8f4faa9ce35d89cadb410a96b40f7be`.

The restored original properties have an empty `server-ip` and `online-mode=false`. Any later development server launch must first set a loopback bind again. No EULA or authentication file was edited.

Evidence under `.superpowers/sdd/2026-11-03-ars-botany-pots/`: `task-7-server-latest.log`, `task-7-server-debug.log`, `task-7-server-listener.txt`, `task-7-server.properties.original`, and `task-7-server.properties.used`.

## Actual client launch

```powershell
.\gradlew.bat --no-daemon --console=plain runClient
```

The first launch began at 02:37:35. Windows reported PID 24196, nonzero window handle 919056, title `Minecraft NeoForge* 1.21.1`, and `Responding=True`. Resource reload listed `mod/sourcepots`; audio and texture-atlas initialization completed at 02:37:53. The client logged `Stopping!` at 02:38:09, and Gradle exited 0 with `BUILD SUCCESSFUL in 44s`. No agent close command was sent; the reason for closure was not established.

A second Task 7 launch began at 02:38:51 while a stop-relaunch instruction was in transit. Windows reported game PID 24704 and window handle 35849704 with the same Minecraft title. See the task report for that task's final process observation.

At Task 7's final process snapshot, PID 24704 remained responsive and its title was `Minecraft NeoForge* 1.21.1 - Singleplayer`. Logs contained integrated-world activity. No agent GUI commands were sent, and no specific gameplay result follows from this observation. That client was left open for the user; it was absent at Task 9's later read-only process check. Snapshot evidence: `task-7-client-second-process.txt`, `task-7-client-second-latest.log`, and `task-7-client-second-debug.log` in the same evidence directory.

The available tools could inspect logs and Windows process metadata, but could not view or control the desktop. Window existence does not establish menu appearance, pot rendering, or gameplay. No agent screenshot or direct visual observation is claimed; the user's later feedback is recorded below. Clean shutdown was verified for the first launch; the second launch's shutdown was not captured.

First-launch logs were archived as `task-7-client-latest.log` and `task-7-client-debug.log` before the second launch rotated them.

## Creative contents and development JEI

Task 9 originally added all 61 mapped items to `botanypots:tab` through a Creative-content listener. Task 11 supersedes that shared-category behavior: a native registered **Ars Botany Pots** tab now owns all 61 parent/search entries, and the old listener is removed. Tests build the actual registered tab's contents and check that the original and unrelated tabs receive no Ars additions. User confirmation of the Creative screen remains pending.

The development client resolves JEI 19.64.0.455, CurseForge file 9205121, with bundled MezzConfig 0.6.6. This exact artifact is from ATM10Aero 0.8.0. It is optional development tooling; there is no shipped JEI API/plugin, bundled jar, or required JEI dependency. Server, GameTest, data, main-runtime, compile, and test classpaths were checked to exclude it. The pack already has JEI; avoid installing a duplicate.

The controller's updated-client launch at 02:52:22, reported PID 19312, reached Singleplayer but omitted JEI from its discovered mods and resource list. The run's legacy-classpath file contained JEI, but its effective JVM classpath did not. That was a failed runtime acceptance check, despite successful dependency resolution.

The first correction added the pinned artifact to the client JVM classpath. A focused Java probe found its metadata, hash, version and bundled library (`task-9-discovery-proof.log`). However, the actual 03:36 client launch still omitted JEI and MezzConfig from its mod list (`task-9-client-final.log`). The probe did not exercise bootstrap module loading and was insufficient to prove discovery during a Minecraft launch.

The launcher investigation found that the additional legacy classpath caused JEI to load in the bootstrap module layer. FML then considered that path already located and skipped it during mod discovery. The final correction declares JEI directly on a separate client-only JVM configuration and leaves it off the legacy bootstrap classpath. `prepareClientRun build` and launch-input assertions pass: JEI is present on the client JVM classpath, absent from the generated legacy list and other run/test/product classpaths, and absent from the shipped jar (`jei-actual-launch-fix.log`). No active world was interrupted.

The fresh launch at 03:52 lists JEI 19.64.0.455 and MezzConfig 0.6.6 in FML's discovered mods (`task-11-client.log`). Windows reported PID 25612, title `Minecraft NeoForge* 1.21.1 - Singleplayer`, responding. JEI logged `Starting JEI...` at 03:52:48, `Starting JEI GUI` at 03:52:50, and successful completion in 2.515 seconds at 03:52:51. These logs verify actual discovery and runtime initialization. No direct GUI inspection or user filtering result is claimed. At that Task 11 launch, the development config gained `growth.minTicks=600` and `growth.maxTicks=1200`; its existing liquid/gas intervals stayed 10/5. No saved interval was overwritten at that checkpoint. The separately authorized Task 12 config update is recorded above.

Manual verification of JEI search/filtering by the display name `Ars Botany Pots` remains pending. The mod ID remains `sourcepots`. Historical launch records below retain the names used at those checkpoints.

## Log findings

No `sourcepots` missing-model, missing-texture, resource-loading, or renderer error was found in the inspected startup logs. There were dependency errors and warnings:

- Both sides: eight missing augment descriptions for `ars_elemental:glyph_carian_phalanx`, reported by Ars Nouveau.
- Dedicated server: NeoForge reported missing spawn-placement registration for `minecraft:breeze`.
- Client: GeckoLib could not parse `adjust_size` expression `aoe` or `swim_charge` expression containing `swimcharge`.
- Client: Ars Nouveau `particle_block` and `planarium_projector` model warnings; a missing `starbunclemania:alak_hat` texture referencing `ars_nouveau:entity/alakarkinos`.
- Development refmap warnings, animation filename warnings, vanilla sound/shader warnings, Ars shader warnings, and a Windows performance-counter registry warning.

These messages name dependency resources and were nonfatal in the first launch. No run without this integration was performed to establish whether the same messages occur in that baseline. They were retained in the archived logs; no dependency pins were changed.

Compilation also reports two pinned-FML deprecation warnings for `SourcePotsClient`'s explicit `EventBusSubscriber.bus` and `EventBusSubscriber.Bus.MOD`. The supported registration is retained for NeoForge 21.1.250. A replacement must be verified against that loader before changing the annotation or client event wiring.

## Gameplay checklist: pending user verification

The user confirmed that the Ars pots look okay in game and work with Source Jars, Liquid Source, and Gaseous Source. No specific variant, exact cadence, payment amount, priority, save/reopen result, or full-pack result was established by that feedback. The detailed checks below remain open.

Use a new local creative world. Preserve existing worlds. Record the tested variant, setup, observed result, log time, and any screenshot for each check.

- [ ] Craft terracotta, one dyed, and one glazed Ars Hopper Botany Pot. Use matching upstream hopper pot `C`, two gold nuggets `A`, and five archwood planks `B`, arranged `A A / BCB / BBB`. Confirm one matching `sourcepots` pot results.
- [ ] Inspect the gold rim in inventory and on placed pots in every facing direction. Record the user's appearance judgment.
- [ ] Confirm all 61 Ars pots appear in the separate Ars Botany Pots Creative category and Creative search; confirm the original Botany Pots category keeps its upstream entries. Startup versions and JEI initialization are verified above. Test JEI search/filtering by the display name `Ars Botany Pots` plus recipe lookup.
- [ ] Insert valid soil, wheat seeds, and a supported tree sapling. Check soil/crop/tree rendering, menu, harvest tool, all 15 inventory slots (soil, seed, tool, and 12 output/storage slots), hopper export, comparator, and waterlogging.
- [ ] Compare empty-input growth and harvesting against an ordinary Hopper Botany Pot.
- [ ] Test Jar-only input with a real Ars Source Jar: default 2,500 Source per payment, payment attempts have an 80-tick cooldown, 100-tick paid window, boosts every 10 ticks, radius 3 including diagonal positions; growth 600–800 ticks per boost. Confirm visible effects and behavior just beyond range.
- [ ] Test liquid-only input with a real container and pipe using `sauce:source_fluid`: 500 mB per boost, 5-tick cadence, growth 500–700 ticks per boost, input-only buffer limited to one configured operation.
- [ ] Test gas-only input with Mekanism storage and pipes using `starbunclemania:source_gas`: 250 units per boost, 1-tick cadence, growth 400–600 ticks per boost, separate input-only buffer limited to one configured operation.
- [ ] Test both buffers and a Jar together. Confirm default gas/liquid/Jar priority, one boost at a time, six-face liquid/gas connections, and no diagonal pipe connection.
- [ ] Remove/change crops, use invalid soil, block harvesting, and reach maturity or fill a retained growth bank. Check that no supplier drains while growth is ineligible. Use automated tick-counter tests for exact timing; visual observation alone cannot measure tick cadence reliably.
- [ ] Save and quit with both buffers populated and paid Jar time remaining. Reopen the world; check contents, no duplication/free boost, and continued unpowered hopper behavior.
- [ ] Force the pot's actual chunk to unload while buffers and paid Jar time remain, then reload it. Record evidence of the chunk lifecycle and check that elapsed unloaded time causes no catch-up growth, resource spending, or timer advance.
- [ ] Exit the current client normally and retain its final logs/result.

Automated tests cover scheduling, resource buffers, persistence, capabilities, growth limits, variant resources, and several inherited pot behaviors. These checks support the expected results above; they do not complete the visual checklist.

## Full-pack checklist: not performed

No separate backed-up ATM10Aero 0.8.0 instance path or permission to alter a pack world was supplied. No external instance was searched, modified, or used.

- [ ] In the user's separate backed-up ATM10Aero 0.8.0 client instance, install only `sourcepots-0.1.0-dev.jar` and repeat the gameplay checklist.
- [ ] Repeat with the matching backed-up dedicated-server pack, including a non-operator player. Keep normal pack authentication and manual EULA handling.
- [ ] Record startup logs, resource errors, save/reopen results, player permissions, and any compatibility failures.

Full-pack compatibility remains unverified.


## Task 14: cleanup and development test isolation

The cleanup gate ran with Java 21: `clean test runGameTestServer build prepareClientRun prepareServerRun prepareDataRun`, plus a read-only Gradle classpath audit. It exited 0 in 56 seconds. All 81 required GameTests passed; JUnit XML records 29 tests with no failures, errors, or skips. Evidence is retained locally in `scratch/task-14-cleanup-final-gate.log` and `scratch/task-14-cleanup-artifact-report.txt`.

All GameTests and fixtures now compile in `src/gametest`, with a separate development test mod. Client, server, and data runs load only `sourcepots` and have no GameTest source-set runtime paths. The GameTest run loads `sourcepots` and `sourcepots_gametest`; its compile classpath includes main output, the pinned NeoForge artifact, and production dependencies. CI runs the unit suite, native GameTests, and build.

All 81 GameTest cases and 29 unit cases remain. Removed only the unused top-texture metadata assertion, two production convenience methods used exclusively by tests (their orchestration now lives in test support), and temporary Task 10 success logs/loot serialization. Actual generated resource and orientation checks remain. Scheduler, ticker, resource adapters, settings/configuration, and the guarded datagen compatibility hook are unchanged from checkpoint `0bad75f`.

`runData` passed before the clean gate, including the pinned StarbuncleMania compatibility hook. Every one of the 310 generated JSON files was byte-identical to the checkpoint afterward. Both final jars contain all 310 JSON files and no GameTest classes, fixtures, test-mod metadata, cache files, or development tools.

| Artifact | Bytes | SHA-256 |
| --- | --- | --- |
| `sourcepots-0.1.0-dev.jar` | 167,345 | `3287061324d59959f207c6434f8387b1cb4b3341c8fc74e0fbfeac19a0003af8` |
| `sourcepots-0.1.0-dev-sources.jar` | 136,266 | `55c89898e482f02d3b77ca9eab30c2ce66edc55fa67f370b1ea30fdcd1bb7b9e` |

The display name is now **Ars Botany Pots**. Registry/save identity remains `sourcepots`; the dependency file pins and upstream references are unchanged. No client was launched for this cleanup. Existing manual gameplay and full-pack checks remain open.
