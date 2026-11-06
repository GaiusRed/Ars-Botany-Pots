# Optional Source integrations

Verified on 2026-11-03 with Java 21, Minecraft 1.21.1 and NeoForge 21.1.250. Dependency versions remain pinned to the [compatibility baseline](../compatibility.md).

## Required and optional resources

Ars Nouveau and Botany Pots are the required gameplay mods. Their required libraries are Bookshelf, Prickle, Curios and GeckoLib; Ars Nouveau also loads its bundled libraries. Jar power works without Sauce, Mekanism, StarbuncleMania or Ars Elemental.

Liquid input is available only when `sauce:source_fluid` exists. Gas input requires Mekanism and `starbunclemania:source_gas`. Mekanism alone provides no Source gas. The addon registers no replacement fluids or chemicals.

Mekanism storage and capability registration are isolated in compatibility classes loaded after the mod-presence check. Core storage fields and method signatures use no Mekanism types. The StarbuncleMania datagen workaround has a separate guarded compatibility class.

## Saved buffers

Unavailable liquid and gas retain their numeric saved amounts, clamped to configured capacity. They cannot accept, pull or spend resources, and expose no corresponding block capability. A later load with the integration available restores the retained amount into native storage. Existing native capability handlers retain their identity when live NBT is loaded.

All 61 variants, recipes, config keys and defaults remain unchanged. The full native suite covers growth, harvests, overflow, the harvest-pause bypass, scheduling and shared six-face input behavior.

## Automated checks

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

## Development profiles and remaining checks

`runClient`, `runServer` and `runData` use the required gameplay mods and libraries. `runFullClient`, `runFullServer` and `runFullData` use the pinned full integration runtime. JEI is a development tool on both client profiles only. Mekanism and StarbuncleMania are compile-only dependencies of production code. Optional mods are not bundled into the distributable.

The final clean command passed: `clean test runCoreGameTestServer runFluidGameTestServer runMekanismGameTestServer runGameTestServer runData runFullData build --console=plain` (`matrix-final.log`, 2 minutes 25 seconds). All 34 unit tests and 96 native tests passed: 5 core, 5 liquid-only, 5 Mekanism-only and the full 81-test integration suite. The frozen source manifest was unchanged throughout verification.

Minimal and full data runs contain the same 310 JSON paths and values. Their bytes match after newline normalization: the existing checkout used CRLF, while fresh output used LF. Both distributable jars contain 310 JSON resources and exclude smoke/GameTest code, development test metadata and nested dependencies. The mod jar SHA-256 is `d382a0bb164c3d2fcbac3ab09977b3345b7633fca937411072d85c466a386ba5`.

All ten run classpaths were audited (`runtime-audit.log` and `matrix-runtime-classpaths.json`): default client/server/data and core smoke contain no optional integration jars; the liquid profile adds only Sauce; the Mekanism profile adds only Mekanism; full profiles include the pinned optional stack. JEI is limited to the two development client profiles. Evidence is retained under `.superpowers/sdd/2026-11-03-optional-source-integrations/`.

No client was launched for this change. These checks do not establish compatibility with the complete ATM10Aero pack, its scripts or multiplayer configuration.
