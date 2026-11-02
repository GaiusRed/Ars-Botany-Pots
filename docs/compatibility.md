# Compatibility baseline

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

In this pack baseline, Liquid Source is registered as `sauce:source_fluid`; Sauce supplies aliases for the older `starbunclemania:source_fluid` name. Gaseous Source is `starbunclemania:source_gas`, registered when Mekanism is present. Future implementation must use these existing resources rather than create competing Source types.

## Verification limits

Bootstrap checks performed with Temurin Java 21.0.13:

- `./gradlew --no-daemon build` completed successfully. No gameplay tests exist yet.
- `runServer` discovered the mod and all listed dependencies, selected Sauce **0.0.61.100**, loaded recipes, generated a world, and reached `Done`. This was a bounded smoke test; the harness terminated the persistent server at its timeout, so it was not a clean-exit test.
- Release archive/manifest hashes, all nine downloaded jar hashes and file IDs, and all 13 clean submodule pins were checked against the JSON record.

The development launch logged upstream missing glyph-augment descriptions for Ars Elemental's `glyph_carian_phalanx`, missing development mixin reference maps, and a Breeze spawn-placement warning. The first launch also logged a missing `server.properties` file before generating it. These did not prevent the server reaching `Done`; they have not been fixed in upstream projects.

A build checks compilation and packaging only. The development launch checks a small dependency set, not the pack's KubeJS scripts, configs, recipes, rendering, or multiplayer interactions. Full-pack client/server testing and tests for all three Source inputs remain future work.

## Updating the baseline

For a later pack release, inspect its released `manifest.json`, update NeoForge and the relevant file IDs together, and inspect the new jars' required and bundled dependencies. Update source pins deliberately, refresh the baseline record, build, and test in separate client/server pack instances before claiming compatibility with that release.

## Bootstrap tooling provenance

Gradle wrapper files were taken from the official [NeoForge 1.21.1 ModDevGradle MDK](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle/tree/e93d7f09d4eec12c76d1d7be9bf4eaad0246e9e0). The wrapper uses Gradle **9.2.1** with distribution checksum verification. ModDevGradle is pinned to **2.0.148**. The entry point and build configuration are intentionally minimal; example blocks and items are not included.
