# Optional Source integrations

Dependency versions remain pinned to the [compatibility baseline](../compatibility.md): Java 21, Minecraft 1.21.1 and NeoForge 21.1.250. Current results are recorded below; the 2026-11-03 results remain under their historical heading.

## Required and optional resources

Ars Nouveau and Botany Pots are the required gameplay mods. Their required libraries are Bookshelf, Prickle, Curios and GeckoLib; Ars Nouveau also loads its bundled libraries. Jar power works without Sauce, Mekanism, StarbuncleMania or Ars Elemental.

Liquid input is available only when `sauce:source_fluid` exists. Gas input requires Mekanism and `starbunclemania:source_gas`. Mekanism alone provides no Source gas. The addon registers no replacement fluids or chemicals.

Mekanism storage and capability registration are isolated in compatibility classes loaded after the mod-presence check. Core storage fields and method signatures use no Mekanism types. The StarbuncleMania datagen workaround has a separate guarded compatibility class.

## Saved buffers

Unavailable liquid and gas retain numeric amounts independently for north, east, south and west, clamped to configured per-face capacity. They cannot accept, pull or spend resources, and expose no corresponding block capability. A later load with the integration available restores the retained amounts into native storage. Existing native capability handlers retain their identity when live NBT is loaded; the current regression changes live amounts before restoring and checks the cached views.

All 61 variants and crafting recipes remain unchanged. The per-face revision replaces shared Source config keys and saved state; see [operation and defaults](../../README.md#defaults). Horizontal faces have separate input-only handlers. Vertical and unsided Source capabilities are absent. Bottom item extraction remains inherited. Buffers may refill while a higher-priority resource is selected, but suppressed resources cannot pay or boost.

## Current automated checks (2026-11-07)

After the final precision fix, `./gradlew.bat test runCoreGameTestServer runFluidGameTestServer runMekanismGameTestServer runGameTestServer build --console=plain` passed in 1m 45s with process-local JDK 21. Each smoke profile passed all 5 required tests; the full profile passed all 104, for 119 native tests total. All 34 unit tests freshly passed against the final production code with zero failures/errors/skips; the matrix reused those results. These include `OptionalApiLoadingTest`, which rejects optional API packages while linking core storage. Smoke source has no Mekanism imports and uses real sided fluid block capabilities; its mod/classloader assertions check which jars are actually absent.

Coverage includes fixed mixed-resource priority, partial higher-priority buffers, Jar suppression/expiry/fallback, all funded face counts, exact batch payments, capacity reductions, directional dormant quantities and live cached views after changed quantities are restored. Tests also exercise bounded harvests and active recipe growth/soil/tool/callback behavior. See [gameplay verification](ars-botany-pots.md#current-revision-per-face-source-and-bounded-harvests-2026-11-07) for operating rules, artifacts and limits.

Evidence: `.superpowers/sdd/2026-11-07-per-side-source/final-fix-matrix.log`, `final-fix-artifacts.json` and `final-fix-report.md`; fresh unit execution is in `final-fix-green.log`. Both jars contain 310 JSON resources, with no test code/metadata, nested jars or bundled optional mods. Guarded compatibility code belongs to this mod and remains packaged. Build configuration, dependency pins and data inputs are unchanged; datagen was not repeated.

Before the server-side arithmetic fix, `runFullClient` opened a responsive client and initialized resources/audio, then exited 0 after the agent's scoped graceful close (`task-3-client.log`). Client startup was not repeated for that fix. No world was opened. Actual pipe connections, rendering, two/four-face gameplay, fallback, harvest tools, bottom export and save/reopen remain unperformed for this revision because desktop interaction was unavailable. No CurseForge instance was supplied or modified. Historical user feedback below does not verify the replacement model.

Passing logs still contain development refmap/Windows counter warnings, Ars Elemental glyph-description errors, the server breeze spawn-placement diagnostic and intentional invalid-recipe/capacity fixtures. The client also retains GeckoLib animation errors and dependency model/texture/sound/shader warnings. They are disclosed in the gameplay verification log findings. No unrelated dependency change was made to remove this noise.

## Historical automated checks (2026-11-03)

The first regression run failed with `NoClassDefFoundError: mekanism/api/chemical/IChemicalHandler` while linking `PotSourceAccess` through a classloader that rejects optional APIs. The same test passes after isolating those types. Its failing XML is retained at `.superpowers/sdd/2026-11-03-optional-source-integrations/optional-api-red.xml`.

`./gradlew compileSmokeJava compileGametestJava test` passed all 34 unit tests: the previous 29, one classloading regression and four unavailable-storage tests.

The native smoke tests use a separate test source set with no Mekanism imports. Each runtime actually omits the excluded jars; mod-list and classloader assertions check that absence. The fluid-only run extracts the pinned Sauce 0.0.61.100 jar from Ars Elemental without loading Ars Elemental itself. Its SHA-256 is `58ce7a36fc6fb7631b35765d58227b2982ece031559fa4122f1391a17811b251`.

| Command | Runtime beyond required mods and libraries | Result |
| --- | --- | --- |
| `./gradlew runCoreGameTestServer` | None | 5 required tests passed |
| `./gradlew runFluidGameTestServer` | Sauce only | 5 required tests passed |
| `./gradlew runMekanismGameTestServer` | Mekanism only, without Source gas | 5 required tests passed |
| `./gradlew runGameTestServer` | Full pinned integrations | 81 required tests passed |

The smoke checks cover registered pots, actual Jar-funded wheat harvests, unpowered ticking, buffer save/load and optional capabilities and payments. The fluid profile also checks a native liquid-funded growth tick. Full integration checks retain the existing gameplay assertions.

Initial runs are recorded in `matrix-compile.log`, `matrix-core.log` and `matrix-optional-full.log` under `.superpowers/sdd/2026-11-03-optional-source-integrations/`.

## Development profiles and historical remaining checks

`runClient`, `runServer` and `runData` use the required gameplay mods and libraries. `runFullClient`, `runFullServer` and `runFullData` use the pinned full integration runtime. JEI is a development tool on both client profiles only. Mekanism and StarbuncleMania are compile-only dependencies of production code. Optional mods are not bundled into the distributable.

The final clean command passed: `clean test runCoreGameTestServer runFluidGameTestServer runMekanismGameTestServer runGameTestServer runData runFullData build --console=plain` (`matrix-final.log`, 2 minutes 25 seconds). All 34 unit tests and 96 native tests passed: 5 core, 5 liquid-only, 5 Mekanism-only and the full 81-test integration suite. The frozen source manifest was unchanged throughout verification.

Minimal and full data runs contain the same 310 JSON paths and values. Their bytes match after newline normalization: the existing checkout used CRLF, while fresh output used LF. Both distributable jars contain 310 JSON resources and exclude smoke/GameTest code, development test metadata and nested dependencies. The mod jar SHA-256 is `d382a0bb164c3d2fcbac3ab09977b3345b7633fca937411072d85c466a386ba5`.

All ten run classpaths were audited (`runtime-audit.log` and `matrix-runtime-classpaths.json`): default client/server/data and core smoke contain no optional integration jars; the liquid profile adds only Sauce; the Mekanism profile adds only Mekanism; full profiles include the pinned optional stack. JEI is limited to the two development client profiles. Evidence is retained under `.superpowers/sdd/2026-11-03-optional-source-integrations/`.

No client was launched for this change. These checks do not establish compatibility with the complete ATM10Aero pack, its scripts or multiplayer configuration.
