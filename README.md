# Fabric Seasons: Serene Shrubbery Compat

Seasonal flower breeding for **Minecraft 1.20.1 / Fabric**, with gentle defaults that preserve your garden throughout winter.

## Requirements and installation

Install the release JAR in `mods/` on the dedicated server and all clients, alongside:

- [Fabric Seasons 2.4.2-BETA+1.20](https://modrinth.com/mod/fabric-seasons)
- [Serene Shrubbery (Fabric) 1.2.1](https://modrinth.com/mod/serene-shrubbery-fabric/version/XfSsaYti)
- Fabric API and Fabric Loader 0.16.0 or newer; Java 17 or newer

The exact dependency versions are intentionally constrained because this integration hooks Serene Shrubbery's breeding implementation. The build uses Java 21, Gradle 9.2.1 and Loom 1.15.5; its output targets Java 17. This is an independent MIT-licensed addon, not an official release from either upstream author. Upstream mods are downloaded as build dependencies and are not bundled.

## Gameplay

| Effective season | Breeding timer multiplier |
| --- | --- |
| Spring | 1.25× |
| Summer | 1× |
| Autumn | 0.75× |
| Winter | 0.5× |

Serene Shrubbery uses an elapsed real-time breeding timer rather than growing through crop ages. Its normal 1–10 minute readiness window becomes approximately 48 seconds–8 minutes in spring and 2–20 minutes in winter. Actual offspring still require a random tick, compatible player-placed parents and free nearby space. Breeding recipes, parent-copy chances, light requirements and player-placement restrictions remain upstream behavior.

The **current effective season scales the elapsed time**. A change of season or greenhouse effect can therefore change readiness immediately. The original persisted timer is never rewritten or reset by this addon; it does not maintain a second clock or accumulate a historical mix of past seasons. Negative elapsed time after a system-clock rollback is clamped to zero while seasonal growth is enabled.

Flowers do not wilt or disappear. Bloom baskets and existing garden decorations are unaffected. All 42 fertilizable flowers receive a seasonal profile. Only the flowers that already breed gain seasonal breeding; the addon does not invent new breeding recipes.

If Fabric Seasons' seasonal bonemeal option is enabled, its existing API also scales flower duplication and blanketflower growth. If that option is disabled, bonemeal works normally. Seasons' crop-growth toggle, greenhouse tickets and underground exemption are respected. A greenhouse is whatever Fabric Seasons recognizes through its greenhouse API; an arbitrary glass roof does not automatically qualify.

## Getting flowers in an existing world

Serene Shrubbery 1.2.1 already hooks bonemeal on grass. In vanilla plains and several forest biomes it can introduce red, white, yellow and purple pansies, even in previously generated chunks. Forest/birch/cherry biomes also provide twinflowers. The extra attempt has a 1-in-6 chance and needs nearby short grass or ferns to replace. Break and replant acquired flowers before breeding them. Other starter plants may require exploring newly generated terrain; this addon does not regenerate terrain or add acquisition recipes.

## Customization

Override standard Fabric Seasons resources in a higher-priority datapack:

`data/serene_shrubbery/seasons/crop/red_pansies.json`

```json
{
  "spring": 1.25,
  "summer": 1.0,
  "fall": 0.75,
  "winter": 0.5
}
```

Use any of the 42 flower IDs included in this repository. A zero multiplier pauses breeding and seasonal bonemeal without removing plants. The center flower's profile controls a breeding attempt. Botany Pots has its own recipe-based growth system; this addon does not change pot timers.

## Build and tests

```sh
./gradlew --no-daemon build integrationCheck
```

JARs are written to `build/libs/`. Integration tests launch a disposable, headless dedicated Minecraft server with the real upstream mods on `127.0.0.1:25587`, exercise their transformed methods, then stop it. The test world is under `run-integration-test/`; no production server or graphical client is opened.

Tests cover loaded profiles for every fertilizable flower, all six bonemeal classes, faster/slower/zero breeding, vanilla player-placement restrictions, preserved decorations and timer timestamps, clock rollback, greenhouse behavior and Seasons toggles.

CI runs build and runtime tests on pushes to `main` and pull requests. Publishing a GitHub release runs the same tests and attaches the remapped JAR, sources JAR and SHA-256 checksums. Release notes are in English; stable tags use `vMAJOR.MINOR.PATCH`.

## Integration research

No ready Fabric Seasons integration was found in the [Fabric port's source](https://github.com/CozyCord/SereneShrubberyFabric), exact 1.2.1 JAR or public project searches on 2026-09-10. A bare crop datapack is insufficient for the upstream wall-clock breeding timer, hence this small Java hook. This project targets Fabric Seasons, which is a different mod from Serene Seasons.
