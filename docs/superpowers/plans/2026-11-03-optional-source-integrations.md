# Optional Source Integrations

The user requested fewer required dependencies after merging Ars Botany Pots. Work on `feature/optional-source-integrations`, created from fast-forwarded `main` at `ddcffc9`. The only required gameplay mods should be Ars Nouveau and Botany Pots, plus their normal required libraries. Minecraft, NeoForge and Java requirements remain unchanged.

Use one distributable mod jar. Ars Jar power always works. Enable liquid input only when `sauce:source_fluid` exists, and gas input only when Mekanism and `starbunclemania:source_gas` are available. Missing integrations must not cause class-loading, registry or saved-state failures. Isolate Mekanism types behind a guarded compatibility boundary; do not expose them in core fields, method signatures or event registration. Guard the StarbuncleMania data-generation workaround as well.

Keep every existing variant, recipe, registry ID, config key/default, scheduler rule, growth range, harvest behavior and overflow policy. Preserve stored liquid/gas quantities while their integration is unavailable, without accepting, pulling or spending unavailable resources. Restore them within configured capacity when support returns. Normal transitive libraries required by Ars and Botany Pots remain dependencies of those mods.

## Progress

- [x] Fast-forward `main` to the merged PR and create `feature/optional-source-integrations`.
- [x] Add failing coverage for missing optional APIs/resources and saved buffers. Split shared resource access from optional Mekanism types and remove mandatory optional-mod metadata.
- [x] Verify the required-only runtime, liquid without Mekanism, Mekanism without the Source gas resource, and the full integration runtime. Missing resources must remain unavailable without exceptions; full integration must retain the existing native regressions.
- [x] Add development runtime profiles and isolated smoke-test runs that genuinely omit optional jars. Keep development fixtures out of the production jar. Verify minimal and full data generation and unchanged generated assets.
- [x] Run unit/native tests, build, package/classpath checks and independent code/prose review. Update the minimal README dependency list and verification evidence. Leave changes uncommitted until the user authorizes a commit for this task.

The implementation may use a small optional-storage interface with an inert storage implementation. The inert implementation preserves saved numbers, reports unavailable funding and exposes no optional capability. Do not add a generic plugin framework or new Source resources. Sample growth from the authorized mode exactly as before.

The user subsequently authorized committing this work, pushing the branch and opening a PR against `main` for approval. This does not authorize merging. GitHub is the delivery record.

Completed verification: clean build, 34 unit tests and 96 native tests (core 5, liquid-only 5, Mekanism-only 5, full integration 81) pass. Minimal/full data generation contains the same 310 JSON resources after newline normalization. All ten runtime classpaths and both jars were audited; the independent final review found no blocking issues. CI now runs the same runtime matrix and data tasks. The dev client defaults to the minimal runtime; `runFullClient` retains the full stack. No client was launched. The reviewed changes are ready for the authorized commit and PR delivery.
