# Botany Pots - StarbuncleMania Integration

A Minecraft mod for **All the Mods 10: Aeronautics (ATM10Aero)**.

## Goals

Add several new versions of both the **Botany Pot** and **Hopper Botany Pot** that consume:

- **Ars Source**, from Ars Nouveau's Source system.
- **Liquid Source**, supplied through fluid pipes.
- **Gaseous Source**, supplied through Mekanism-compatible chemical pipes.

The new pots should work with the pack's existing crops and automation. Pot variants, Source costs, growth effects, recipes, storage limits, and pipe behaviour will be specified after the bootstrap.

## Current status

**Bootstrap only.** The project has a NeoForge mod entry point, pinned build dependencies, upstream source submodules, and a build workflow. It does not add pots, recipes, tanks, pipe connections, or Source consumption yet.

The target is **ATM10Aero 0.8.0**, the latest published release found on **2026-11-02**:

| Component | Target |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.250 |
| Java | 21 |
| Botany Pots | 21.1.44 |
| StarbuncleMania | 1.5.9 |
| Ars Nouveau | 5.13.3 |
| Mekanism | 10.7.19.85 |

Build versions come from the released pack's `manifest.json`, not the latest upstream branches. See [the compatibility baseline](docs/compatibility.md) for dependency details and verification limits. Full-pack client and server compatibility still requires testing in ATM10Aero; matching versions is not proof of gameplay compatibility.

## Get the sources

```sh
git clone --recurse-submodules https://github.com/GaiusRed/BotanyPots-Starbunklemania-Integration.git
cd BotanyPots-Starbunklemania-Integration
```

For an existing clone:

```sh
git submodule update --init --recursive
```

`upstream/` contains reference-only Git submodules for the integrations, their required mod dependencies, and bundled mod libraries. Each submodule is pinned to a commit. Gradle uses published jars and does not build or package these source trees. Do not use `git submodule update --remote` for routine setup: it changes the source baseline.

## Build and run

Install a Java 21 JDK and set `JAVA_HOME` to it. No global Gradle installation is needed.

```sh
./gradlew build
./gradlew runClient
./gradlew runServer
```

On Windows, use `gradlew.bat`. Build output is in `build/libs/`; use `sourcepots-0.1.0-dev.jar`, not the sources jar.

The development runs load only the integration dependency set, not the entire modpack. NeoForge development servers can bypass the normal EULA prompt and use offline authentication. Keep them local; do not expose a development server to the internet. Read and accept the Minecraft EULA yourself when setting up a normal packaged server.

For pack testing, use a separate ATM10Aero 0.8.0 instance and back up any worlds. Add the built mod jar to its `mods/` folder on both client and server. The pack already supplies the upstream mods; do not copy the submodules or add duplicate dependency jars.

## Repository

- `src/main/`: mod entry point and NeoForge metadata (`sourcepots`).
- `upstream/`: pinned upstream source submodules.
- `gradle.properties`: pack-matched dependency file IDs.
- `compatibility/atm10aero-0.8.0.json`: release provenance, artifact hashes, and source pins.
- `.github/workflows/build.yml`: Java 21 build on pushes and pull requests.

Feature design and the implementation plan are deferred until after the initial bootstrap commit.

## Licensing

No project licence has been selected. The bootstrap metadata uses **All Rights Reserved** until that decision is made. Each upstream project retains its own licence; its inclusion as a submodule does not relicense it.
