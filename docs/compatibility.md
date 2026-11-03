# Compatibility baseline

Ars Nouveau and Botany Pots are now the only required gameplay mods. The versions below remain the full integration test baseline; Sauce, Mekanism, StarbuncleMania and Ars Elemental are optional. See [optional integration verification](verification/optional-source-integrations.md) for the required-only runtime and resource availability checks.

## Released pack

Checked on 2026-11-02:

- Pack: **ATM10AERO-0.8.0.zip**, published 2026-10-29.
- CurseForge project: **1644918**; release file: **9366703**.
- [Release page](https://www.curseforge.com/minecraft/modpacks/all-the-mods-10-aeronautics/files/9366703).
- [Public release metadata](https://api.cfwidget.com/1644918).
- [Official pack repository at 0.8.0](https://github.com/AllTheMods/ATM10-Aeronautics/tree/15a32d1fdb9a68bf4060e62136e2c695a6089da6).
- The downloaded release archive's `manifest.json` specifies Minecraft **1.21.1** and **neoforge-21.1.250**.

The direct CurseForge website was blocked in the bootstrap environment. Release discovery used CFWidget, corroborated by the official repository's `0.8.0` commit. Version pins were then read from the actual release archive downloaded from ForgeCDN. The JSON baseline records archive and artifact SHA-256 hashes.

## Published runtime dependencies

All nine jars below are selected by exact file ID from the released pack manifest. Gradle resolves them through CurseMaven. Required transitive mod dependencies are explicit because CurseMaven does not supply a complete transitive dependency graph.

| Mod | Version | Project ID | File ID |
| --- | --- | --- | --- |
| Botany Pots | 21.1.44 | 353928 | 8243851 |
| StarbuncleMania | 1.5.9 | 746215 | 8964554 |
| Ars Nouveau | 5.13.3 | 401955 | 9052154 |
| Ars Elemental | 0.7.10.4 | 561470 | 9057244 |
| Mekanism | 10.7.19.85 | 268560 | 7904058 |
| Bookshelf | 21.1.81 | 228525 | 7606240 |
| Prickle | 21.1.11 | 1023259 | 6961457 |
| Curios | 9.5.1+1.21.1 | 309927 | 6529130 |
| GeckoLib | 4.9.3 | 388172 | 8893490 |

## Source submodule coverage

The scope is the integrations' required mod dependency graph, bundled mod libraries, and Ars Elemental/Mekanism for the pack's Source integrations. It does not include every optional integration, every Java build library, Minecraft/NeoForge sources, or all 433 pack mods.

| Source submodule | Reason |
| --- | --- |
| BotanyPots | Main pot integration |
| StarbuncleMania | Main Source/chemical integration |
| Bookshelf | Required by Botany Pots |
| PrickleMC | Required by Botany Pots |
| Ars-Nouveau | Required by StarbuncleMania and Sauce |
| Curios | Required by Ars Nouveau |
| GeckoLib | Required by Ars Nouveau |
| SauceLib | Bundled by StarbuncleMania and Ars Elemental; provides Liquid Source |
| Nuggets | Bundled by Ars Nouveau |
| Infiniverse | Bundled by Ars Nouveau |
| LambDynamicLights | Its API is bundled by Ars Nouveau |
| Ars-Elemental | Pack addon; supplies the newer bundled Sauce version |
| Mekanism | Registers the chemical system used by Gaseous Source |

Botany Pots requires Bookshelf and Prickle. StarbuncleMania requires Ars Nouveau, which requires Curios and GeckoLib. Required dependencies of those mods and bundled mod libraries terminate at Minecraft/NeoForge or the same listed mods. This was checked against the released jars' NeoForge metadata, including nested jars.

The pack's Ars Nouveau jar bundles Nuggets **1.1.0.48**, Infiniverse file **5486311**, and LambDynamicLights API **4.5.1+1.21.1**. StarbuncleMania bundles Sauce **0.0.40.82**; Ars Elemental bundles Sauce **0.0.61.100**. NeoForge selects the compatible newer Sauce. Do not add separate copies of these bundled jars.

The source pins are recorded in the parent Git index and in the JSON baseline. Release tags/version commits are used where identifiable; a source pin is reference material, not a guarantee that rebuilding that repository reproduces the pack jar. Branch hints in `.gitmodules` are for deliberate maintenance only.

## Source identities

The implementation uses Liquid Source `sauce:source_fluid` and Gaseous Source `starbunclemania:source_gas`. Sauce supplies aliases for the older `starbunclemania:source_fluid` name. StarbuncleMania registers the gas when Mekanism is present. This addon registers no replacement Source resources.

## Optional development JEI

The development client uses the pack's JEI **19.64.0.455**, CurseForge project **238222**, file **9205121**. Its SHA-256 is `f7a9de3d7ee07eed8d293a1a1a0f6afaac556eb43c103523457bd07ebb551e7b`; the jar bundles MezzConfig **0.6.6**. The separate development-tool entry in the JSON baseline records this provenance without changing the nine runtime dependency pins above.

JEI is added only to the development client launch. Main compilation, tests, dedicated server, GameTest server, and data generation do not depend on it. The shipped addon jar contains neither JEI nor a required JEI dependency. ATM10Aero already supplies JEI; install only the addon jar in the pack and avoid a duplicate JEI jar.

A focused metadata lookup found the exact JEI jar, but the subsequent actual launch still omitted it. The legacy classpath loaded JEI in the bootstrap layer before FML mod discovery. The final correction keeps JEI on the client JVM classpath and excludes it from that bootstrap list; generated launch-input checks pass. The 03:52 launch loaded JEI 19.64.0.455 and MezzConfig 0.6.6, and JEI's runtime initialized after joining Singleplayer. See [the verification record](verification/ars-botany-pots.md).

## Generated data compatibility

Data generation is limited to this addon's 310 distributable JSON files. The pinned StarbuncleMania datagen subscriber references an Ars Nouveau Patchouli provider class absent from the pinned Ars jar. During this addon's data run only, a guarded helper skips that StarbuncleMania datagen subscriber. Normal gameplay and other listeners remain enabled. The workaround must be rechecked when changing these dependency versions; [asset attribution](asset-attribution.md) records its scope and the model references.

Two final data runs produced identical hashes for all 310 JSON files; the second wrote zero changed files. Generated `.cache` metadata is ignored and excluded from both distributable jars.

## Verification limits

Historical bootstrap checks used Temurin Java 21.0.13:

- `./gradlew --no-daemon build` completed successfully before gameplay tests were added.
- `runServer` discovered the mod and all listed dependencies, selected Sauce **0.0.61.100**, loaded recipes, generated a world, and reached `Done`. This was a bounded smoke test; the harness terminated the persistent server at its timeout, so it was not a clean-exit test.
- Release archive/manifest hashes, all nine downloaded jar hashes and file IDs, and all 13 clean submodule pins were checked against the JSON record.

Current implementation checks use Windows OpenJDK 21.0.2+13-58. The Task 12 automated gate covers 28 unit tests and 79 required GameTests, including all 61 variants, Source scheduling, buffers, capabilities, growth, persistence, Creative event contents, shared six-face cadence, and matched mature wheat harvests. Current Task 13 amendment defaults are liquid 500 mB/5 ticks and gas 250 chemical units/1 tick, with inclusive growth ranges Jar 600–800, liquid 500–700 and gas 400–600. Each mode uses its own `growthMinTicks` and `growthMaxTicks`; the former shared growth keys are replaced. Jar settings remain 2500/80/100/10 with radius 3; saved world overrides are preserved. The historical shared-range Task 13 gate passed 28 unit tests and 79 required GameTests. The per-mode amendment gate passed 29 unit tests and 81 required GameTests. Development source launches reached dedicated-server `Done` and opened an actual client window. The dedicated server accepted `stop`, saved its worlds, and exited successfully; a completed client launch also exited successfully.

Startup logs contain missing Ars Elemental glyph-augment descriptions, development mixin reference-map warnings, and a Breeze spawn-placement warning. Client logs also contain GeckoLib animation-expression errors and model/texture messages naming dependency resources. They did not prevent the recorded startup and completed shutdowns. No control launch without this addon was performed, so these resource names do not prove that the same diagnostics occur without it. Full details and retained logs are in [the verification record](verification/ars-botany-pots.md).

The user confirmed that the Ars pots look okay in game and work with Source Jars, Liquid Source, and Gaseous Source. Detailed rendering, pipe, timing, and priority scenarios remain on the manual checklist.

These checks use the development source set and a small dependency set. They do not establish packaged-jar startup in the pack or the pack's KubeJS/config/recipe and multiplayer behavior. Remaining manual scenarios, user confirmation of Creative/JEI visibility, save/reopen, and backed-up ATM10Aero client/server checks remain pending. Full-pack compatibility is unverified.

## Updating the baseline

For a later pack release, inspect its released `manifest.json`, update NeoForge and the relevant file IDs together, and inspect the new jars' required and bundled dependencies. Update source pins deliberately, refresh the baseline record, build, and test in separate client/server pack instances before claiming compatibility with that release.

## Bootstrap tooling provenance

Gradle wrapper files were taken from the official [NeoForge 1.21.1 ModDevGradle MDK](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle/tree/e93d7f09d4eec12c76d1d7be9bf4eaad0246e9e0). The wrapper uses Gradle **9.2.1** with distribution checksum verification. ModDevGradle is pinned to **2.0.148**. The integration adds its own pots and resources to this build baseline.
